package com.libuke.evidence.integration.oss;

import java.math.BigDecimal;

public record WatermarkRenderOptions(
    String position,
    BigDecimal opacity,
    BigDecimal backgroundOpacity,
    Integer fontSize,
    String textColor,
    String backgroundColor
) {
}
