package com.loohp.interactionvisualizer.nms;

import com.loohp.interactionvisualizer.InteractionVisualizer;
import com.loohp.interactionvisualizer.entityholders.ArmorStand;
import com.loohp.interactionvisualizer.entityholders.VisualizerEntity;
import com.loohp.platformscheduler.Scheduler;
import com.mojang.math.Transformation;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.PositionPath;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** Packet-only render objects. These entities are never added to a server world. */
final class DisplayRenderer {
    private final Map<Integer, Visual> visuals = new ConcurrentHashMap<>();
    private boolean ticking;

    private synchronized void start() {
        if (ticking) return;
        ticking = true;
        Scheduler.runTaskTimerAsynchronously(InteractionVisualizer.plugin, this::tick, 20, 20);
    }

    private Visual visual(VisualizerEntity source) {
        start();
        return visuals.computeIfAbsent(source.getEntityId(), ignored -> new Visual(source));
    }

    void spawn(Collection<Player> players, VisualizerEntity source) {
        Visual visual = visual(source);
        synchronized (visual) {
            if (visual.viewers.isEmpty()) visual.configure(source);
            visual.captureMetadata();
            for (Player player : players) {
                visual.viewers.put(player.getUniqueId(), player);
                synchronizePlayer(visual, player, false);
            }
            // Cached metadata also includes values reset to defaults, such as cleared text/items.
        }
    }

    void update(Collection<Player> players, VisualizerEntity source) {
        update(players, source, false);
    }

    List<SynchedEntityData.DataValue<?>> metadata(ArmorStand source) {
        Visual visual = visual(source);
        synchronized (visual) {
            visual.configure(source);
            visual.captureMetadata();
            return visual.metadata(visual.text);
        }
    }

    void update(Collection<Player> players, VisualizerEntity source, boolean metadataOnly) {
        Visual visual = visual(source);
        synchronized (visual) {
            visual.configure(source);
            visual.captureMetadata();
            for (Player player : players) {
                if (!visual.viewers.containsKey(player.getUniqueId())) continue;
                if (!metadataOnly) visual.viewLocations.remove(player.getUniqueId());
                synchronizePlayer(visual, player, metadataOnly);
            }
        }
    }

    void remove(Collection<Player> players, VisualizerEntity source) {
        Visual visual = visuals.get(source.getEntityId());
        if (visual == null) return;
        synchronized (visual) {
            Packet<?> packet = new ClientboundRemoveEntitiesPacket(visual.ids());
            for (Player player : players) {
                send(player, packet);
                visual.viewers.remove(player.getUniqueId());
                visual.spawned.remove(player.getUniqueId());
                visual.viewLocations.remove(player.getUniqueId());
            }
        }
    }

    int[] expand(int... roots) {
        return Arrays.stream(roots).flatMap(id -> {
            Visual visual = visuals.get(id);
            return visual == null ? java.util.stream.IntStream.of(id) : Arrays.stream(visual.ids());
        }).distinct().toArray();
    }

    boolean teleport(Player player, int root, Location location) {
        Visual visual = visuals.get(root);
        if (visual == null) return false;
        synchronized (visual) {
            if (!visual.viewers.containsKey(player.getUniqueId())) return true;
            visual.viewLocations.put(player.getUniqueId(), location.clone());
            // Viewer-specific locations must not modify the shared source or render state.
            for (Display node : visual.nodes) {
                if (!visual.spawned.getOrDefault(player.getUniqueId(), Collections.emptySet()).contains(node.getId())) continue;
                send(player, new ClientboundEntityPositionSyncPacket(node.getId(),
                        PositionPath.of(new Vec3(location.getX(), location.getY(), location.getZ())), 0, 0, false));
            }
        }
        return true;
    }

    private void tick() {
        visuals.entrySet().removeIf(entry -> {
            Visual visual = entry.getValue();
            synchronized (visual) {
                if (visual.source.get() == null) {
                    Packet<?> remove = new ClientboundRemoveEntitiesPacket(visual.ids());
                    visual.viewers.values().forEach(player -> send(player, remove));
                    return true;
                }
                visual.viewers.values().removeIf(player -> {
                    if (player.isOnline()) return false;
                    visual.spawned.remove(player.getUniqueId());
                    visual.viewLocations.remove(player.getUniqueId());
                    return true;
                });
                return false;
            }
        });
    }

