package com.libuke.evidence.config;

import com.libuke.evidence.domain.service.RuntimeConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupWarmupRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final RuntimeConfigService runtimeConfigService;

    @Override
    public void run(ApplicationArguments args) {
        long startedAt = System.currentTimeMillis();
        Integer databaseAlive = jdbcTemplate.queryForObject("select 1", Integer.class);
        runtimeConfigService.basicConfigs();
        runtimeConfigService.mapConfig();
        runtimeConfigService.uploadPolicy("report_image");
        log.info("[startup-warmup] databaseAlive={}, costMs={}", databaseAlive, System.currentTimeMillis() - startedAt);
    }
}
