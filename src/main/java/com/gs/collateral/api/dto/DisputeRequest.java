package com.gs.collateral.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class DisputeRequest {

    @NotBlank
    @Size(max = 500)
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
