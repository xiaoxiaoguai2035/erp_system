package com.dongjian.erp.manufacturingerpsystem.modules.report.controller;

import com.dongjian.erp.manufacturingerpsystem.common.result.ApiResponse;
import com.dongjian.erp.manufacturingerpsystem.modules.report.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/sales-summary")
    public ApiResponse<Map<String, Object>> salesSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long customerId) {
        return ApiResponse.success(reportService.salesSummary(startDate, endDate, customerId));
    }

    @GetMapping("/purchase-summary")
    public ApiResponse<Map<String, Object>> purchaseSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long supplierId) {
        return ApiResponse.success(reportService.purchaseSummary(startDate, endDate, supplierId));
    }

    @GetMapping("/inventory-summary")
    public ApiResponse<Map<String, Object>> inventorySummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long warehouseId) {
        return ApiResponse.success(reportService.inventorySummary(startDate, endDate, warehouseId));
    }

    @GetMapping("/production-summary")
    public ApiResponse<Map<String, Object>> productionSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.success(reportService.productionSummary(startDate, endDate));
    }

    @GetMapping("/ar-summary")
    public ApiResponse<List<Map<String, Object>>> arSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Long customerId) {
        return ApiResponse.success(reportService.arSummary(startDate, endDate, limit, customerId));
    }

    @GetMapping("/ap-summary")
    public ApiResponse<List<Map<String, Object>>> apSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Long supplierId) {
        return ApiResponse.success(reportService.apSummary(startDate, endDate, limit, supplierId));
    }
}
