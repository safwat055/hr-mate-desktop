package com.safwat.hr.payroll.payrollManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.safwat.hr.network.ApiClient;
import com.safwat.hr.network.ApiEndpoints;
import lombok.SneakyThrows;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PayrollManagerApiService {

    private static PayrollManagerApiService instance;

    public static PayrollManagerApiService getInstance() {
        if (instance == null) {
            instance = new PayrollManagerApiService();
        }
        return instance;
    }

    @SneakyThrows
    public List<String> getAllMonthsForYearly() {
        return ApiClient.post(ApiEndpoints.PayrollYearly.PAY_MONTHS_List, null, new TypeReference<List<String>>() {
        }).getData();
    }

    @SneakyThrows
    public List<String> getAllMonthForReview() {
        return ApiClient.post(
                ApiEndpoints.PayrollReview.ALL_MONTHS_List,
                null,
                new TypeReference<List<String>>() {
                }
        ).getData();
    }

    @SneakyThrows
    public List<String> getAllMonthForChangeCard() {
        return ApiClient.post(
                ApiEndpoints.PayrollChange.ALL_MONTHS_List,
                null,
                new TypeReference<List<String>>() {
                }
        ).getData();
    }

    @SneakyThrows
    public Integer deleteFullMonthYearly(LocalDate payMonth) {
        Map<String, String> data = Map.of("payMonth", payMonth.toString());

        return ApiClient.delete(ApiEndpoints.PayrollYearly.DELETE_ONE_MONTH, data, Integer.class).getData();
    }

    @SneakyThrows
    public List<String> getAvailablePayGroupForMonth(LocalDate payMonth) {
        Map<String, String> data = new HashMap<>();
        data.put("payMonth", payMonth != null ? payMonth.toString() : null);
        return ApiClient.post(
                ApiEndpoints.PayrollYearly.PAY_GROUP_LIST_MONTH,
                data,
                new TypeReference<List<String>>() {
                }
        ).getData();
    }

    @SneakyThrows
    public Integer deleteTargetGroupByMonth(LocalDate payMonth, String payGroup) {
        Map<String, String> data = new HashMap<>();
        data.put("payMonth", payMonth != null ? payMonth.toString() : null);
        data.put("payGroup", payGroup);
        return ApiClient.delete(
                ApiEndpoints.PayrollYearly.DELETE_TARGET_PAY_GROUP,
                data,
                Integer.class
        ).getData();

    }
}
