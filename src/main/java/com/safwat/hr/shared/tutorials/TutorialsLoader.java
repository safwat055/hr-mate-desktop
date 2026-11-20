package com.safwat.hr.shared.tutorials;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * تحميل شروحات التطبيق + فتح روابط يوتيوب في المتصفح (Windows / Linux / macOS).
 *
 * <p>الملف بيتعمل في مسار التشغيل ({@code user.dir/tutorials.json}) من الكود نفسه،
 * ومحتواه الافتراضي متعرّف في {@link #createDefaultContent()}.
 *
 * <p>لتحديث الشروحات في النسخ الجاية: عدّل {@link #createDefaultContent()} وزوّد {@link #DEFAULT_VERSION}.
 */
public final class TutorialsLoader {

    public static final String FILE_NAME = "tutorials.json";

    /** رقم نسخة الشروحات المدمجة في الكود. زوّده مع كل تعديل على المحتوى */
    private static final int DEFAULT_VERSION = 2;
    private static final String DEFAULT_UPDATED_AT = "2026-11-21";

    private static final Pattern VIDEO_ID_PATTERN = Pattern.compile(
            "(?:v=|youtu\\.be/|shorts/|embed/)([A-Za-z0-9_-]{11})");

    /** أقصى وقت ننتظر فيه أمر فتح المتصفح قبل ما نعتبره نجح */
    private static final long OPEN_TIMEOUT_SECONDS = 5;

    private TutorialsLoader() {
    }

    // ══════════════════════════════════════════════════════════════
    //  Models
    // ══════════════════════════════════════════════════════════════

    public enum Source {
        EXTERNAL,          // الملف اللي في مسار التشغيل
        BUNDLED,           // النسخة المدمجة في الكود
        BUNDLED_FALLBACK   // الملف الخارجي موجود بس بايظ، والمعروض النسخة المدمجة
    }

    public record Tutorial(String id, String title, String description,
                           String youtubeUrl, String videoId) {

        public boolean hasVideo() {
            return videoId != null;
        }

        /** رابط الصورة المصغرة من يوتيوب، null لو مفيش فيديو */
        public String thumbnailUrl() {
            return hasVideo() ? "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg" : null;
        }
    }

    public record TutorialsFile(int version, String updatedAt, List<Tutorial> sections, Source source) {
    }

    // ══════════════════════════════════════════════════════════════
    //  Shared content API
    // ══════════════════════════════════════════════════════════════

    /** مسار الملف الخارجي في مسار التشغيل */
    public static Path externalPath() {
        return Path.of(System.getProperty("user.dir"), FILE_NAME);
    }

    /** يرجّع أحدث نسخة متاحة، ومينفعش يرجع null */
    public static TutorialsFile load() {
        ensureExternalExists();

        Path external = externalPath();
        TutorialsFile bundled = parse(createDefaultContent());

        if (!Files.isRegularFile(external)) {
            return withSource(bundled, Source.BUNDLED);
        }

        TutorialsFile externalData = tryLoadExternal(external);
        if (externalData == null) {
            return withSource(bundled, Source.BUNDLED_FALLBACK);
        }
        if (externalData.version() >= bundled.version()) {
            return withSource(externalData, Source.EXTERNAL);
        }
        return withSource(bundled, Source.BUNDLED);
    }

    /** يكتب النسخة الافتراضية فوق الملف الخارجي (بيمسح أي تعديل) */
    public static void resetExternalFile() throws IOException {
        Files.writeString(externalPath(), createDefaultContent().toString(2), StandardCharsets.UTF_8);
    }

    // ══════════════════════════════════════════════════════════════
    //  Opening links (non-blocking, cross-platform)
    // ══════════════════════════════════════════════════════════════

    /**
     * يفتح الرابط في المتصفح الافتراضي بدون ما يوقف الواجهة.
     *
     * <p>بيرجّع {@link CompletableFuture} بيتعمل {@code completeExceptionally} لو الفتح فشل،
     * فالـ caller يقدر يعرض رسالة بعد ما يعمل {@code Platform.runLater}.
     */
    public static CompletableFuture<Void> openInBrowser(String url) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Thread worker = new Thread(() -> {
            try {
                validateHttpUrl(url);
                openWithOsCommand(url);
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }, "tutorials-open-browser");
        worker.setDaemon(true);
        worker.start();

        return future;
    }

    // ══════════════════════════════════════════════════════════════
    //  Opening helpers
    // ══════════════════════════════════════════════════════════════

    /** نقبل http و https بس، عشان ما نفتحش أي scheme تاني بالغلط */
    private static void validateHttpUrl(String url) throws IOException {
        if (url == null || url.isBlank()) {
            throw new IOException("الرابط فاضي");
        }
        URI uri;
        try {
            uri = new URI(url.trim());
        } catch (Exception e) {
            throw new IOException("الرابط غير صالح: " + url, e);
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IOException("الرابط لازم يبدأ بـ http أو https");
        }
    }

    private static void openWithOsCommand(String url) throws Exception {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        if (os.contains("win")) {
            // rundll32 بيفتح الرابط من غير shell، فمفيش مشكلة مع & في الروابط
            runAndWait(new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url));
            return;
        }

        if (os.contains("mac")) {
            runAndWait(new ProcessBuilder("open", url));
            return;
        }

        if (os.contains("linux") || os.contains("nix") || os.contains("nux")) {
            try {
                runAndWait(new ProcessBuilder("xdg-open", url));
                return;
            } catch (Exception xdgFailed) {
                // لو xdg-open مش موجود نجرب Desktop API كـ fallback
                if (tryDesktopBrowse(url)) return;
                throw xdgFailed;
            }
        }

        // أي نظام تاني: نجرب Desktop API
        if (!tryDesktopBrowse(url)) {
            throw new UnsupportedOperationException("فتح المتصفح مش مدعوم على الجهاز ده");
        }
    }

    /**
     * يشغّل الأمر وينتظر حد أقصى {@link #OPEN_TIMEOUT_SECONDS} ثانية.
     * لو الأمر خلص بكود خطأ نرمي exception، ولو انتهى الوقت نعتبره نجح
     * (المتصفح ممكن يكون بيفتح ومش خلص).
     */
    private static void runAndWait(ProcessBuilder pb) throws Exception {
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        Process process = pb.start();

        boolean finished = process.waitFor(OPEN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (finished && process.exitValue() != 0) {
            throw new IOException("الأمر " + pb.command().get(0) + " فشل بكود " + process.exitValue());
        }
    }

    private static boolean tryDesktopBrowse(String url) {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return true;
            }
        } catch (Exception ignored) {
            // نرجّع false ونسيب الـ caller يتعامل مع الفشل
        }
        return false;
    }

    // ══════════════════════════════════════════════════════════════
    //  Default content (المصدر الوحيد للمحتوى الافتراضي)
    // ══════════════════════════════════════════════════════════════

    /**
     * المحتوى الافتراضي للشروحات. ده اللي بيتكتب في ملف {@code tutorials.json}
     * ويتعرض كـ fallback.
     */
    static JSONObject createDefaultContent() {
        JSONObject root = new JSONObject();
        root.put("version", DEFAULT_VERSION);
        root.put("updatedAt", DEFAULT_UPDATED_AT);

        JSONArray sections = new JSONArray();

        sections.put(section("all-tutorials",
                "كل شروحات البرنامج",
                "كل شروحات البرنامج",
                "https://youtube.com/playlist?list=PLVfF7h2n8YhM&si=y9nvv-Ol5MY3y1Tl"));

        sections.put(section("reports-upload",
                "شرح تحميل التقارير",
                "شرح كامل لتحميل تقارير المنظومة: مراجعة، وسنوي، وأجر اشتراك، وفهرس.",
                "https://youtu.be/bycxuzEUK_w"));

        sections.put(section("deductions-manager",
                "شرح واجهة مدير الاستحقاقات",
                "شرح واجهة مدير الاستحقاقات وإدارة البنود والمجموعات.",
                "https://youtu.be/F4syhq9IoEE"));

        sections.put(section("payroll-vocabulary",
                "شرح واجهة مفردات بيرول",
                "شرح واجهة مفردات بيرول وطريقة التعامل معها.",
                "https://youtu.be/BXHO97gWPOE"));

        sections.put(section("payroll-payReport-Ui",
                "شرح واجهة تقارير الصرف",
                "شرح واجهة تقارير الصرف وطريقة التعامل معها.",
                "https://youtu.be/W3zwiNF0l-0"));

        sections.put(section("data-entry-ui",
                "شرح واجهة الإدخال",
                "شرح كامل لواجهة إدخال البيانات وخطوات الإدخال خطوة بخطوة.",
                "https://youtu.be/Ibemo9OJC0s"));

        sections.put(section("form-129",
                "شرح واجهة سجلات 129",
                "شرح كامل لواجهة سجلات 129 الإلكترونية، وطريقة التعامل معها، واستخراج سجلات 129 من التقارير.",
                "https://youtu.be/BuMHMZYTGcY"));

        sections.put(section("payroll-ChangeCard-ui",
                "شرح واجهة أجر الاشتراك",
                "شرح كامل لواجهة أجر الاشتراك وطريقة استخراج بطاقات أجر الاشتراك المحمّلة من المنظومة.",
                "https://youtu.be/X5869MzeuAI"));

        root.put("sections", sections);
        return root;
    }

    private static JSONObject section(String id, String title, String description, String youtubeUrl) {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("title", title);
        o.put("description", description);
        o.put("youtubeUrl", youtubeUrl);
        return o;
    }

    // ══════════════════════════════════════════════════════════════
    //  Loading helpers
    // ══════════════════════════════════════════════════════════════

    private static void ensureExternalExists() {
        if (Files.exists(externalPath())) return;
        try {
            resetExternalFile();
        } catch (IOException e) {
            System.err.println("[Tutorials] تعذر إنشاء الملف في مسار التشغيل: " + e.getMessage());
        }
    }

    private static TutorialsFile tryLoadExternal(Path path) {
        try {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            // Notepad على ويندوز ممكن يحفظ الملف ومعاه BOM، فبنشيله
            text = text.replace("\uFEFF", "").trim();
            return parse(new JSONObject(text));
        } catch (Exception e) {
            System.err.println("[Tutorials] الملف الخارجي بايظ: " + e.getMessage());
            return null;
        }
    }

    private static TutorialsFile parse(JSONObject root) {
        int version = root.optInt("version", 0);
        String updatedAt = root.optString("updatedAt", "");

        List<Tutorial> sections = new ArrayList<>();
        JSONArray arr = root.optJSONArray("sections");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) continue;

                String title = o.optString("title", "").trim();
                if (title.isEmpty()) continue; // القسم من غير عنوان ما يتعرضش

                String url = o.optString("youtubeUrl", "").trim();
                sections.add(new Tutorial(
                        o.optString("id", "section-" + i),
                        title,
                        o.optString("description", "").trim(),
                        url.isEmpty() ? null : url,
                        extractVideoId(url)
                ));
            }
        }
        return new TutorialsFile(version, updatedAt, sections, null);
    }

    private static TutorialsFile withSource(TutorialsFile f, Source source) {
        return new TutorialsFile(f.version(), f.updatedAt(), f.sections(), source);
    }

    /** يدعم watch?v=، youtu.be/، shorts/، embed/ */
    static String extractVideoId(String url) {
        if (url == null || url.isEmpty()) return null;
        Matcher m = VIDEO_ID_PATTERN.matcher(url);
        return m.find() ? m.group(1) : null;
    }

    // ══════════════════════════════════════════════════════════════