    private static void synchronizePlayer(Visual visual, Player player, boolean metadataOnly) {
        Set<Integer> spawned = visual.spawned.computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>());
        Location view = visual.viewLocations.get(player.getUniqueId());
        for (Display node : visual.nodes) {
            boolean visible = node instanceof Display.ItemDisplay item ? !item.getItemStack().isEmpty()
                    : !((Display.TextDisplay) node).getText().getString().isEmpty();
            if (!visible) {
                if (spawned.remove(node.getId())) send(player, new ClientboundRemoveEntitiesPacket(node.getId()));
                continue;
            }
            Vec3 position = view == null ? node.position() : new Vec3(view.getX(), view.getY(), view.getZ());
            if (spawned.add(node.getId())) {
                send(player, new ClientboundAddEntityPacket(node.getId(), node.getUUID(), position.x, position.y, position.z,
                        0, 0, node.getType(), 0, Vec3.ZERO, 0));
            } else if (!metadataOnly) {
                send(player, new ClientboundEntityPositionSyncPacket(node.getId(), PositionPath.of(position), 0, 0, false));
            }
            List<SynchedEntityData.DataValue<?>> data = visual.metadata(node);
            if (!data.isEmpty()) send(player, new ClientboundSetEntityDataPacket(node.getId(), data));
        }
    }

    private static void send(Player player, Packet<?> packet) {
        if (player.isOnline()) ((CraftPlayer) player).getHandle().connection.send(packet);
    }

    private static Transformation transform(Vector3f offset, Quaternionf rotation, Vector3f scale) {
        return new Transformation(offset, rotation, scale, new Quaternionf());
    }

    private static final class Visual {
        final WeakReference<VisualizerEntity> source;
        final List<Display> nodes = new ArrayList<>();
        final Map<UUID, Player> viewers = new ConcurrentHashMap<>();
        final Map<UUID, Set<Integer>> spawned = new HashMap<>();
        final Map<UUID, Location> viewLocations = new HashMap<>();
        final Map<Integer, Map<Integer, SynchedEntityData.DataValue<?>>> metadata = new HashMap<>();
        final Display.TextDisplay text;
        final Display.ItemDisplay hand;
        final Display.ItemDisplay head;

        Visual(VisualizerEntity source) {
            this.source = new WeakReference<>(source);
            var level = ((CraftWorld) source.getWorld()).getHandle();
            text = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, level);
            hand = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, level);
            head = new Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, level);
            hand.setBrightnessOverride(net.minecraft.util.Brightness.FULL_BRIGHT);
            head.setBrightnessOverride(net.minecraft.util.Brightness.FULL_BRIGHT);
            nodes.add(text);
            text.setBillboardConstraints(Display.BillboardConstraints.VERTICAL);
            text.getEntityData().set(Display.TextDisplay.DATA_BACKGROUND_COLOR_ID, 0);
            text.getEntityData().set(Display.TextDisplay.DATA_LINE_WIDTH_ID, 10000);
            // Like the original hologram names, keep block-adjacent labels readable
            // when the viewer moves around the block and its faces occlude the text.
            text.setFlags((byte) (Display.TextDisplay.FLAG_SHADOW | Display.TextDisplay.FLAG_SEE_THROUGH));
            text.setBrightnessOverride(net.minecraft.util.Brightness.FULL_BRIGHT);
            nodes.add(hand);
            nodes.add(head);
            nodes.getFirst().setId(source.getEntityId());
            nodes.getFirst().setUUID(source.getUniqueId());
            for (Display node : nodes) {
                node.setNoGravity(true);
                node.setSilent(true);
                node.setViewRange(4);
                node.setTransformationInterpolationDuration(0);
            }
        }

        int[] ids() { return nodes.stream().mapToInt(Display::getId).toArray(); }

        void captureMetadata() {
            for (Display node : nodes) {
                Map<Integer, SynchedEntityData.DataValue<?>> values = metadata.computeIfAbsent(node.getId(), ignored -> new TreeMap<>());
                List<SynchedEntityData.DataValue<?>> dirty = node.getEntityData().packDirty();
                if (dirty != null) dirty.forEach(value -> values.put(value.id(), value));
            }
        }

        List<SynchedEntityData.DataValue<?>> metadata(Display node) {
            return new ArrayList<>(metadata.get(node.getId()).values());
        }

        void configure(VisualizerEntity source) {
            Location location = source.getLocation();
            for (Display node : nodes) node.setPos(location.getX(), location.getY(), location.getZ());

            if (source instanceof ArmorStand stand) {
                float size = stand.isSmall() ? 0.5f : 1;
                Quaternionf yaw = DisplayTransforms.yaw(location.getYaw());
                Quaternionf arm = DisplayTransforms.pose(stand.getRightArmPose());
                Vector3f handOffset = DisplayTransforms.hand(location.getYaw(), stand.getRightArmPose(), size);
                hand.setItemStack(CraftItemStack.asNMSCopy(stand.getItemInMainHand()));
                hand.setItemTransform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
                hand.setTransformation(transform(handOffset, new Quaternionf(yaw).mul(arm).rotateX((float) -Math.PI / 2), new Vector3f(size)));
                String tag = stand.getCustomName() == null ? "" : PlainTextComponentSerializer.plainText().serialize(stand.getCustomName());
                if (tag.startsWith("IV.CraftingTable.")) {
                    String mode = tag.substring("IV.CraftingTable.".length());
                    hand.setItemTransform(ItemDisplayContext.FIXED);
                    hand.setTransformation(transform(DisplayTransforms.craftingOffset(location.getYaw(), mode),
                            DisplayTransforms.yaw(DisplayTransforms.craftingYaw(location.getYaw(), mode)).rotateX((float) Math.PI / 2),
                            new Vector3f(0.18f)));
                } else if (tag.startsWith("IV.Anvil.")) {
                    String mode = tag.substring("IV.Anvil.".length());
                    hand.setItemTransform(ItemDisplayContext.FIXED);
                    hand.setTransformation(transform(DisplayTransforms.anvilOffset(location.getYaw(), mode),
                            DisplayTransforms.yaw(DisplayTransforms.craftingYaw(location.getYaw(), mode)).rotateX((float) Math.PI / 2),
                            new Vector3f(0.26f)));
                } else if (tag.startsWith("IV.Grindstone.")) {
                    String mode = tag.substring("IV.Grindstone.".length());
                    Vector3f offset = DisplayTransforms.grindstoneOffset(location.getYaw(), mode, size);
                    if (mode.equals("Tool")) {
                        // Preserve the tool's current height, rotation and model context.
                        hand.setTransformation(transform(offset,
                                new Quaternionf(yaw).mul(arm).rotateX((float) -Math.PI / 2), new Vector3f(size)));
                    } else {
                        hand.setItemTransform(ItemDisplayContext.FIXED);
                        hand.setTransformation(transform(offset,
                                DisplayTransforms.yaw(DisplayTransforms.craftingYaw(location.getYaw(), mode)).rotateX((float) Math.PI / 2),
                                new Vector3f(0.26f)));
                    }
                }
                head.setItemStack(CraftItemStack.asNMSCopy(stand.getHelmet()));
                head.setItemTransform(ItemDisplayContext.HEAD);
                head.setTransformation(transform(yaw.transform(new Vector3f(0, 1.4375f * size, 0)),
                        new Quaternionf(yaw).mul(DisplayTransforms.pose(stand.getHeadPose())), new Vector3f(size)));
                name(stand.getCustomName(), stand.isCustomNameVisible(), (float) (stand.isMarker() ? 0.5 : stand.getHeight() + 0.5));
            }
        }

        private void name(Component name, boolean visible, float height) {
            text.setText(name == null || !visible ? net.minecraft.network.chat.Component.empty()
                    : CraftChatMessage.fromJSON(GsonComponentSerializer.gson().serialize(name)));
            text.setTransformation(transform(new Vector3f(0, height, 0), new Quaternionf(), new Vector3f(1)));
        }
    }
}
