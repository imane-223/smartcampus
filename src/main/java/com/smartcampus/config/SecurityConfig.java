package com.smartcampus.config;

import java.io.IOException;
import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import com.smartcampus.common.ErrorResponse;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Sécurité de l'API (Spring Security 7) : session serveur + cookie, CSRF par cookie XSRF-TOKEN.
 * Publics : connexion, jeton CSRF et santé de l'actuator (healthcheck Docker). Tout le reste exige une session.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CsrfTokenRepository csrfTokenRepository,
            SecurityContextRepository securityContextRepository, ObjectMapper objectMapper) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                // spa() : gestionnaire adapté à un frontend JavaScript qui renvoie le cookie dans X-XSRF-TOKEN
                .csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokenRepository))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, ex) -> writeError(objectMapper, request, response,
                                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentification requise."))
                        .accessDeniedHandler((request, response, ex) -> {
                            if (ex instanceof CsrfException) {
                                writeError(objectMapper, request, response, HttpStatus.FORBIDDEN, "CSRF",
                                        "Jeton CSRF manquant ou invalide. Rechargez la page.");
                            } else {
                                writeError(objectMapper, request, response, HttpStatus.FORBIDDEN, "FORBIDDEN",
                                        "Vous n'avez pas les droits nécessaires.");
                            }
                        }));
        return http.build();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        // Lisible par le JavaScript (pas HttpOnly) : le frontend renvoie sa valeur dans l'en-tête X-XSRF-TOKEN
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    private static void writeError(ObjectMapper objectMapper, HttpServletRequest request,
            HttpServletResponse response, HttpStatus status, String error, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ErrorResponse.of(status.value(), error, message, request.getRequestURI()));
    }
}
