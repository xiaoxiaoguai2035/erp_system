package com.dongjian.erp.manufacturingerpsystem.modules.ai.service;

import com.dongjian.erp.manufacturingerpsystem.modules.ai.vo.AiReportInsightResponse;

import java.time.LocalDate;

public interface AiAssistantService {

    String chat(String question, LocalDate startDate, LocalDate endDate);

    AiReportInsightResponse generateReportInsight(LocalDate startDate, LocalDate endDate, String question);
}
