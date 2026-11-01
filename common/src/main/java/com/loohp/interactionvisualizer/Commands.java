/*
 * This file is part of InteractionVisualizer.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.loohp.interactionvisualizer;

import com.loohp.interactionvisualizer.api.InteractionVisualizerAPI.Modules;
import com.loohp.interactionvisualizer.managers.MaterialManager;
import com.loohp.interactionvisualizer.managers.MusicManager;
import com.loohp.interactionvisualizer.managers.PacketManager;
import com.loohp.interactionvisualizer.objectholders.EntryKey;
import com.loohp.interactionvisualizer.updater.Updater;
import com.loohp.interactionvisualizer.updater.Updater.UpdaterResponse;
import com.loohp.interactionvisualizer.utils.ChatColorUtils;
import com.loohp.platformscheduler.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.LinkedList;
import java.util.List;

public class Commands implements CommandExecutor, TabCompleter {

    private static final InteractionVisualizer plugin = InteractionVisualizer.plugin;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] input) {
        final String[] args = CommandLanguage.normalize(input);
        if (!cmd.getName().equalsIgnoreCase("시각효과")) {
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.AQUA + "[kokonut_ineteraction] InteractionVisualizer 포크 관리자: onestarluv (원작: LOOHP)");
            sender.sendMessage(ChatColor.GOLD + "[kokonut_ineteraction] 실행 중인 버전: " + plugin.getDescription().getVersion());
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("interactionvisualizer.reload")) {
                plugin.loadConfig();
                MusicManager.reloadConfig();
                MaterialManager.reloadConfig();
                sender.sendMessage(ChatColorUtils.translateAlternateColorCodes('&', plugin.getConfiguration().getString("Messages.Reload")));
            } else {
                sender.sendMessage(ChatColorUtils.translateAlternateColorCodes('&', plugin.getConfiguration().getString("Messages.NoPermission")));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("refresh")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.hasPermission("interactionvisualizer.refresh")) {
                    Scheduler.runTask(InteractionVisualizer.plugin, () -> PacketManager.reset(player));
                } else {
                    sender.sendMessage(ChatColorUtils.translateAlternateColorCodes('&', plugin.getConfiguration().getString("Messages.NoPermission")));
                }
            } else {
                sender.sendMessage(ChatColorUtils.translateAlternateColorCodes('&', plugin.getConfiguration().getString("Messages.Toggle.Console")));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("update")) {
            if (sender.hasPermission("interactionvisualizer.update")) {
                sender.sendMessage(ChatColor.AQUA + "[kokonut_ineteraction] InteractionVisualizer 포크 관리자: onestarluv (원작: LOOHP)");
                sender.sendMessage(ChatColor.GOLD + "[kokonut_ineteraction] 실행 중인 버전: " + plugin.getDescription().getVersion());
                InteractionVisualizer.asyncExecutorManager.runTaskAsynchronously(() -> {
                    UpdaterResponse version = Updater.checkUpdate();
                    if (version.getResult().equals("latest")) {
                        if (version.isDevBuildLatest()) {
                            sender.sendMessage(ChatColor.GREEN + "[kokonut_ineteraction] 최신 버전을 사용 중입니다!");
                        } else {
                            Updater.sendUpdateMessage(sender, version.getResult(), version.getSpigotPluginId(), true);
                        }
                    } else {
                        Updater.sendUpdateMessage(sender, version.getResult(), version.getSpigotPluginId());
                    }
                });
            } else {
                sender.sendMessage(ChatColorUtils.translateAlternateColorCodes('&', plugin.getConfiguration().getString("Messages.NoPermission")));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("toggle")) {
            if (sender.hasPermission("interactionvisualizer.toggle")) {
                if ((args.length == 4 && (args[3].equalsIgnoreCase("true") || args[3].equalsIgnoreCase("false"))) || args.length == 3) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColorUtils.translateAlternateColorCodes('&', plugin.getConfiguration().getString("Messages.Toggle.Console")));
                        return true;
                    }
                    Player player = (Player) sender;
                    InteractionVisualizer.asyncExecutorManager.runTaskAsynchronously(() -> {
                        EntryKey[] entries;
                        String verboseEntry = null;
                        if (args[2].equalsIgnoreCase("all")) {
                            entries = InteractionVisualizer.preferenceManager.getRegisteredEntries().toArray(new EntryKey[0]);
                            verboseEntry = ChatColorUtils.translateAlternateColorCodes('&', plugin.getConfiguration().getString("Messages.Toggle.All"));
                        } else {
                            entries = new EntryKey[] {new EntryKey(args[2])};
                        }
                        if (args.length == 4) {
                            boolean value = args[3].equalsIgnoreCase("true");
                            switch (args[1].toLowerCase()) {
                                case "itemstand":
                                    Toggle.toggle(sender, player, Modules.ITEMSTAND, value, true, verboseEntry, entries);
                                    break;
                                case "itemdrop":
                                    Toggle.toggle(sender, player, Modules.ITEMDROP, value, true, verboseEntry, entries);
                                    break;
                                case "hologram":
                                    Toggle.toggle(sender, player, Modules.HOLOGRAM, value, true, verboseEntry, entries);
                                    break;
                                case "all":
                                    for (Modules modules : Modules.values()) {
                                        Toggle.toggle(sender, player, modules, value, true, verboseEntry, entries);
                                    }
                                    break;
                                default:
                                    sender.sendMessage(ChatColorUtils.translateAlternateColorCodes('&', plugin.getConfiguration().getString("Messages.Toggle.Modes")));
                            }
                        } else {
                            switch (args[1].toLowerCase()) {
                                case "itemstand":
                                    Toggle.toggle(sender, player, Modules.ITEMSTAND, true, verboseEntry, entries);
                                    break;
                                case "itemdrop":
                                    Toggle.toggle(sender, player, Modules.ITEMDROP, true, verboseEntry, entries);
                                    break;
                                case "hologram":
                                    Toggle.toggle(sender, player, Modules.HOLOGRAM, true, verboseEntry, entries);
                                    break;
                                case "all":
                                    for (Modules modules : Modules.values()) {
                                        Toggle.toggle(sender, player, modules, true, verboseEntry, entries);
                                    }
                                    break;
                                default:
                                    sender.sendMessage(ChatColorUtils.translateAlternateColorCodes('&', plugin.getConfiguration().getString("Messages.Toggle.Modes")));
                            }
                        }
                    });
                    return true;
                } else if (args.length == 5 || args.length == 4) {
                    Player player = Bukkit.getPlayer(args[args.length == 4 ? 3 : 4]);
                    if (sender instanceof Player) {
                        if (player != null) {
                            if (!player.equals(sender)) {
                                if (!sender.hasPermission("interactionvisualizer.toggle.others")) {
                                    sender.sendMessage(ChatColorUtils.translateAlternateColorCodes('&', plugin.getConfiguration().getString("Messages.NoPermission")));
                                    return true;
                                }
                            }
                        }
                    }
                    if (player == null) {
                        sender.sendMessage(ChatColorUtils.translateAlternateColorCodes('&', plugin.getConfiguration().getString("Messages.Toggle.PlayerNotFound")));
                        return true;
                    }
                    InteractionVisualizer.asyncExecutorManager.runTaskAsynchronously(() -> {
                        EntryKey[] entries;
                        String verboseEntry = null;
                        if (args[2].equalsIgnoreCase("all")) {
                            entries = InteractionVisualizer.preferenceManager.getRegisteredEntries().toArray(new EntryKey[0]);
                            verboseEntry = ChatColorUtils.translateAlternateColorCodes('&', plugin.getConfiguration().getString("Messages.Toggle.All"));
                        } else {
                            entries = new EntryKey[] {new EntryKey(args[2])};
                        }
                        if (args.length == 5) {
                            boolean value = args[3].equalsIgnoreCase("true");
                            switch (args[1].toLowerCase()) {
                                case "itemstand":
                                    Toggle.toggle(sender, player, Modules.ITEMSTAND, value, true, verboseEntry, entries);
                                    break;
                                case "itemdrop":
                                    Toggle.toggle(sender, player, Modules.ITEMDROP, value, true, verboseEntry, entries);
                                    break;
                                case "hologram":
                                    Toggle.toggle(sender, player, Modules.HOLOGRAM, value, true, verboseEntry, entries);
                                    break;
                                case "all":
                                    for (Modules modules : Modules.values()) {
                                        Toggle.toggle(sender, player, modules, value, true, verboseEntry, entries);
                                    }
                                    break;
                                default:
                                    sender.sendMessage(ChatColorUtils.translateAlternateColorCodes('&', plugin.getConfiguration().getString("Messages.Toggle.Modes")));
                            }
                        } else {
                            switch (args[1].toLowerCase()) {
                                case "itemstand":
                                    Toggle.toggle(sender, player, Modules.ITEMSTAND, true, verboseEntry, entries);
                                    break;
                                case "itemdrop":
                                    Toggle.toggle(sender, player, Modules.ITEMDROP, true, verboseEntry, entries);
                                    break;
                                case "hologram":
                                    Toggle.toggle(sender, player, Modules.HOLOGRAM, true, verboseEntry, entries);
                                    break;
                                case "all":
                                    for (Modules modules : Modules.values()) {
                                        Toggle.toggle(sender, player, modules, true, verboseEntry, entries);
                                    }
                                    break;
                                default:
                                    sender.sendMessage(ChatColorUtils.translateAlternateColorCodes('&', plugin.getConfiguration().getString("Messages.Toggle.Modes")));
                            }
                        }
                    });
                    return true;
                } else {
                    sender.sendMessage(ChatColorUtils.translateAlternateColorCodes('&', plugin.getConfiguration().getString("Messages.Toggle.Usage")));
                }
                return true;
            }
            sender.sendMessage(ChatColorUtils.translateAlternateColorCodes('&', plugin.getConfiguration().getString("Messages.NoPermission")));
            return true;
        }

        if (args[0].equalsIgnoreCase("ethereal")) {
            Component text = Component.text("그녀는 상상 속에 있어요~~");
            text = text.color(NamedTextColor.YELLOW);
            Component bone = LegacyComponentSerializer.legacySection().deserialize("\u00a7eNana's Bone\n\u00a77Lost \u00a76In-\u00a7dMaginary~~");
            text = text.hoverEvent(HoverEvent.showText(bone)).clickEvent(ClickEvent.openUrl("https://www.instagram.com/narliar/"));
            InteractionVisualizer.sendMessage(sender, text);
            return true;
        }

        sender.sendMessage(ChatColor.RED + "알 수 없는 명령어입니다. /시각효과 토글 <종류> <블록/전체> [켜기/끄기]를 사용하세요.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] input) {
        List<String> tab = new LinkedList<>();
        String[] args = CommandLanguage.normalize(input);
        java.util.Map<String, String> candidates = new java.util.LinkedHashMap<>();
        if (args.length <= 1) {
            CommandLanguage.ACTIONS.forEach((ko, en) -> {
                if (sender.hasPermission("interactionvisualizer." + en)) {
                    candidates.put(ko, en);
                    candidates.put(en, en);
                }
            });
        } else if (args[0].equalsIgnoreCase("toggle") && sender.hasPermission("interactionvisualizer.toggle")) {
            if (args.length == 2) candidates.putAll(CommandLanguage.MODULES);
            if (args.length == 3) {
                candidates.putAll(CommandLanguage.ENTRIES);
                InteractionVisualizer.preferenceManager.getRegisteredEntries().forEach(entry -> candidates.put(entry.toString(), entry.toString()));
            }
            if (args.length == 4) candidates.putAll(CommandLanguage.VALUES);
            if (args.length == 5 && sender.hasPermission("interactionvisualizer.toggle.others")) {
                Bukkit.getOnlinePlayers().forEach(player -> candidates.put(player.getName(), player.getName()));
            }
            new java.util.ArrayList<>(candidates.values()).forEach(value -> candidates.put(value, value));
        }
        String prefix = input.length == 0 ? "" : input[input.length - 1].toLowerCase(java.util.Locale.ROOT);
        candidates.keySet().stream()
                .filter(value -> !prefix.isEmpty() || args.length == 5 || value.codePoints().anyMatch(c -> c >= 0xAC00 && c <= 0xD7A3))
                .filter(value -> value.toLowerCase(java.util.Locale.ROOT).startsWith(prefix)).forEach(tab::add);
        return tab;
    }
}
