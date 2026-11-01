package com.loohp.interactionvisualizer.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import com.loohp.yamlconfiguration.YamlConfiguration;

/** Immutable configuration shared by the whole server; independent of player preferences. */
public record ServerSettings(Map<String, Object> values) {
    public static final ServerSettings INSTANCE = defaults();

    public ServerSettings {
        Map<String, Object> frozen = new LinkedHashMap<>();
        values.forEach((key, value) -> frozen.put(key, freeze(value)));
        values = Map.copyOf(frozen);
    }

    private static Object freeze(Object value) {
        if (value instanceof List<?> list) return list.stream().map(ServerSettings::freeze).toList();
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> frozen = new LinkedHashMap<>();
            map.forEach((key, item) -> frozen.put(key.toString(), freeze(item)));
            return Map.copyOf(frozen);
        }
        return value;
    }

    /** Import existing server settings without rewriting or deleting the user's file. */
    public static ServerSettings importLegacy(File file) throws IOException {
        if (!file.isFile()) return INSTANCE;
        YamlConfiguration legacy = new YamlConfiguration(file, false, false);
        Map<String, Object> values = new LinkedHashMap<>(INSTANCE.values());
        try (var stream = ServerSettings.class.getClassLoader().getResourceAsStream("legacy-command-messages.yml")) {
            YamlConfiguration oldDefaults = stream == null ? null : new YamlConfiguration(stream);
            for (var entry : legacy.getValues(true).entrySet()) {
                String path = entry.getKey();
                if (!legacy.isConfigurationSection(path) && entry.getValue() != null) {
                    if (oldDefaults != null && entry.getValue().equals(oldDefaults.get(path)) && values.containsKey(path)) continue;
                    values.put(path, entry.getValue());
                }
            }
        }
        return new ServerSettings(values);
    }

    private static ServerSettings defaults() {
        Map<String, Object> values = new LinkedHashMap<>();
        for (ServerSetting setting : ServerSetting.values()) values.put(setting.path(), setting.value());
        return new ServerSettings(values);
    }

    public Object get(String path) { return values.get(path); }
    public boolean contains(String path) { return values.containsKey(path) || isConfigurationSection(path); }
    public List<?> getList(String path) { return get(path) instanceof List<?> list ? list : List.of(); }
    public String getString(String path) { return getString(path, null); }
    public String getString(String path, String fallback) {
        Object value = get(path);
        return value instanceof String text ? text : fallback;
    }
    public boolean getBoolean(String path) { return Boolean.parseBoolean(String.valueOf(get(path))); }
    private Number number(String path) {
        Object value = get(path);
        if (value instanceof Number n) return n;
        try { return Double.valueOf(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return 0; }
    }
    public int getInt(String path) { return number(path).intValue(); }
    public long getLong(String path) { return number(path).longValue(); }
    public double getDouble(String path) { return number(path).doubleValue(); }
    public List<String> getStringList(String path) {
        return get(path) instanceof List<?> list ? list.stream().filter(String.class::isInstance).map(String.class::cast).toList() : List.of();
    }
    public boolean isConfigurationSection(String path) {
        return get(path) instanceof Map || values.keySet().stream().anyMatch(key -> key.startsWith(path + "."));
    }
    public ServerSettings getConfigurationSection(String path) {
        Map<String, Object> section = new LinkedHashMap<>();
        String prefix = path + ".";
        values.forEach((key, value) -> { if (key.startsWith(prefix)) section.put(key.substring(prefix.length()), value); });
        return new ServerSettings(section);
    }
    public Map<String, Object> getValues(boolean deep) {
        if (deep) return values;
        Map<String, Object> direct = new LinkedHashMap<>();
        values.forEach((key, value) -> { if (!key.contains(".")) direct.put(key, value); });
        return Map.copyOf(direct);
    }
}
