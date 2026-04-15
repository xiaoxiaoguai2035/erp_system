package com.dongjian.erp.manufacturingerpsystem.modules.basic.dto;

import jakarta.validation.constraints.NotBlank;

public class CustomerSaveRequest {

    @NotBlank(message = "客户编码不能为空")
    private String code;

    @NotBlank(message = "客户名称不能为空")
    private String name;

    private String contact;
    private String phone;
    private String address;

    @NotBlank(message = "状态不能为空")
    private String status;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
