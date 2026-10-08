package com.safwat.hr.controller.backendSetting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * ══════════════════════════════════════════════════════════════════
 * BackendJsonStore — قراءة/كتابة app_config.json
 * ══════════════════════════════════════════════════════════════════
 * <p>
 * بيستخدم للباك إند والفرونت. الفرق بينهم بيتحدد بالـ {@link Docs} اللي بتتبعت:
 * <ul>
 *   <li>{@link #BACKEND_DOCS}: بيضيف تعليقات `_comment_*` للمفاتيح الناقصة عند الكتابة.</li>
 *   <li>{@code new BackendJsonStore(file)}: بيحافظ على التعليقات الموجودة بس، ومايضيفش تعليقات جديدة.</li>
 * </ul>
 * <p>
 * التعليقات بتتخزن في الملف بالشكل ده:
 * <pre>
 * "_comment_setting": "إعدادات عامة",        ← تعليق قسم
 * "setting": {
 *   "_comment_scaleUp": "تطبيق زيادة...",   ← تعليق مفتاح
 *   "scaleUp": false
 * }
 * </pre>
 * <p>
 * {@link #read()} بيرجع البيانات بس (من غير التعليقات)، و{@link #comments()} بيرجع التعليقات.
 */
public final class BackendJsonStore {

    public record Entry(
            String path,     // "setting.scaleUp"
            String section,  // "setting"
            String key,      // "scaleUp"
            String value,    // "true" / "15" / "سوهاج"
            String type      // BOOLEAN / INT / DOUBLE / STRING / ARRAY / OBJECT / OTHER
    ) {
    }

    /**
     * تعريفات التعليقات: تعليق كل قسم، وتعليق كل مفتاح داخل كل قسم.
     */
    public record Docs(Map<String, String> sections, Map<String, Map<String, String>> keys) {
        public static final Docs NONE = new Docs(Map.of(), Map.of());
    }

    /** بادئة مفاتيح التعليقات. */
    public static final String COMMENT_PREFIX = "_comment_";

    private static final Map<String, String> BACKEND_SECTION_DOCS = Map.of(
            "setting", "إعدادات عامة للبرنامج",
            "reward", "إعدادات المكافآت (عدد الأيام ونسبتها)",
            "basic", "بيانات الجهة الأساسية (تظهر في التقارير)",
            "startupScripts", "إعدادات تشغيل سكريبتات قاعدة البيانات عند بدء التطبيق"
    );

    private static final Map<String, Map<String, String>> BACKEND_KEY_DOCS = Map.of(
            "setting", Map.of(
                    "scaleUp", "تطبيق زيادة المقياس على الأجور (true/false)",
                    "upgradeFirst", "تنفيذ الترقية قبل الاحتساب (true/false)"
            ),
            "reward", Map.of(
                    "dayCount_1", "عدد الأيام للشريحة الأولى",
                    "dayPercent_1", "نسبة المكافأة للشريحة الأولى (%)",
                    "dayCount_2", "عدد الأيام للشريحة الثانية",
                    "dayPercent_2", "نسبة المكافأة للشريحة الثانية (%)"
            ),
            "basic", Map.of(
                    "government", "اسم المحافظة",
                    "organize", "اسم الجهة (يظهر في رأس التقارير)",
                    "sectorName", "اسم القطاع",
                    "prefix", "بادئة تُضاف لأرقام المستندات (اختياري)"
            ),
            "startupScripts", Map.of(
                    "enabled", "تشغيل سكريبتات البدء التلقائي (true/false)",
                    "folder", "اسم مجلد السكريبتات داخل جذر التخزين"
            )
    );

    /** تعريفات تعليقات الباك إند (نفس اللي في AppConfig). */
    public static final Docs BACKEND_DOCS = new Docs(BACKEND_SECTION_DOCS, BACKEND_KEY_DOCS);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Path file;
    private final Docs docs;

    public BackendJsonStore(Path file) {
        this(file, Docs.NONE);
    }

    public BackendJsonStore(Path file, Docs docs) {
        this.file = file;
        this.docs = docs == null ? Docs.NONE : docs;
    }

    public Path file() {
        return file;
    }

    public boolean exists() {
        return Files.exists(file);
    }

    // ══════════════════════════ Read ══════════════════════════

    /**
     * يقرأ كل المفاتيح مسطّحة، من غير مفاتيح التعليقات.
     */
    public List<Entry> read() throws IOException {
        if (!Files.exists(file)) return List.of();

        ObjectNode root = readRootObject();
        List<Entry> out = new ArrayList<>();

        forEachField(root, (sectionName, sectionNode) -> {
            if (isComment(sectionName)) return;

            if (sectionNode.isObject()) {
                forEachField(sectionNode, (key, val) -> {
                    if (isComment(key)) return;
                    out.add(new Entry(
                            sectionName + "." + key,
                            sectionName,
                            key,
                            display(val),
                            typeOf(val)));
                });
            } else {
                // قيمة سطحية على مستوى الجذر (نادرة)
                out.add(new Entry(
                        sectionName,
                        "",
                        sectionName,
                        display(sectionNode),
                        typeOf(sectionNode)));
            }
        });

        return out;
    }

    /**
     * يرجع التعليقات: المفتاح = مسار القسم أو المفتاح ("setting" أو "setting.scaleUp")،
     * والقيمة = نص التعليق.
     */
    public Map<String, String> comments() throws IOException {
        Map<String, String> out = new LinkedHashMap<>();
        if (!Files.exists(file)) return out;

        ObjectNode root = readRootObject();
        forEachField(root, (name, value) -> {
            if (isComment(name)) {
                out.put(name.substring(COMMENT_PREFIX.length()), display(value));
                return;
            }
            if (value.isObject()) {
                forEachField(value, (key, v) -> {
                    if (isComment(key)) {
                        out.put(name + "." + key.substring(COMMENT_PREFIX.length()), display(v));
                    }
                });
            }
        });
        return out;
    }

    // ══════════════════════════ Write ══════════════════════════

    /**
     * يكتب قيمة واحدة مع الحفاظ على النوع الأصلي إن وُجد.
     */
    public void update(String path, String newValue) throws IOException {
        updateBatch(Collections.singletonMap(path, newValue));
    }

    /**
     * يكتب عدة قيم في عملية واحدة. لو فيه مسار غلط، مايتكتبش أي حاجة.
     */
    public void updateBatch(Map<String, String> updates) throws IOException {
        if (updates == null || updates.isEmpty() || !Files.exists(file)) return;

        // التحقق من كل المسارات قبل أي كتابة
        for (String path : updates.keySet()) {
            if (path == null || path.isBlank() || isCommentPath(path)) {
                throw new IllegalArgumentException("مسار غير صالح: " + path);
            }
            if (path.split("\\.", 2).length < 2) {
                throw new IllegalArgumentException("المسار لازم يكون section.key: " + path);
            }
        }

        ObjectNode root = readRootObject();

        for (Map.Entry<String, String> e : updates.entrySet()) {
            String[] parts = e.getKey().split("\\.", 2);
            ObjectNode section = sectionOf(root, parts[0]);
            JsonNode existing = section.get(parts[1]);
            section.set(parts[1], coerce(e.getValue(), existing));
        }

        writeRoot(root);
    }

    /**
     * ينشئ الملف بالإعدادات الافتراضية. لو الملف موجود مابيعملش حاجة.
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

        writeRoot(root);
    }

    // ══════════════════════════ Helpers ══════════════════════════

    private ObjectNode readRootObject() throws IOException {
        JsonNode node = MAPPER.readTree(Files.readString(file, StandardCharsets.UTF_8));
        if (node instanceof ObjectNode obj) return obj;
        throw new IOException("الملف ليس JSON Object صالح");
    }

    /**
     * يكتب الملف بعد إضافة التعليقات الناقصة. الكتابة بتتعمل في ملف مؤقت الأول،
     * عشان لو حصل خطأ في النص ما يتبوظش الملف الأصلي.
     */
    private void writeRoot(ObjectNode root) throws IOException {
        String json = MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsString(withComments(root));

        Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
        Files.writeString(tmp, json, StandardCharsets.UTF_8);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * يرجّع نسخة من الـ JSON فيها تعليق قبل كل قسم ومفتاح.
     * التعليق الموجود في الملف بياخد الأولوية، والتعليق الافتراضي بيتضاف بس لو مفيش.
     */
    private ObjectNode withComments(ObjectNode src) {
        ObjectNode out = MAPPER.createObjectNode();

        forEachField(src, (name, value) -> {
            if (isComment(name)) return;

            if (value instanceof ObjectNode section) {
                String doc = existingText(src, COMMENT_PREFIX + name);
                if (doc == null) doc = docs.sections().get(name);
                if (doc != null) out.put(COMMENT_PREFIX + name, doc);
                out.set(name, sectionWithComments(name, section));
            } else {
                out.set(name, value);
            }
        });
        return out;
    }

    private ObjectNode sectionWithComments(String sectionName, ObjectNode section) {
        ObjectNode out = MAPPER.createObjectNode();
        Map<String, String> keyDocs = docs.keys().getOrDefault(sectionName, Map.of());

        forEachField(section, (key, value) -> {
            if (isComment(key)) return;

            String doc = existingText(section, COMMENT_PREFIX + key);
            if (doc == null) doc = keyDocs.get(key);
            if (doc != null) out.put(COMMENT_PREFIX + key, doc);
            out.set(key, value);
        });
        return out;
    }

    private ObjectNode sectionOf(ObjectNode root, String name) {
        JsonNode existing = root.get(name);
        if (existing instanceof ObjectNode obj) return obj;

        ObjectNode created = MAPPER.createObjectNode();
        root.set(name, created);
        return created;
    }

    /**
     * يحوّل النص للنوع المناسب بناءً على القيمة القديمة.
     * لو مفيش قيمة قديمة، بيستنتج النوع من النص.
     */
    private JsonNode coerce(String value, JsonNode existing) {
        String raw = value == null ? "" : value;
        String v = raw.trim();

        if (existing != null && !existing.isNull() && !existing.isMissingNode()) {
            if (existing.isBoolean()) {
                return MAPPER.getNodeFactory().booleanNode("true".equalsIgnoreCase(v));
            }
            if (existing.isInt() || existing.isLong()) {
                try {
                    return MAPPER.getNodeFactory().numberNode(Long.parseLong(v));
                } catch (NumberFormatException e) {
                    return MAPPER.getNodeFactory().textNode(raw);
                }
            }
            if (existing.isDouble() || existing.isFloat()) {
                try {
                    return MAPPER.getNodeFactory().numberNode(Double.parseDouble(v));
                } catch (NumberFormatException e) {
                    return MAPPER.getNodeFactory().textNode(raw);
                }
            }
            return MAPPER.getNodeFactory().textNode(raw);
        }

        // مفيش قيمة قديمة → نستنتج النوع من النص
        if ("true".equalsIgnoreCase(v) || "false".equalsIgnoreCase(v)) {
            return MAPPER.getNodeFactory().booleanNode(Boolean.parseBoolean(v));
        }
        try {
            return MAPPER.getNodeFactory().numberNode(Long.parseLong(v));
        } catch (NumberFormatException ignored) {
        }
        try {
            return MAPPER.getNodeFactory().numberNode(Double.parseDouble(v));
        } catch (NumberFormatException ignored) {
        }
        return MAPPER.getNodeFactory().textNode(raw);
    }

    private static void forEachField(JsonNode node, BiConsumer<String, JsonNode> action) {
        Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            action.accept(e.getKey(), e.getValue());
        }
    }

    private static String existingText(ObjectNode node, String key) {
        JsonNode n = node.get(key);
        return (n == null || n.isNull()) ? null : n.asText();
    }

    private static boolean isComment(String name) {
        return name != null && name.startsWith(COMMENT_PREFIX);
    }

    private static boolean isCommentPath(String path) {
        return path.contains(COMMENT_PREFIX);
    }

    private static String display(JsonNode node) {
        if (node == null || node.isNull()) return "";
        return node.asText();
    }

    private static String typeOf(JsonNode node) {
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