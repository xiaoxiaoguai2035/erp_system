package com.dongjian.erp.manufacturingerpsystem.modules.ai.controller;

import com.dongjian.erp.manufacturingerpsystem.common.result.ApiResponse;
import com.dongjian.erp.manufacturingerpsystem.modules.ai.dto.AiChatRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.ai.dto.AiReportInsightRequest;
import com.dongjian.erp.manufacturingerpsystem.modules.ai.service.AiAssistantService;
import com.dongjian.erp.manufacturingerpsystem.modules.ai.vo.AiReportInsightResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiAssistantService aiAssistantService;

    public AiController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/chat")
    public ApiResponse<Map<String, String>> chat(@Valid @RequestBody AiChatRequest request) {
        String answer = aiAssistantService.chat(request.getQuestion(), request.getStartDate(), request.getEndDate());
        return ApiResponse.success(Map.of("question", request.getQuestion(), "answer", answer));
    }

    @PostMapping("/report-insight")
    public ApiResponse<AiReportInsightResponse> reportInsight(@RequestBody(required = false) AiReportInsightRequest request) {
        AiReportInsightRequest safeRequest = request == null ? new AiReportInsightRequest() : request;
        return ApiResponse.success(
                aiAssistantService.generateReportInsight(
                        safeRequest.getStartDate(),
                        safeRequest.getEndDate(),
                        safeRequest.getQuestion()
                )
        );
    }
}
