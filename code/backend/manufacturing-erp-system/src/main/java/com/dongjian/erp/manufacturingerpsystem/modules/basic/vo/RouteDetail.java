package com.dongjian.erp.manufacturingerpsystem.modules.basic.vo;

import java.math.BigDecimal;
import java.util.List;

public class RouteDetail {

    private Long id;
    private Long productId;
    private String productCode;
    private String productName;
    private String versionNo;
    private String status;
    private List<RouteDetailItem> items;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
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

    public List<RouteDetailItem> getItems() {
        return items;
    }

    public void setItems(List<RouteDetailItem> items) {
        this.items = items;
    }

    public static class RouteDetailItem {

        private Long id;
        private String processCode;
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
