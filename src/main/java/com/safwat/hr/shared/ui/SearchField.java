package com.safwat.hr.shared.ui;

import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * =====================================================
 * SearchField — حقل نصي مع زر بحث يفتح SearchDialog
 * =====================================================
 * <p>
 * يحل محل الكومبوبوكس: الكتابة مباشرة مسموحة (لإدخال اسم جديد للحفظ)،
 * وضغط Enter أو زر 🔍 يفتح واجهة البحث القادمة من السيرفر
 * ويختار منها، وبعد الاختيار يُنفَّذ onSelect.
 * </p>
 * <p>
 * الاستخدام في الـ FXML:
 * <pre>{@code
 * <?import com.safwat.hr.shared.ui.SearchField?>
 * <SearchField fx:id="fieldPayrollGroup"
 *              promptText="اكتب اسم أو اضغط Enter للبحث"
 *              dialogTitle="اختر مجموعة التعيين"
 *              prefWidth="240"/>
 * }</pre>
 * <p>
 * الربط في الكونترولر:
 * <pre>{@code
 * fieldPayrollGroup.setFetcher(api::getPayrollGroupNames);
 * fieldPayrollGroup.setOnSelect(name -> loadHeaders(name));
 * }</pre>
 */
public class SearchField extends HBox {

    private final TextField textField = new TextField();
    private final Button searchButton = new Button("🔍");

    private Supplier<List<String>> fetcher;
    private Consumer<String> onSelect;
    private String dialogTitle = "بحث";
    private String dialogPlaceholder = "اكتب للتصفية...";

    public SearchField() {
        setSpacing(6);
        setAlignment(Pos.CENTER_LEFT);
        setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        textField.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        textField.setPrefHeight(32);
        searchButton.setPrefHeight(32);
        searchButton.setPrefWidth(38);
        HBox.setHgrow(textField, Priority.ALWAYS);

        searchButton.setOnAction(e -> openSearch());
        textField.setOnAction(e -> openSearch()); // Enter يفتح البحث

        getChildren().addAll(textField, searchButton);
    }

    // ===================== فتح واجهة البحث =====================
    private void openSearch() {
        if (fetcher == null) return;

        List<String> items;
        try {
            items = fetcher.get();
        } catch (Exception ex) {
            items = List.of();
        }

        Optional<String> result = SearchDialog.forStrings()
                .title(dialogTitle)
                .data(items == null ? List.of() : items)
                .searchPlaceholder(dialogPlaceholder)
                .owner(getScene() != null ? (javafx.stage.Stage) getScene().getWindow() : null)
                .show();

        result.ifPresent(value -> {
            textField.setText(value);
            if (onSelect != null) {
                onSelect.accept(value);
            }
        });
    }

    // ===================== خصائص FXML =====================

    public void setPromptText(String v) {
        textField.setPromptText(v);
    }

    public void setDialogTitle(String v) {
        this.dialogTitle = v;
    }

    public void setDialogPlaceholder(String v) {
        this.dialogPlaceholder = v;
    }

    // ===================== API للكونترولر =====================

    public void setFetcher(Supplier<List<String>> fetcher) {
        this.fetcher = fetcher;
    }

    public void setOnSelect(Consumer<String> onSelect) {
        this.onSelect = onSelect;
    }

    public String getText() {
        return textField.getText();
    }

    public void setText(String value) {
        textField.setText(value);
    }

    public void clear() {
        textField.clear();
    }

    public javafx.beans.property.StringProperty textProperty() {
        return textField.textProperty();
    }

    public TextField getTextField() {
        return textField;
    }
}