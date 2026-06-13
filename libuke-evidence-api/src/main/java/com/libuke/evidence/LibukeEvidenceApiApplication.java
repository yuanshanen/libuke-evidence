package com.libuke.evidence;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@MapperScan("com.libuke.evidence.domain.mapper")
@SpringBootApplication
public class LibukeEvidenceApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibukeEvidenceApiApplication.class, args);
    }
}
