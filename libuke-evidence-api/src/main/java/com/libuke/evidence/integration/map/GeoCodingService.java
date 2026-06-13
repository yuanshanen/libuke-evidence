package com.libuke.evidence.integration.map;

import java.math.BigDecimal;

public interface GeoCodingService {

    String reverseGeocode(BigDecimal longitude, BigDecimal latitude);
}
