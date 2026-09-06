package com.danasea.backend.authentication.infrastructure.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.danasea.backend.authentication.application.port.PasswordHasher;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityBeans {
    
    @Bean
    PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    PasswordHasher passwordHasher(PasswordEncoder encoder){
        return new BCryptPasswordHasher(encoder);
    }
}
