package com.spacemit.musebackendv2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors().and().csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/api/v2/**").permitAll() // V2所有接口允许访问
                .antMatchers("/websocket/**").permitAll() // WebSocket端点允许访问
                .antMatchers("/actuator/**").permitAll()
                .antMatchers("/", "/index.html", "/report.html", "/heatmap.html", "/monitor.html").permitAll() // 静态页面允许访问
                .antMatchers("/*.html", "/static/**", "/css/**", "/js/**", "/images/**").permitAll() // 静态资源允许访问
                .anyRequest().permitAll(); // 允许所有请求
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "https://topcroesus.site", "http://topcroesus.site",
                "https://web.topcroesus.site", "http://web.topcroesus.site",
                "https://spacemit.topcroesus.site", "http://spacemit.topcroesus.site",
                "http://localhost:5173", "http://localhost:5174", "http://localhost:5175",
                "http://localhost:3000", "http://localhost:8080",
                "http://127.0.0.1:5173", "http://127.0.0.1:5174", "http://127.0.0.1:5175",
                "http://127.0.0.1:3000", "http://127.0.0.1:8080"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