//  Social & project links
// ══════════════════════════════════════════════════════════════

    /** صفحة التطبيق على فيسبوك. غيّرها للرابط الحقيقي */
    private static final String APP_FACEBOOK_PAGE = "https://www.facebook.com/share/1KGb84FAXR/";

    /** قائمة فيديوهات القناة على يوتيوب. غيّرها للرابط الحقيقي */
    private static final String YOUTUBE_VIDEOS_LIST = "https://www.youtube.com/playlist?list=PLVfF7h2n8YhM";

    /** رقم واتساب بصيغة دولية بدون + ولا 00 (مصر = 20، والرقم بدون الصفر الأول) */
    private static final String WHATSAPP_PHONE = "201021816864";

    /** الفيسبوك الشخصي. غيّره للرابط الحقيقي */
    private static final String PERSONAL_FACEBOOK = "https://www.facebook.com/share/1E8BfqcJbZ/";

    private static final String GITHUB_PROFILE = "https://github.com/safwat055";
    private static final String GITHUB_PAGES = "https://safwat055.github.io/my-portfolio/";

    public static CompletableFuture<Void> openAppFacebookPage() {
        return openInBrowser(APP_FACEBOOK_PAGE);
    }

    public static CompletableFuture<Void> openYouTubeVideos() {
        return openInBrowser(YOUTUBE_VIDEOS_LIST);
    }

    public static CompletableFuture<Void> openWhatsApp() {
        return openInBrowser("https://wa.me/" + WHATSAPP_PHONE);
    }

    public static CompletableFuture<Void> openPersonalFacebook() {
        return openInBrowser(PERSONAL_FACEBOOK);
    }

    public static CompletableFuture<Void> openGitHubProfile() {
        return openInBrowser(GITHUB_PROFILE);
    }

    public static CompletableFuture<Void> openGitHubPages() {
        return openInBrowser(GITHUB_PAGES);
    }
}