package com.libuke.evidence.integration.map;

import com.fasterxml.jackson.databind.JsonNode;
import com.libuke.evidence.config.AmapMapProperties;
import com.libuke.evidence.domain.service.RuntimeConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmapGeoCodingService implements GeoCodingService {

    private static final String DEFAULT_REVERSE_GEOCODER_URL = "https://restapi.amap.com/v3/geocode/regeo";

    private final AmapMapProperties amapMapProperties;
    private final RuntimeConfigService runtimeConfigService;

    @Override
    public String reverseGeocode(BigDecimal longitude, BigDecimal latitude) {
        RuntimeConfigService.MapRuntimeConfig mapConfig = runtimeConfigService.mapConfig();
        if (longitude == null || latitude == null || !StringUtils.hasText(mapConfig.reverseGeocodeKey())) {
            return null;
        }

        try {
            URI uri = buildUri(resolveEndpointUrl(), buildParams(longitude, latitude, mapConfig.reverseGeocodeKey()));
            JsonNode response = getJson(uri);
            return parseAddress(response);
        } catch (RuntimeException exception) {
            log.warn("Amap reverse geocode failed, longitude={}, latitude={}", longitude, latitude, exception);
            return null;
        }
    }

    private Map<String, String> buildParams(BigDecimal longitude, BigDecimal latitude, String key) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("key", key);
        params.put("location", longitude.toPlainString() + "," + latitude.toPlainString());
        params.put("extensions", "base");
        params.put("output", "JSON");
        return params;
    }

    private URI buildUri(String url, Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        params.forEach(builder::queryParam);
        return builder.build(true).toUri();
    }

    private String resolveEndpointUrl() {
        String url = StringUtils.hasText(amapMapProperties.getReverseGeocoderUrl())
            ? amapMapProperties.getReverseGeocoderUrl()
            : DEFAULT_REVERSE_GEOCODER_URL;
        int queryIndex = url.indexOf('?');
        return queryIndex >= 0 ? url.substring(0, queryIndex) : url;
    }

    private RestClient buildRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(resolveTimeout(amapMapProperties.getConnectTimeoutMillis(), 3000)));
        requestFactory.setReadTimeout(Duration.ofMillis(resolveTimeout(amapMapProperties.getReadTimeoutMillis(), 5000)));
        return RestClient.builder()
            .requestFactory(requestFactory)
            .build();
    }

    private JsonNode getJson(URI uri) {
        return buildRestClient()
            .get()
            .uri(uri)
            .retrieve()
            .body(JsonNode.class);
    }

    private long resolveTimeout(Integer value, long defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private String parseAddress(JsonNode response) {
        if (response == null || !"1".equals(response.path("status").asText())) {
            logAmapApiError(response);
            return null;
        }

        JsonNode regeocode = response.path("regeocode");
        JsonNode addressComponent = regeocode.path("addressComponent");
        String province = addressComponent.path("province").asText("");
        String formattedAddress = regeocode.path("formatted_address").asText("");
        if (StringUtils.hasText(province) && formattedAddress.startsWith(province)) {
            return formattedAddress.substring(province.length());
        }
        return StringUtils.hasText(formattedAddress) ? formattedAddress : null;
    }

    private void logAmapApiError(JsonNode response) {
        if (response == null) {
            log.warn("Amap reverse geocode failed: empty response");
            return;
        }
        log.warn(
            "Amap reverse geocode failed: status={}, infocode={}, info={}",
            response.path("status").asText(""),
            response.path("infocode").asText(""),
            response.path("info").asText("")
        );
    }
}
