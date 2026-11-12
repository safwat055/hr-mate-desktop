package com.safwat.hr.ui.util;

import com.safwat.hr.controller.login.CentralController;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class PDFView {

    /**
     * رسالة مميزة بيبعتها JS لما المستخدم يضغط طباعة
     */
    private static final String PRINT_SIGNAL = "__SAF_PRINT__";

    private static final String[] VIEWER_PATHS = {
            "subresource/pdfjs/web/viewer.html",
            "app/pdfjs/web/viewer.html",
            "pdfjs/web/viewer.html",
            "src/main/resources/pdfjs/web/viewer.html"
    };

    /**
     * يعرض الـ PDF في WebView موجود. يقدر يتنادى أكتر من مرة على نفس الـ WebView.
     */
    public static void showIN(String pdfPath, WebView webView) {
        try {
            File viewerFile = findViewer();
            File pdfFile = new File(pdfPath);
            if (!pdfFile.exists()) {
                throw new RuntimeException("PDF file not found: " + pdfPath);
            }

            String pdfUrl = pdfFile.toURI().toURL().toString();
            String viewerUrl = viewerFile.toURI().toURL().toString();
            String finalUrl = viewerUrl + "?file="
                    + URLEncoder.encode(pdfUrl, StandardCharsets.UTF_8);

            WebEngine engine = webView.getEngine();
            Path pdfPathObj = pdfFile.toPath();

            // listener واحد بيتسجل مرة واحدة على الـ engine
            engine.setOnAlert(event -> {
                if (PRINT_SIGNAL.equals(event.getData())) {
                    Platform.runLater(() -> PrintDialogService.openDialog(pdfPathObj));
                }
            });

            // بعد كل تحميل نحقن الـ override لـ window.print
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    engine.executeScript(PRINT_INTERCEPT_JS);
                }
            });

            engine.load(finalUrl);

        } catch (Exception e) {
            e.printStackTrace();
            webView.getEngine().loadContent(
                    "<html><body style='text-align: center; padding: 50px;'>"
                            + "<h2>❌ خطأ في تحميل الملف</h2>"
                            + "<p>" + e.getMessage() + "</p>"
                            + "<p>Path: " + pdfPath + "</p>"
                            + "</body></html>"
            );
        }
    }

    public static WebView createViewer(String pdfPath) {
        WebView webView = new WebView();
        showIN(pdfPath, webView);
        return webView;
    }

    public static void shoePdf(String path, String mainTabPane) {
        TabPane tabPane = CentralController.getInstance().getComponent(mainTabPane, TabPane.class);

        for (Tab tab : tabPane.getTabs()) {
            if ("عارض الملف".equals(tab.getText())) {
                WebView existing = (WebView) tab.getContent();
                showIN(path, existing);
                tabPane.getSelectionModel().select(tab);
                return;
            }
        }

        WebView webView = new WebView();
        showIN(path, webView);

        Tab pdfTab = new Tab("عارض الملف", webView);
        pdfTab.setClosable(true);
        tabPane.getTabs().add(pdfTab);
        tabPane.getSelectionModel().select(pdfTab);
    }

    private static File findViewer() {
        for (String path : VIEWER_PATHS) {
            File f = new File(path);
            if (f.exists()) return f;
        }
        throw new RuntimeException("viewer.html not found in known locations");
    }

    /**
     * يعترض window.print ويبعت إشارة عن طريق alert، من غير أي bridge.
     */
    private static final String PRINT_INTERCEPT_JS = """
            (function () {
                if (window.__safPrintHooked) return;
                window.__safPrintHooked = true;
                window.print = function () {
                    alert('__SAF_PRINT__');
                };
                document.addEventListener('keydown', function (e) {
                    if ((e.ctrlKey || e.metaKey) && e.key === 'p') {
                        e.preventDefault();
                        alert('__SAF_PRINT__');
                    }
                }, true);
            })();
            """;
}