package com.dongjian.erp.manufacturingerpsystem.modules.production.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class MrpGenerateRequest {
    @NotBlank(message = "taskKey不能为空")
    private String taskKey;
    @NotEmpty(message = "选择项不能为空")
    private List<Long> selectedItems;
    public String getTaskKey() { return taskKey; }
    public void setTaskKey(String taskKey) { this.taskKey = taskKey; }
    public List<Long> getSelectedItems() { return selectedItems; }
    public void setSelectedItems(List<Long> selectedItems) { this.selectedItems = selectedItems; }
}
