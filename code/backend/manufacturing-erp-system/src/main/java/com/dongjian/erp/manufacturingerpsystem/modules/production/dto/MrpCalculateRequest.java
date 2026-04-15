package com.dongjian.erp.manufacturingerpsystem.modules.production.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class MrpCalculateRequest {
    @NotEmpty(message = "计划ID不能为空")
    private List<Long> planIds;
    public List<Long> getPlanIds() { return planIds; }
    public void setPlanIds(List<Long> planIds) { this.planIds = planIds; }
}
