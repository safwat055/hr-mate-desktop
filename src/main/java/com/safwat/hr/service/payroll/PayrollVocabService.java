package com.safwat.hr.service.payroll;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.service.payroll.dto.DTO;
import com.safwat.hr.service.payroll.dto.PayrollRequest;
import com.safwat.hr.ui.controls.SAFNotification;
import com.safwat.hr.utils.ApiClient;
import com.safwat.hr.utils.ApiEndpoints;
import com.safwat.hr.utils.ApiResponse;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class PayrollVocabService {
    public ApiResponse<List<DTO.searchVocab>> searchVocab(PayrollRequest request) {
        try {
            return ApiClient.post(
                    ApiEndpoints.PayrollHistory.SEARCH,
                    request,
                    new TypeReference<List<DTO.searchVocab>>() {
                    }
            );
        } catch (IOException | InterruptedException e) {
            SAFNotification.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public boolean downloadVocab(PayrollRequest request, Path filePath) {
        try {
            return ApiClient.downloadFileViaPostWithBody(
                    ApiEndpoints.PayrollHistory.downloadReview,
                    request,
                    filePath

            );
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
