package com.safwat.hr.controller.backendSetting;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * قراءة/كتابة application.properties للباك إند.
 * يحافظ على الترتيب والتعليقات الأصلية.
 */
public final class BackendPropertiesStore {

    public record Entry(String key, String value, String comment, int line) {
    }

    private final Path file;

    public BackendPropertiesStore(Path file) {
        this.file = file;
    }

    public Path file() {
        return file;
    }

    public boolean exists() {
        return Files.exists(file);
    }

    public List<Entry> read() throws IOException {
        if (!Files.exists(file)) return List.of();
        List<Entry> list = new ArrayList<>();
        var lines = Files.readAllLines(file);
        String pendingComment = null;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                pendingComment = null;
                continue;
            }
            if (line.startsWith("#") || line.startsWith("!")) {
                pendingComment = line.substring(1).trim();
                continue;
            }
            int eq = line.indexOf('=');
            if (eq < 0) continue;
            String k = line.substring(0, eq).trim();
            String v = line.substring(eq + 1).trim();
            list.add(new Entry(k, v, pendingComment, i));
            pendingComment = null;
        }
        return list;
    }

    public void update(String key, String newValue) throws IOException {
        if (!Files.exists(file)) return;
        var lines = new ArrayList<>(Files.readAllLines(file));
        boolean updated = false;
        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).trim();
            if (trimmed.startsWith("#") || trimmed.startsWith("!")) continue;
            int eq = trimmed.indexOf('=');
            if (eq < 0) continue;
            if (trimmed.substring(0, eq).trim().equals(key)) {
                lines.set(i, key + "=" + newValue);
                updated = true;
                break;
            }
        }
        if (!updated) lines.add(key + "=" + newValue);
        Files.write(file, lines);
    }

    public void updateBatch(java.util.Map<String, String> updates) throws IOException {
        if (!Files.exists(file)) return;
        var lines = new ArrayList<>(Files.readAllLines(file));
        for (var e : updates.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            boolean updated = false;
            for (int i = 0; i < lines.size(); i++) {
                String trimmed = lines.get(i).trim();
                if (trimmed.startsWith("#") || trimmed.startsWith("!")) continue;
                int eq = trimmed.indexOf('=');
                if (eq < 0) continue;
                if (trimmed.substring(0, eq).trim().equals(key)) {
                    lines.set(i, key + "=" + value);
                    updated = true;
                    break;
                }
            }
            if (!updated) lines.add(key + "=" + value);
        }
        Files.write(file, lines);
    }
}