package com.safwat.hr.shared.ui;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * نطق النصوص العربية (يحاكي TextToSpeech القديم).
 * <p>يستخدم خدمة ترجمة جوجل الصوتية عبر MediaPlayer.
 * إن فشل التشغيل لأي سبب (لا يوجد إنترنت/Codec) يتم التجاهل بصمت
 * حتى لا تتعطل عملية الإدخال.</p>
 */
public final class TextToSpeech {

    private static MediaPlayer currentPlayer;

    private TextToSpeech() {
    }

    /**
     * نطق نص عربي (يُستدعى غالباً لنطق اسم الموظف الناتج من البحث)
     *
     * @param text النص المراد نطقه
     */
    public static synchronized void speak(String text) {
        if (text == null || text.isBlank()) return;
        try {
            stop();
            String url = "https://translate.google.com/translate_tts?ie=UTF-8&tl=ar&client=tw-ob&q="
                    + URLEncoder.encode(text, StandardCharsets.UTF_8);
            currentPlayer = new MediaPlayer(new Media(url));
            currentPlayer.setOnEndOfMedia(TextToSpeech::stop);
            currentPlayer.play();
        } catch (Exception ignored) {
            // لا نطق متاح — نتجاهل بصمت
        }
    }

    public static synchronized void stop() {
        if (currentPlayer != null) {
            try {
                currentPlayer.stop();
                currentPlayer.dispose();
            } catch (Exception ignored) {
            }
            currentPlayer = null;
        }
    }
}