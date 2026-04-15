package com.dongjian.erp.manufacturingerpsystem.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public class AiChatRequest {

    @NotBlank(message = "问题不能为空")
    private String question;
    private LocalDate startDate;
    private LocalDate endDate;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
