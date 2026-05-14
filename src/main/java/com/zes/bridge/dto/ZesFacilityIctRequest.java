package com.zes.bridge.dto;

import jakarta.validation.constraints.NotBlank;

public class ZesFacilityIctRequest {

    @NotBlank(message = "company_code is required")
    private String companyCode;

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }
}
