package com.libuke.evidence.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class MiniLoginResponse {

    private String openid;
    private Long userId;
    private Long communityId;
    private String communityName;
    private String authStatus;
    private List<BigDecimal> center;
    private List<List<BigDecimal>> boundary;
    private Integer mapZoom;
}
