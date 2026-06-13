package com.libuke.evidence.api.admin.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminMapConfigRequest {
    private String reverseGeocodeKey;
    private String jsApiKey;
    private String jsApiSecurityKey;
    private Integer defaultZoom;
    private BigDecimal defaultLongitude;
    private BigDecimal defaultLatitude;
    private Boolean enabled;
    private String remark;
}
