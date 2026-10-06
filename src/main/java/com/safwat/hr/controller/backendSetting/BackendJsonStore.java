package com.safwat.hr.controller.backendSetting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════════
 * BackendJsonStore — قراءة/كتابة app_config.json للباك إند
 * ══════════════════════════════════════════════════════════════════
 * <p>
 * يستخدم Jackson (نفس ما يستخدمه الباك إند) لضمان:
 * <ul>
 *   <li>الحفاظ على الأنواع (boolean / int / double / string)</li>
 *   <li>الحفاظ على بنية JSON الأصلية</li>
 * </ul>
 * <p>
 * يدعم بنية مستويين: section.key
 */
public final class BackendJsonStore {

    public record Entry(
            String path,     // "setting.scaleUp"
            String section,  // "setting"
            String key,      // "scaleUp"
            String value,    // "true" / "15" / "سوهاج"
            String type      // BOOLEAN / INT / DOUBLE / STRING / OTHER
    ) {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path file;

    public BackendJsonStore(Path file) {
        this.file = file;
    }

    public Path file() {
        return file;
    }

    public boolean exists() {
        return Files.exists(file);
    }

    /**
     * يقرأ كل المفاتيح مسطّحة.
     */
    public List<Entry> read() throws IOException {
        if (!Files.exists(file)) return List.of();

        JsonNode root = MAPPER.readTree(file.toFile());
        List<Entry> out = new ArrayList<>();

        root.fields().forEachRemaining(sectionEntry -> {
            String sectionName = sectionEntry.getKey();
            JsonNode sectionNode = sectionEntry.getValue();

            if (sectionNode.isObject()) {
                sectionNode.fields().forEachRemaining(kv -> {
                    String key = kv.getKey();
                    JsonNode val = kv.getValue();
                    out.add(new Entry(
                            sectionName + "." + key,
                            sectionName,
                            key,
                            jsonToDisplay(val),
                            detectType(val)
                    ));
                });
            } else {
                // قيمة سطحية (نادر — لكن نحسبها)
                out.add(new Entry(
                        sectionName,
                        "",
                        sectionName,
                        jsonToDisplay(sectionNode),
                        detectType(sectionNode)
                ));
            }
        });

        return out;
    }

    /**
     * يكتب قيمة واحدة مع الحفاظ على النوع الأصلي إن وُجد.
     */
    public void update(String path, String newValue) throws IOException {
        if (!Files.exists(file)) return;

        JsonNode rootNode = MAPPER.readTree(file.toFile());
        if (!(rootNode instanceof ObjectNode root)) {
            throw new IOException("الملف ليس JSON Object صالح");
        }

        String[] parts = path.split("\\.", 2);
        if (parts.length < 2) return;
        String section = parts[0];
        String key = parts[1];

        // ✅ الحل: نُعلن sectionObj قبل الـ if، وندخل الـ pattern داخل فرع
        ObjectNode sectionObj;
        JsonNode sectionNode = root.get(section);
        if (sectionNode instanceof ObjectNode obj) {
            sectionObj = obj;
        } else {
            sectionObj = MAPPER.createObjectNode();
            root.set(section, sectionObj);
        }

        // ── اكتب بالنوع الأصلي لو موجود ──
        JsonNode existing = sectionObj.get(key);
        JsonNode newTyped = coerce(newValue, existing);
        sectionObj.set(key, newTyped);

        writePretty(root);
    }

    /**
     * يكتب عدة قيم في عملية واحدة.
     */
    public void updateBatch(java.util.Map<String, String> updates) throws IOException {
        if (!Files.exists(file) || updates.isEmpty()) return;

        JsonNode rootNode = MAPPER.readTree(file.toFile());
        if (!(rootNode instanceof ObjectNode root)) {
            throw new IOException("الملف ليس JSON Object صالح");
        }

        for (var e : updates.entrySet()) {
            String[] parts = e.getKey().split("\\.", 2);
            if (parts.length < 2) continue;

            String section = parts[0];
            String key = parts[1];

            // ✅ نفس الحل
            ObjectNode sectionObj;
            JsonNode sectionNode = root.get(section);
            if (sectionNode instanceof ObjectNode obj) {
                sectionObj = obj;
            } else {
                sectionObj = MAPPER.createObjectNode();
                root.set(section, sectionObj);
            }

            JsonNode existing = sectionObj.get(key);
            sectionObj.set(key, coerce(e.getValue(), existing));
        }

        writePretty(root);
    }

    /**
     * ينشئ الملف بالإعدادات الافتراضية المطابقة للباك إند.
     */
    public void createDefaults() throws IOException {
        if (Files.exists(file)) return;

        Path parent = file.getParent();
        if (parent != null) Files.createDirectories(parent);

        ObjectNode root = MAPPER.createObjectNode();

        ObjectNode setting = MAPPER.createObjectNode();
        setting.put("scaleUp", false);
        setting.put("upgradeFirst", false);
        root.set("setting", setting);

        ObjectNode reward = MAPPER.createObjectNode();
        reward.put("dayCount_1", 0);
        reward.put("dayPercent_1", 0);
        reward.put("dayCount_2", 0);
        reward.put("dayPercent_2", 0);
        root.set("reward", reward);

        ObjectNode basic = MAPPER.createObjectNode();
        basic.put("government", "سوهاج");
        basic.put("organize", "مديرية التربية والتعليم");
        basic.put("sectorName", "تعليم");
        basic.put("prefix", "");
        root.set("basic", basic);

        ObjectNode startup = MAPPER.createObjectNode();
        startup.put("enabled", true);
        startup.put("folder", "scripts");
        root.set("startupScripts", startup);

        writePretty(root);
    }

    // ══════════════════════════ Helpers ══════════════════════════

    private void writePretty(JsonNode root) throws IOException {
        Files.writeString(file,
                MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root));
    }

