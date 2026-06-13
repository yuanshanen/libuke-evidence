package com.libuke.evidence.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MiniLoginRequest {

    @NotBlank
    private String code;
}
