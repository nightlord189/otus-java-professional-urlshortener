package org.aburavov.urlshortener.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final UserService userService;
    private final AdminProperties properties;

    public AdminBootstrap(UserRepository userRepository, UserService userService, AdminProperties properties) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            return;
        }
        userService.create(properties.email(), properties.password(), UserRole.ADMIN);
        log.info("Created admin user {}", properties.email());
    }
}
