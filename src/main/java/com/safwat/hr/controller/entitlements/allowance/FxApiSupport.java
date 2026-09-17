package com.safwat.hr.controller.entitlements.allowance;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiResponse;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/**
 * غلاف خفيف فوق {@link ApiClient} (static بالكامل + CompletableFuture)
 * بيوفر أسلوب استدعاء موحّد (onSuccess/onError، جوّه على FX Application
 * Thread تلقائيًا) لكل شاشات البدلات — بدل ما كل controller يكرر نفس
 * منطق {@code thenAccept}/{@code Platform.runLater} بنفسه.
 *
 * <p>{@link ApiClient} بيوفر async جاهز لـ GET وPOST بس. الـ PUT
 * والـ DELETE عنده sync فقط (بيرموا IOException/InterruptedException)،
 * فبنغلفهم هنا بـ {@link CompletableFuture#supplyAsync}.
 *
 * <p>مبني على {@link ApiResponse#isSuccess()}, {@link ApiResponse#getData()},
 * {@link ApiResponse#getMessage()}.
 */
public final class FxApiSupport {

    private FxApiSupport() {
    }


    // ─────────────────────────────────────────────
    //  GET
    // ─────────────────────────────────────────────

    static <T> void get(String path, Class<T> type,
                        Consumer<T> onSuccess, Consumer<String> onError) {
        ApiClient.getAsync(path, type)
                .thenAccept(resp -> Platform.runLater(() -> handle(resp, onSuccess, onError)));
    }

    /**
     * لقوائم — لازم {@link TypeReference} جاهزة من عند الـ caller (مش
     * ممكن نبنيها جوه ميثود generic بسبب type erasure). مثال:
     * <pre>{@code FxApiSupport.getList("/api/x", new TypeReference<List<X>>() {}, onS, onE);}</pre>
     */
    public static <T> void getList(String path, TypeReference<List<T>> typeRef,
                                   Consumer<List<T>> onSuccess, Consumer<String> onError) {
        ApiClient.getAsync(path, Map.of(), typeRef)
                .thenAccept(resp -> Platform.runLater(() -> handle(resp, onSuccess, onError)));
    }

    // ─────────────────────────────────────────────
    //  POST
    // ─────────────────────────────────────────────

    public static <T> void post(String path, Object body, Class<T> type,
                                Consumer<T> onSuccess, Consumer<String> onError) {
        ApiClient.postAsync(path, body, type)
                .thenAccept(resp -> Platform.runLater(() -> handle(resp, onSuccess, onError)));
    }

    // ─────────────────────────────────────────────
    //  PUT — مفيش async جاهزة في ApiClient، بنغلفها إحنا
    // ─────────────────────────────────────────────

    public static <T> void put(String path, Object body, Class<T> type,
                               Consumer<T> onSuccess, Consumer<String> onError) {
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return ApiClient.put(path, body, type);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                })
                .whenComplete((resp, ex) -> Platform.runLater(() -> {
                    if (ex != null) onError.accept(rootMessage(ex));
                    else handle(resp, onSuccess, onError);
                }));
    }

    // ─────────────────────────────────────────────
    //  DELETE — مفيش async جاهزة برضه.
    //  كل endpoints الحذف في المشروع بترجع ApiResponse<Boolean>
    //  (الباك اند بيلفها بـ Optional.of(true))
    // ─────────────────────────────────────────────

    public static void delete(String path, Runnable onSuccess, Consumer<String> onError) {
        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return ApiClient.delete(path, Boolean.class);
                    } catch (Exception e) {
                        throw new CompletionException(e);
                    }
                })
                .whenComplete((resp, ex) -> Platform.runLater(() -> {
                    if (ex != null) {
                        onError.accept(rootMessage(ex));
                    } else if (resp != null && resp.isSuccess()) {
                        onSuccess.run();
                    } else {
                        onError.accept(resp != null ? resp.getMessage() : "فشل الاتصال بالسيرفر");
                    }
                }));
    }

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────

    private static <T> void handle(ApiResponse<T> resp, Consumer<T> onSuccess, Consumer<String> onError) {
        if (resp != null && resp.isSuccess()) {
            onSuccess.accept(resp.getData());
        } else {
            onError.accept(resp != null ? resp.getMessage() : "فشل الاتصال بالسيرفر");
        }
    }

    private static String rootMessage(Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        return cause.getMessage() != null ? cause.getMessage() : cause.toString();


    }

    public static void downloadBinary(
            String path,
            String defaultFileName,
            java.util.function.Consumer<String> onSuccess,
            java.util.function.Consumer<String> onError
    ) {
        ApiClient.downloadBinaryAsync(path)
                .whenComplete((bytes, ex) -> Platform.runLater(() -> {
                    if (ex != null) {
                        onError.accept(rootMessage(ex));
                        return;
                    }
                    if (bytes == null || bytes.length == 0) {
                        onError.accept("الملف فارغ أو فشل التحميل");
                        return;
                    }

                    FileChooser fc = new FileChooser();
                    fc.setTitle("احفظ التقرير");
                    fc.setInitialFileName(defaultFileName);

                    String ext = defaultFileName.contains(".")
                            ? defaultFileName.substring(defaultFileName.lastIndexOf('.') + 1)
                            : "*";
                    fc.getExtensionFilters().add(
                            new FileChooser.ExtensionFilter(ext.toUpperCase() + " Files", "*." + ext));

                    Window owner = null;
                    try {
                        owner = javafx.stage.Window.getWindows().stream()
                                .filter(Window::isFocused)
                                .findFirst()
                                .orElse(null);
                    } catch (Exception ignored) {
                    }

                    File file = fc.showSaveDialog(owner);
                    if (file == null) {
                        onSuccess.accept("(تم الإلغاء)");
                        return;
                    }

                    try {
                        Files.write(file.toPath(), bytes);
                        onSuccess.accept(file.getAbsolutePath());
                    } catch (Exception e) {
                        onError.accept("فشل حفظ الملف: " + e.getMessage());
                    }
                }));
    }
}
