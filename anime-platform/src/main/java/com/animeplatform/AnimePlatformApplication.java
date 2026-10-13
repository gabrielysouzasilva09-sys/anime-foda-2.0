package com.animeplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class AnimePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnimePlatformApplication.class, args);
    }

    /**
     * RestTemplate usado pelo KitsuService para chamar a Kitsu API.
     * Registado como @Bean para ser injectado via @Autowired.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
