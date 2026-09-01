package com.ino.admin.identity.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
class AdminBootstrapRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminBootstrapProperties properties;
    private final AdminBootstrapService service;

    AdminBootstrapRunner(AdminBootstrapProperties properties, AdminBootstrapService service) {
        this.properties = properties;
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) return;
        var result = service.bootstrap(properties.getEmail(), properties.getPassword(), properties.getDisplayName());
        if (result == AdminBootstrapService.Result.CREATED) {
            log.info("초기 관리자 계정을 생성했습니다.");
        } else {
            log.info("초기 관리자 계정이 이미 존재하여 생성을 건너뜁니다.");
        }
    }
}
