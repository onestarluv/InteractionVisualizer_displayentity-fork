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

package com.loohp.interactionvisualizer.utils;

import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum MCVersion {

    V26_3("26.3", 42),
    UNSUPPORTED("Unsupported", null, -1);

    public static final MCVersion MINIMUM_SUPPORTED_VERSION = V26_3;

    private static final MCVersion[] SUPPORTED_VALUES = Arrays.stream(values()).filter(v -> v.isSupported()).toArray(MCVersion[]::new);

    public static MCVersion resolve() {
        MCVersion version = fromVersion(Bukkit.getVersion());
        if (version.isSupported()) {
            return version;
        }
        return fromPackageName(Bukkit.getServer().getClass().getPackage().getName());
    }

    public static MCVersion fromVersion(String bukkitVersion) {
        Pattern versionPattern = Pattern.compile("(?i)\\(MC:? ([0-9]+\\.[0-9]+(?:\\.[0-9]+)?)\\)");
        Matcher matcher = versionPattern.matcher(bukkitVersion);
        if (matcher.find()) {
            String minecraftVersion = matcher.group(1);
            for (MCVersion version : SUPPORTED_VALUES) {
                if (minecraftVersion.equals(version.getMinecraftVersion())) {
                    return version;
                }
            }
        }
        return UNSUPPORTED;
    }

    public static MCVersion fromPackageName(String packageName) {
        for (MCVersion version : SUPPORTED_VALUES) {
            if (version.hasPackageName() && packageName.contains(version.getPackageName())) {
                return version;
            }
        }
        return UNSUPPORTED;
    }

    public static MCVersion fromNumber(int number) {
        for (MCVersion version : SUPPORTED_VALUES) {
            if (version.shortNum == number) {
                return version;
            }
        }
        return UNSUPPORTED;
    }

    private final String name;
    private final String packageName;
    private final int shortNum;

    MCVersion(String name, String packageName, int shortNum) {
        this.name = name;
        this.packageName = packageName;
        this.shortNum = shortNum;
    }

    MCVersion(String name, int shortNum) {
        this(name, null, shortNum);
    }

    @Override
    public String toString() {
        return name;
    }

    public String getMinecraftVersion() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public boolean hasPackageName() {
        return packageName != null;
    }

    public int getNumber() {
        return shortNum;
    }

    public int compareWith(MCVersion version) {
        return this.shortNum - version.shortNum;
    }

    public boolean isOlderThan(MCVersion version) {
        return compareWith(version) < 0;
    }

    public boolean isOlderOrEqualTo(MCVersion version) {
        return compareWith(version) <= 0;
    }

    public boolean isNewerThan(MCVersion version) {
        return compareWith(version) > 0;
    }

    public boolean isNewerOrEqualTo(MCVersion version) {
        return compareWith(version) >= 0;
    }

    public boolean isBetweenInclusively(MCVersion v1, MCVersion v2) {
        int difference = v1.compareWith(v2);
        if (difference == 0) {
            return this.equals(v1);
        } else if (difference < 0) {
            return this.isNewerOrEqualTo(v1) && this.isOlderOrEqualTo(v2);
        } else {
            return this.isNewerOrEqualTo(v2) && this.isOlderOrEqualTo(v1);
        }
    }

    public boolean isLegacy() {
        return false;
    }

    public boolean isOld() {
        return false;
    }

    public boolean isSupported() {
        return this.shortNum >= MINIMUM_SUPPORTED_VERSION.shortNum;
    }

    public boolean isLegacyRGB() {
        return false;
    }

}
