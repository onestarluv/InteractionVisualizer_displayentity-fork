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

package com.loohp.interactionvisualizer.updater;

import com.loohp.interactionvisualizer.InteractionVisualizer;
import com.loohp.interactionvisualizer.utils.HTTPRequestUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.json.simple.JSONObject;

public class Updater implements Listener {

    public static final String PLUGIN_NAME = "InteractionVisualizer";

    public static void sendUpdateMessage(CommandSender sender, String version, int spigotPluginId) {
        sendUpdateMessage(sender, version, spigotPluginId, false);
    }

    public static void sendUpdateMessage(CommandSender sender, String version, int spigotPluginId, boolean devbuild) {
        if (!version.equals("error")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (!devbuild) {
                    player.sendMessage(ChatColor.YELLOW + "[kokonut_ineteraction] 원본 프로젝트의 새 버전이 있습니다: " + version);
                    Component url = LegacyComponentSerializer.legacySection().deserialize(ChatColor.GOLD + "https://www.spigotmc.org/resources/" + spigotPluginId);
                    url = url.hoverEvent(HoverEvent.showText(Component.text("눌러서 다운로드 페이지 열기").color(NamedTextColor.AQUA)));
                    url = url.clickEvent(ClickEvent.openUrl("https://www.spigotmc.org/resources/" + spigotPluginId));
                    InteractionVisualizer.sendMessage(player, url);
                } else {
                    sender.sendMessage(ChatColor.GREEN + "[kokonut_ineteraction] 원본 프로젝트의 최신 릴리스 버전입니다.");
                    Component url = LegacyComponentSerializer.legacySection().deserialize(ChatColor.YELLOW + "[kokonut_ineteraction] 원본 프로젝트의 새 개발 빌드가 있습니다.");
                    url = url.hoverEvent(HoverEvent.showText(Component.text("눌러서 다운로드 페이지 열기").color(NamedTextColor.AQUA)));
                    url = url.clickEvent(ClickEvent.openUrl("https://ci.loohpjames.com/job/" + PLUGIN_NAME));
                    InteractionVisualizer.sendMessage(player, url);
                }
            } else {
                if (!devbuild) {
                    sender.sendMessage(ChatColor.YELLOW + "[kokonut_ineteraction] 원본 프로젝트의 새 버전이 있습니다: " + version);
                    sender.sendMessage(ChatColor.GOLD + "다운로드: https://www.spigotmc.org/resources/" + spigotPluginId);
                } else {
                    sender.sendMessage(ChatColor.GREEN + "[kokonut_ineteraction] 원본 프로젝트의 최신 릴리스 버전입니다.");
                    sender.sendMessage(ChatColor.YELLOW + "[kokonut_ineteraction] 원본 프로젝트의 새 개발 빌드가 있습니다.");
                }
            }
        }
    }

    public static UpdaterResponse checkUpdate() {
        try {
            String localPluginVersion = InteractionVisualizer.plugin.getDescription().getVersion();
            JSONObject response = (JSONObject) HTTPRequestUtils.getJSONResponse("https://api.loohpjames.com/spigot/data").get(PLUGIN_NAME);
            String spigotPluginVersion = (String) ((JSONObject) response.get("latestversion")).get("release");
            String devBuildVersion = (String) ((JSONObject) response.get("latestversion")).get("devbuild");
            int spigotPluginId = (int) (long) ((JSONObject) response.get("spigotmc")).get("pluginid");
            int posOfThirdDot = localPluginVersion.indexOf(".", localPluginVersion.indexOf(".", localPluginVersion.indexOf(".") + 1) + 1);
            Version currentDevBuild = new Version(localPluginVersion);
            Version currentRelease = new Version(localPluginVersion.substring(0, posOfThirdDot >= 0 ? posOfThirdDot : localPluginVersion.length()));
            Version spigotmc = new Version(spigotPluginVersion);
            Version devBuild = new Version(devBuildVersion);
            if (currentRelease.compareTo(spigotmc) < 0) {
                return new UpdaterResponse(spigotPluginVersion, spigotPluginId, currentDevBuild.compareTo(devBuild) >= 0);
            } else {
                return new UpdaterResponse("latest", spigotPluginId, currentDevBuild.compareTo(devBuild) >= 0);
            }
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[kokonut_ineteraction] 원본 프로젝트의 버전 확인 서버에 연결할 수 없습니다. 네트워크와 서버 상태를 확인하세요.");
        }
        return new UpdaterResponse("error", -1, false);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        InteractionVisualizer.asyncExecutorManager.runTaskLaterAsynchronously(() -> {
            if (InteractionVisualizer.updaterEnabled) {
                Player player = event.getPlayer();
                if (player.hasPermission("interactionvisualizer.update")) {
                    UpdaterResponse version = Updater.checkUpdate();
                    if (!version.getResult().equals("latest")) {
                        Updater.sendUpdateMessage(player, version.getResult(), version.getSpigotPluginId());
                    }
                }
            }
        }, 100);
    }

    public static class UpdaterResponse {

        private final String result;
        private final int spigotPluginId;
        private final boolean devBuildIsLatest;

        public UpdaterResponse(String result, int spigotPluginId, boolean devBuildIsLatest) {
            this.result = result;
            this.spigotPluginId = spigotPluginId;
            this.devBuildIsLatest = devBuildIsLatest;
        }

        public String getResult() {
            return result;
        }

        public int getSpigotPluginId() {
            return spigotPluginId;
        }

        public boolean isDevBuildLatest() {
            return devBuildIsLatest;
        }

    }

}
