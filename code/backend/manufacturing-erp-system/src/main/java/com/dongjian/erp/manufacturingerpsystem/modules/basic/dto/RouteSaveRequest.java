package com.dongjian.erp.manufacturingerpsystem.modules.basic.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class RouteSaveRequest {

    @NotNull(message = "产品物料不能为空")
    private Long productId;

    private String versionNo;
    private String status;

    @Valid
    @NotEmpty(message = "工艺路线明细不能为空")
    private List<RouteItemRequest> items;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(String versionNo) {
        this.versionNo = versionNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<RouteItemRequest> getItems() {
        return items;
    }

    public void setItems(List<RouteItemRequest> items) {
        this.items = items;
    }

    public static class RouteItemRequest {

        private Long id;

        @NotBlank(message = "工序编号不能为空")
        private String processCode;

        @NotBlank(message = "工序名称不能为空")
        private String processName;

        private BigDecimal standardHours;
        private String workCenter;
        private Integer sortNo;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getProcessCode() {
            return processCode;
        }

        public void setProcessCode(String processCode) {
            this.processCode = processCode;
        }

        public String getProcessName() {
            return processName;
        }

        public void setProcessName(String processName) {
            this.processName = processName;
        }

        public BigDecimal getStandardHours() {
            return standardHours;
        }

        public void setStandardHours(BigDecimal standardHours) {
            this.standardHours = standardHours;
        }

        public String getWorkCenter() {
            return workCenter;
        }

        public void setWorkCenter(String workCenter) {
            this.workCenter = workCenter;
        }

        public Integer getSortNo() {
            return sortNo;
        }

        public void setSortNo(Integer sortNo) {
            this.sortNo = sortNo;
        }
    }
}
