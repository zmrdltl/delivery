package com.sparta.delivery.global.config;

import com.sparta.delivery.global.security.JwtAuthenticationFilter;
import com.sparta.delivery.global.security.JwtUtil;
import com.sparta.delivery.global.security.SecurityErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtUtil jwtUtil,
            SecurityErrorHandler securityErrorHandler
    )
        throws Exception {

        http
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtUtil),
                UsernamePasswordAuthenticationFilter.class
            )
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(securityErrorHandler)
                .accessDeniedHandler(securityErrorHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/error").permitAll()
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/users",
                    "/api/users/login"
                ).permitAll()
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/stores",
                    "/api/menus"
                ).hasRole("OWNER")
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/menus",
                    "/api/menus/{menuId}"
                ).permitAll()
                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/menus/{menuId}"
                ).hasRole("OWNER")
                .requestMatchers(
                    HttpMethod.DELETE,
                    "/api/menus/{menuId}"
                ).hasRole("OWNER")
                .requestMatchers(
                    HttpMethod.PATCH,
                    "/api/orders/{orderId}/accept",
                    "/api/orders/{orderId}/deliver",
                    "/api/orders/{orderId}/reject"
                ).hasRole("OWNER")
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/orders"
                ).hasRole("CUSTOMER")
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/orders",
                    "/api/orders/{orderId}"
                ).hasAnyRole("CUSTOMER", "OWNER")
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/orders/{orderId}/payments"
                ).hasRole("CUSTOMER")
                .requestMatchers(
                    HttpMethod.PATCH,
                    "/api/orders/{orderId}/cancel"
                ).hasRole("CUSTOMER")
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/payments"
                ).hasRole("CUSTOMER")
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
