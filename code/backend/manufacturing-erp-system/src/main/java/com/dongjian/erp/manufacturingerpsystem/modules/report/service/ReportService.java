package com.dongjian.erp.manufacturingerpsystem.modules.report.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReportService {

    Map<String, Object> salesSummary(LocalDate startDate, LocalDate endDate);

    Map<String, Object> purchaseSummary(LocalDate startDate, LocalDate endDate);

    Map<String, Object> inventorySummary(LocalDate startDate, LocalDate endDate);

    Map<String, Object> productionSummary(LocalDate startDate, LocalDate endDate);

    List<Map<String, Object>> arSummary(LocalDate startDate, LocalDate endDate, Integer limit);

    List<Map<String, Object>> apSummary(LocalDate startDate, LocalDate endDate, Integer limit);
}
