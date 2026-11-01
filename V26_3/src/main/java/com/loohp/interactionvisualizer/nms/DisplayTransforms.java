package com.loohp.interactionvisualizer.nms;

import org.bukkit.util.EulerAngle;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Converts the armor stand's model coordinate system to display coordinates. */
final class DisplayTransforms {
    private DisplayTransforms() {}

    static Quaternionf yaw(float degrees) {
        return new Quaternionf().rotateY((float) -Math.toRadians(degrees));
    }

    static Quaternionf pose(EulerAngle pose) {
        // Model parts apply Z, Y, X rotation; model-space Y points downward.
        return new Quaternionf().rotateZ((float) -pose.getZ())
                .rotateY((float) -pose.getY()).rotateX((float) pose.getX());
    }

    static Vector3f hand(float yaw, EulerAngle pose, float size) {
        Vector3f offset = pose(pose).transform(new Vector3f(0, -0.625f, 0));
        offset.add(-0.3125f, 1.375f, 0).mul(size);
        return yaw(yaw).transform(offset);
    }

    static float craftingYaw(float yaw, String mode) {
        return mode.equals("Block") || mode.equals("LowBlock") ? yaw - 45 : yaw;
    }

    static Vector3f craftingOffset(float yaw, String mode) {
        // spawnArmorStands offsets the logical slot to compensate for a small stand's hand.
        // Undo that compensation without changing the slot's recipe/animation coordinates.
        Vector3f compensation = new Vector3f(0, 0, 0.19f).rotateY((float) Math.toRadians(100));
        compensation.add(0, 0, -0.11f);
        // Surface is blockY + 1; stand origin is blockY + 0.600781.
        return yaw(craftingYaw(yaw, mode)).transform(
                compensation.negate().sub(modeOffset(mode)).add(0, 1.10f - 0.600781f, 0));
    }

    static Vector3f anvilOffset(float sourceYaw, String mode) {
        float standYaw = craftingYaw(sourceYaw, mode);
        // The two slots face +/-20 degrees from the table, but their shared
        // hand compensation was calculated using the table's cardinal yaw.
        float tableYaw = Math.round(standYaw / 90f) * 90f;
        Vector3f compensation = new Vector3f(0, 0, .19f).rotateY((float) Math.toRadians(100)).add(0, 0, -.11f);
        yaw(tableYaw).transform(compensation);
        Vector3f modeDelta = yaw(standYaw).transform(modeOffset(mode));
        return compensation.negate().sub(modeDelta).add(0, 1.14f - .600781f, 0);
    }

    static Vector3f grindstoneOffset(float sourceYaw, String mode, float size) {
        Vector3f offset = anvilOffset(sourceYaw, mode);
        // Keep the existing tool height. Other modes (including enchanted books)
        // must reach that same height after their logical mode displacement.
        float toolHeight = hand(0, new EulerAngle(357.99, 0, 300), size).y - .26f;
        offset.y = toolHeight - modeOffset(mode).y;
        return offset;
    }

    private static Vector3f modeOffset(String mode) {
        return switch (mode) {
            case "Block" -> new Vector3f(0.102f, 0.084f, 0.14f);
            case "LowBlock" -> new Vector3f(0.09f, 0.02f, 0.15f);
            case "Tool" -> new Vector3f(-0.3f, -0.26f, -0.1f);
            case "Standing" -> new Vector3f(-0.323f, -0.32f, 0.115f);
            default -> new Vector3f();
        };
    }
}
