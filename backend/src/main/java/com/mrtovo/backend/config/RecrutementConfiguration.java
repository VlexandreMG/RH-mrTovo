package com.mrtovo.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({RecrutementProperties.class, TempsTravailProperties.class})
public class RecrutementConfiguration {
}
