package com.safwat.hr.controller.payroll;

import com.safwat.hr.notification.model.HRNotification;
import com.safwat.hr.notification.service.NotificationService;
import com.safwat.hr.service.payroll.PayrollVocabService;
import com.safwat.hr.service.payroll.dto.DTO;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFButton;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.controls.SAFTextField;
import com.safwat.hr.ui.icons.Icons;
import com.safwat.hr.ui.util.PDFView;
import com.safwat.hr.ui.util.SearchDialog;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.web.WebView;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class PayrollVocabController implements Initializable {


    private final PayrollVocabService vocabService = new PayrollVocabService();
    @FXML
    private Button btn_open;
    @FXML
    private Button btn_search;
    @FXML
    private Label lbl_path;
    @FXML
    private TextField txt_management;
    @FXML
    private TextField txt_month;
    @FXML
    private TextField txt_name;
    @FXML
    private TextField txt_nationalID;
    @FXML
    private TextField txt_payID;
    @FXML
    private TextField txt_search;
    @FXML
    private WebView webView;
    @FXML
    private CheckBox chk_open;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setUi();
        setButtonActions();
        setTextFieldsAction();
    }

    void setUi() {
        SAFButton.flat(false, btn_search);
        SAFTextField.apply(txt_search, txt_nationalID, txt_payID, txt_name, txt_management);
        Icons.getInstance().getPDFImage(btn_open);

    }

    void setButtonActions() {
        btn_search.setOnAction(_ -> searchEmp());
        btn_open.setOnAction(_ -> exportToPDF());
    }

    void setTextFieldsAction() {
        txt_search.setOnAction(_ -> searchEmp());
        txt_nationalID.textProperty().addListener((observable, oldValue, newValue) -> lbl_path.setText(""));

    }

    void searchEmp() {
        if (txt_search.getText().isEmpty()) {
            SAFNotification.error("ادخل قيمة في حقل البحث اولا !");
            return;
        }
        if (txt_month.getText().isEmpty() || txt_month.getText().length() < 6) {
            SAFNotification.error("ادخل الشهر المطلوب اولا  (شهر سنة 2025 6)");
            return;
        }
        PayrollRequest request = new PayrollRequest();
        request.setSearchValue(txt_search.getText());
        request.setStartDate(DateUtils.getFirstDayOfMonth(txt_month.getText()));

        List<DTO.searchVocab> data = vocabService.searchVocab(request).getData();
        System.out.println(data.size());
        if (data.size() == 1) {
            txt_nationalID.setText(data.getFirst().nationalID());
            txt_payID.setText(data.getFirst().payID());
            txt_name.setText(data.getFirst().name());
            txt_management.setText(data.getFirst().management());
        } else if (data.size() > 1) {
            List<Object[]> subData =
                    data.stream().map(e -> new Object[]{e.nationalID(), e.payID(), e.name(), e.management()}).toList();
            Optional<Object[]> result = SearchDialog.builder()
                    .searchPlaceholder("بحث")
                    .headers(new String[]{"م قومي", "رقم صرف", "اسم", "إدارة"})
                    .data(subData).title("فرز")
                    .show();

            result.ifPresent(row -> {
                txt_nationalID.setText(row[0].toString());
                txt_payID.setText(row[1].toString());
                txt_name.setText(row[2].toString());
                txt_management.setText(row[3].toString());
            });
        }
    }

    private void exportToPDF() {

        try {
            if (txt_nationalID.getText().isEmpty() || txt_nationalID.getText().length() != 14) {
                SAFNotification.warning("يجب إدخال الرقم القومى او البحث عن قيمة أولا");
                return;
            }
            if (!lbl_path.getText().isEmpty()) {
                openPDF(Path.of(lbl_path.getText()));
                return;

            }
            PayrollRequest request = new PayrollRequest();

            request.setNationalId(txt_nationalID.getText());
            request.setStartDate(DateUtils.getFirstDayOfMonth(txt_month.getText()));

            request.setFormat("PDF");
            String fileName = "REVIEW_REPORT" + System.currentTimeMillis() + ".pdf";

            String workingDir = System.getProperty("user.dir");

            Path tempDownloadsDir = Paths.get(workingDir, "temp_downloads");
            if (!Files.exists(tempDownloadsDir)) {
                Files.createDirectories(tempDownloadsDir);
            }

            Path targetPath = tempDownloadsDir.resolve(fileName);

            SAFNotification.success("جاري تحميل الملف...");

            boolean success = vocabService.downloadVocab(request, targetPath);

            if (success) {
                openPDF(targetPath);
            } else {
                SAFNotification.error("فشل تحميل الملف");
            }

        } catch (Exception e) {
            SAFNotification.error("حدث خطأ أثناء التحميل: " + e.getMessage());

        }
    }

    private void openPDF(Path targetPath) {

        PDFView.showIN(targetPath.toString(), webView);
        NotificationService.getInstance().send(
                HRNotification.builder()
                        .type(HRNotification.NotificationType.SYSTEM)
                        .priority(HRNotification.Priority.HIGH)
                        .title("تقرير مراجعه")
                        .message(txt_name.getText())
                        .file(targetPath.toString())
                        .sender("system")
                        .build()
        );
    }

}
