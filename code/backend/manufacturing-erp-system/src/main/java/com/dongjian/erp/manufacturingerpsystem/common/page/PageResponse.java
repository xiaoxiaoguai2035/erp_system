package com.dongjian.erp.manufacturingerpsystem.common.page;

import java.util.Collections;
import java.util.List;

public class PageResponse<T> {

    private long total;
    private long pageNo;
    private long pageSize;
    private List<T> records;

    public PageResponse() {
    }

    public PageResponse(long total, long pageNo, long pageSize, List<T> records) {
        this.total = total;
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.records = records;
    }

    public static <T> PageResponse<T> of(long total, long pageNo, long pageSize, List<T> records) {
        return new PageResponse<>(total, pageNo, pageSize, records);
    }

    public static <T> PageResponse<T> empty(long pageNo, long pageSize) {
        return new PageResponse<>(0, pageNo, pageSize, Collections.emptyList());
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPageNo() {
        return pageNo;
    }

    public void setPageNo(long pageNo) {
        this.pageNo = pageNo;
    }

    public long getPageSize() {
        return pageSize;
    }

    public void setPageSize(long pageSize) {
        this.pageSize = pageSize;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }
}
