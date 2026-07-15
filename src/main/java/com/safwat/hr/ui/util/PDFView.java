package com.safwat.hr.ui.util;

import com.safwat.hr.controller.CentralController;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.web.WebView;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class PDFView {
    public static WebView createViewer(String pdfPath) {
        WebView webView = new WebView();

        try {
            // المسارات المحتملة لـ viewer.html
            String[] possiblePaths = {
                    "subresource/pdfjs/web/viewer.html",
                    "app/pdfjs/web/viewer.html",
                    "pdfjs/web/viewer.html",
                    "src/main/resources/pdfjs/web/viewer.html"
            };

            File viewerFile = null;
            for (String path : possiblePaths) {
                viewerFile = new File(path);
                if (viewerFile.exists()) {
                    break;
                }
            }

            if (!viewerFile.exists()) {
                throw new RuntimeException("viewer.html not found in known locations");
            }

            // تأكد من وجود ملف PDF
            File pdfFile = new File(pdfPath);
            if (!pdfFile.exists()) {
                throw new RuntimeException("PDF file not found: " + pdfPath);
            }

            // استخدم URI للتأكد من الترميز الصحيح
            String pdfUrl = pdfFile.toURI().toURL().toString();
            String viewerUrl = viewerFile.toURI().toURL().toString();

            // URL النهائي للعرض
            String finalUrl = viewerUrl + "?file=" + URLEncoder.encode(pdfUrl, StandardCharsets.UTF_8);

            webView.getEngine().load(finalUrl);

        } catch (Exception e) {

            e.printStackTrace();

            // عرض رسالة خطأ في الـ WebView نفسه
            webView.getEngine().loadContent(
                    "<html><body style='text-align: center; padding: 50px;'>"
                            + "<h2>❌ خطأ في تحميل الملف</h2>"
                            + "<p>" + e.getMessage() + "</p>"
                            + "<p>Path: " + pdfPath + "</p>"
                            + "</body></html>"
            );
        }

        return webView;
    }

    public static void shoePdf(String path, String mainTabPane) {
        TabPane tabPane = CentralController.getInstance().getComponent(mainTabPane, TabPane.class);

        // التحقق مما إذا كان التاب موجود بالفعل
        for (Tab tab : tabPane.getTabs()) {
            if ("عارض الملف".equals(tab.getText())) {
                tab.setContent(createViewer(path));
                tabPane.getSelectionModel().select(tab);
                return;
            }
        }

        // إذا لم يكن موجودًا، إنشاء تاب جديد
        Tab pdfTab = new Tab("عارض الملف");
        pdfTab.setContent(createViewer(path));
        pdfTab.setClosable(true);

        tabPane.getTabs().add(pdfTab);
        tabPane.getSelectionModel().select(pdfTab);
    }


    public static void showIN(String pdfPath, WebView webView) {

        try {
            // المسارات المحتملة لـ viewer.html
            String[] possiblePaths = {
                    "subresource/pdfjs/web/viewer.html",
                    "app/pdfjs/web/viewer.html",
                    "pdfjs/web/viewer.html",
                    "src/main/resources/pdfjs/web/viewer.html"
            };

            File viewerFile = null;
            for (String path : possiblePaths) {
                viewerFile = new File(path);
                if (viewerFile.exists()) {
                    break;
                }
            }

            if (!viewerFile.exists()) {
                throw new RuntimeException("viewer.html not found in known locations");
            }

            // تأكد من وجود ملف PDF
            File pdfFile = new File(pdfPath);
            if (!pdfFile.exists()) {
                throw new RuntimeException("PDF file not found: " + pdfPath);
            }

            // استخدم URI للتأكد من الترميز الصحيح
            String pdfUrl = pdfFile.toString();
            String viewerUrl = viewerFile.toURI().toURL().toString();

            // URL النهائي للعرض
            String finalUrl = viewerUrl + "?file=" + pdfUrl;

            webView.getEngine().load(finalUrl);

        } catch (Exception e) {

            e.printStackTrace();

            // عرض رسالة خطأ في الـ WebView نفسه
            webView.getEngine().loadContent(
                    "<html><body style='text-align: center; padding: 50px;'>"
                            + "<h2>❌ خطأ في تحميل الملف</h2>"
                            + "<p>" + e.getMessage() + "</p>"
                            + "<p>Path: " + pdfPath + "</p>"
                            + "</body></html>"
            );
        }

    }
}
