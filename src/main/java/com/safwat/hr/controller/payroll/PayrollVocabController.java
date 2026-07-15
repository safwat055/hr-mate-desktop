package com.safwat.hr.controller.payroll;

import com.safwat.hr.service.payroll.PayrollVocabService;
import com.safwat.hr.service.payroll.dto.DTO;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.shared.util.DateUtils;
import com.safwat.hr.ui.controls.SAFButton;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.ui.util.PDFView;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.web.WebView;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

public class PayrollVocabController implements Initializable {


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
    private PayrollVocabService vocabService = new PayrollVocabService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setUi();
        setButtonActions();
    }

    void setUi() {
        SAFButton.flat(false, btn_open, btn_search);
        //SAFTextField.apply(txt_search, txt_nationalID, txt_payID, txt_name, txt_management);
    }

    void setButtonActions() {
        btn_search.setOnAction(_ -> searchEmp());
        btn_open.setOnAction(_ -> exportToPDF());
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
        }
    }

    private void exportToPDF() {

        try {
            if (txt_nationalID.getText().isEmpty() || txt_nationalID.getText().length() != 14) {
                SAFNotification.warning("يجب ادخال الرقم القومى او البحث عن قيمة اولا");
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
                File downloadedFile = targetPath.toFile();
                //SAFNotification.withAction("✅ تم تحميل الملف بنجاح", downloadedFile);
                PDFView.showIN(targetPath.toString(), webView);
            } else {
                SAFNotification.error("فشل تحميل الملف");
            }

        } catch (Exception e) {
            SAFNotification.error("حدث خطأ أثناء التحميل: " + e.getMessage());

        }
    }

}
