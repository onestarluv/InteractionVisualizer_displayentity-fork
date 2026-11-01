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
import com.loohp.interactionvisualizer.api.InteractionVisualizerAPI;
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
            if (!sender.hasPermission("interactionvisualizer.toggle")) {
                sender.sendMessage(ChatColor.RED + "이 명령어를 사용할 권한이 없습니다.");
                return true;
            }
            final CommandLanguage.ToggleArguments request;
            try {
                request = CommandLanguage.toggleArguments(args);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "사용법: /시각효과 토글 <블록/전체> [켜기/끄기] [플레이어]");
                return true;
            }
            Player player = request.player() == null ? (sender instanceof Player self ? self : null) : Bukkit.getPlayerExact(request.player());
            if (player == null) {
                sender.sendMessage(ChatColor.RED + (request.player() == null ? "콘솔에서는 대상 플레이어를 지정하세요." : "해당 플레이어가 접속 중이 아닙니다."));
                return true;
            }
            if (sender instanceof Player && !player.equals(sender) && !sender.hasPermission("interactionvisualizer.toggle.others")) {
                sender.sendMessage(ChatColor.RED + "다른 플레이어의 설정을 변경할 권한이 없습니다.");
                return true;
            }
            final EntryKey[] entries;
            final String group;
            try {
                if (request.entry().equalsIgnoreCase("all")) {
                    entries = InteractionVisualizer.preferenceManager.getRegisteredEntries().toArray(new EntryKey[0]);
                    group = "전체";
                } else {
                    EntryKey entry = new EntryKey(request.entry());
                    if (!InteractionVisualizerAPI.isRegisteredEntry(entry)) throw new IllegalArgumentException();
                    entries = new EntryKey[] {entry};
                    group = InteractionVisualizerAPI.getUserFriendlyName(entry);
                }
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "지원하지 않는 블록 또는 항목입니다. 자동완성에서 선택하세요.");
                return true;
            }
            InteractionVisualizer.asyncExecutorManager.runTaskAsynchronously(() -> Toggle.toggleAll(sender, player, request.enabled(), group, entries));
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

        sender.sendMessage(ChatColor.RED + "알 수 없는 명령어입니다. /시각효과 토글 <블록/전체> [켜기/끄기]를 사용하세요.");
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
            
            if (args.length == 2) {
                candidates.putAll(CommandLanguage.ENTRIES);
                InteractionVisualizer.preferenceManager.getRegisteredEntries().forEach(entry -> candidates.put(entry.toString(), entry.toString()));
            }
            if (args.length == 3) candidates.putAll(CommandLanguage.VALUES);
            if ((args.length == 3 || args.length == 4) && sender.hasPermission("interactionvisualizer.toggle.others")) {
                Bukkit.getOnlinePlayers().forEach(player -> candidates.put(player.getName(), player.getName()));
            }
            new java.util.ArrayList<>(candidates.values()).forEach(value -> candidates.put(value, value));
        }
        String prefix = input.length == 0 ? "" : input[input.length - 1].toLowerCase(java.util.Locale.ROOT);
        candidates.keySet().stream()
                .filter(value -> !prefix.isEmpty() || args.length >= 3 || value.codePoints().anyMatch(c -> c >= 0xAC00 && c <= 0xD7A3))
                .filter(value -> value.toLowerCase(java.util.Locale.ROOT).startsWith(prefix)).forEach(tab::add);
        return tab;
    }
}