    /**
     * يحوّل النص إلى النوع المناسب بناءً على القيمة القديمة.
     * لو القديم boolean → الجديد boolean، إلخ.
     */
    private JsonNode coerce(String value, JsonNode existing) {
        if (existing != null) {
            if (existing.isBoolean()) return MAPPER.getNodeFactory().booleanNode(
                    "true".equalsIgnoreCase(value));
            if (existing.isInt() || existing.isLong()) {
                try {
                    return MAPPER.getNodeFactory().numberNode(Long.parseLong(value.trim()));
                } catch (Exception e) {
                    return MAPPER.getNodeFactory().textNode(value);
                }
            }
            if (existing.isDouble() || existing.isFloat()) {
                try {
                    return MAPPER.getNodeFactory().numberNode(Double.parseDouble(value.trim()));
                } catch (Exception e) {
                    return MAPPER.getNodeFactory().textNode(value);
                }
            }
            // STRING أو غيره
            return MAPPER.getNodeFactory().textNode(value);
        }

        // مفيش قيمة سابقة → نستنتج من النص
        String v = value == null ? "" : value.trim();
        if ("true".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v))
            return MAPPER.getNodeFactory().booleanNode(Boolean.parseBoolean(v));
        try {
            return MAPPER.getNodeFactory().numberNode(Long.parseLong(v));
        } catch (Exception ignored) {
        }
        try {
            return MAPPER.getNodeFactory().numberNode(Double.parseDouble(v));
        } catch (Exception ignored) {
        }
        return MAPPER.getNodeFactory().textNode(value == null ? "" : value);
    }

    private String jsonToDisplay(JsonNode node) {
        if (node == null || node.isNull()) return "";
        return node.asText();
    }

    private String detectType(JsonNode node) {
        if (node == null) return "STRING";
        if (node.isBoolean()) return "BOOLEAN";
        if (node.isInt() || node.isLong()) return "INT";
        if (node.isDouble() || node.isFloat()) return "DOUBLE";
        if (node.isTextual()) return "STRING";
        if (node.isArray()) return "ARRAY";
        if (node.isObject()) return "OBJECT";
        return "OTHER";
    }
}