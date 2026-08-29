package com.safwat.hr.ui.util;

import javafx.scene.text.Font;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * FontLoader — بيحمّل خطوط Cairo من resources ويوفّر أسماء خطوط
 * السيستم (Emoji + Arial) عشان تتحدد في CSS بشكل صريح.
 *
 * <p>الاستخدام:
 * <pre>
 *   FontLoader.load(); // قبل أي scene.getStylesheets()
 * </pre>
 */
public class FontLoader {

    private static final Logger LOG = Logger.getLogger(FontLoader.class.getName());

    /** المسار داخل resources */
    private static final String FONTS_BASE = "/com/safwat/hr/fonts/";

    /** أوزان Cairo المتاحة */
    private static final String[] CAIRO_FILES = {
            "Cairo-ExtraLight.ttf",
            "Cairo-Light.ttf",
            "Cairo-Regular.ttf",
            "Cairo-Medium.ttf",
            "Cairo-SemiBold.ttf",
            "Cairo-Bold.ttf",
            "Cairo-ExtraBold.ttf",
            "Cairo-Black.ttf",
    };

    // ── أسماء الخطوط بعد التحميل ─────────────────────────────────

    /** اسم خط Cairo كما عرّفه JavaFX بعد التحميل */
    public static final String CAIRO = "Cairo";

    /**
     * اسم خط الإيموجي المتاح على السيستم.
     * Linux: Noto Color Emoji / Twemoji
     * Windows: Segoe UI Emoji
     * macOS: Apple Color Emoji
     */
    public static final String EMOJI = resolveEmojiFont();

    /**
     * اسم خط Arial — متاح على Windows و macOS مباشرةً.
     * على Linux بييجي من wine أو من font substitution.
     * لو مش موجود، JavaFX بيرجع لـ SansSerif تلقائيًا.
     */
    public static final String ARIAL = "Arial";

    // ── تحميل الخطوط ─────────────────────────────────────────────

    private static boolean loaded = false;

    /**
     * بيحمّل كل خطوط Cairo من resources.
     * آمن يتنادى أكتر من مرة (بيتجاهل المرات الجاية بعد الأولى).
     */
    public static void load() {
        if (loaded) return;
        loaded = true;

        List<String> failed = new ArrayList<>();

        for (String file : CAIRO_FILES) {
            String path = FONTS_BASE + file;
            try (InputStream is = FontLoader.class.getResourceAsStream(path)) {
                if (is == null) {
                    failed.add(file + " (not found)");
                    continue;
                }
                Font font = Font.loadFont(is, 13);
                if (font == null) {
                    failed.add(file + " (load failed)");
                } else {
                    LOG.fine("✔ Loaded font: " + font.getName());
                }
            } catch (Exception e) {
                failed.add(file + " (" + e.getMessage() + ")");
            }
        }

        if (!failed.isEmpty()) {
            LOG.warning("FontLoader: فشل تحميل الخطوط التالية:\n  " +
                    String.join("\n  ", failed));
        } else {
            LOG.info("FontLoader: تم تحميل خطوط Cairo بنجاح ✔  |  Emoji: " + EMOJI);
        }
    }

    // ── CSS font-family string ────────────────────────────────────

    /**
     * بيرجع قيمة جاهزة للـ -fx-font-family تجمع Cairo + Emoji.
     * مثال: "Cairo, Noto Color Emoji"
     *
     * ملاحظة: JavaFX بيدعم font-family واحد بس في الـ property،
     * لكن بيعمل fallback على مستوى الـ glyph تلقائيًا لو الخط
     * الأول مش فيه الرمز المطلوب.
     */
    public static String cairoWithEmoji() {
        if (EMOJI.isEmpty()) return CAIRO;
        return CAIRO + ", " + EMOJI;
    }

    // ── Helpers ───────────────────────────────────────────────────

    /** بيكتشف أفضل خط إيموجي متاح على السيستم الحالي */
    private static String resolveEmojiFont() {
        // الخطوط مرتبة من الأفضل للأضعف على كل نظام
        String[] candidates = {
                "Noto Color Emoji",   // Linux (Ubuntu/Debian)
                "Twemoji",            // Linux (بعض التوزيعات)
                "Segoe UI Emoji",     // Windows 8.1+
                "Apple Color Emoji",  // macOS
                "EmojiOne Color",     // Linux (بديل)
        };

        for (String name : candidates) {
            // Font.font() بيرجع خط فعلي لو الاسم موجود،
            // أو يرجع default لو مش موجود — نتحقق بمقارنة الاسم
            Font f = Font.font(name, 12);
            if (f.getFamily().equalsIgnoreCase(name)) {
                return name;
            }
        }
        return ""; // مش موجود — JavaFX هيعمل fallback تلقائي
    }
}