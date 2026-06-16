package com.exam.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_CURATOR = "CURATOR";
    private static final String ROLE_EXAMINER = "EXAMINER";
    private static final String ROLE_STUDENT = "STUDENT";
    private static final String USERS_PATTERN = "/users/**";

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers("/ws/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/users/me/class").authenticated()
                        .requestMatchers(HttpMethod.GET, "/users/me").authenticated()
                        .requestMatchers(HttpMethod.POST, "/users/register").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT, USERS_PATTERN).hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, USERS_PATTERN).hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/users/classes").hasAnyRole(ROLE_ADMIN, ROLE_CURATOR, ROLE_EXAMINER)
                        .requestMatchers(HttpMethod.GET, USERS_PATTERN).hasAnyRole(ROLE_ADMIN, ROLE_CURATOR, ROLE_EXAMINER)

                        .requestMatchers(HttpMethod.POST, "/exam/session").hasRole(ROLE_EXAMINER)
                        .requestMatchers(HttpMethod.POST, "/exam/session/start/**").hasRole(ROLE_EXAMINER)
                        .requestMatchers(HttpMethod.POST, "/exam/session/end/**").hasRole(ROLE_EXAMINER)
                        .requestMatchers(HttpMethod.POST, "/exam/session/*/grades").hasRole(ROLE_EXAMINER)
                        .requestMatchers(HttpMethod.POST, "/exam/session/*/answers/me").hasRole(ROLE_STUDENT)
                        .requestMatchers(HttpMethod.POST, "/exam/session/*/attempt/me/submit").hasRole(ROLE_STUDENT)
                        .requestMatchers(HttpMethod.POST, "/exam/session/*/answers").hasAnyRole(ROLE_EXAMINER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/exam/session").hasAnyRole(ROLE_STUDENT, ROLE_EXAMINER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/exam/session/dashboard").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/exam/session/my").hasAnyRole(ROLE_STUDENT, ROLE_EXAMINER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/exam/session/*/monitor").hasAnyRole(ROLE_EXAMINER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/exam/session/*/questions").hasAnyRole(ROLE_EXAMINER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/exam/session/*/answers/**").hasAnyRole(ROLE_EXAMINER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/exam/session/*/results").hasAnyRole(ROLE_EXAMINER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/exam/session/students/*/results").hasAnyRole(ROLE_STUDENT, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/exam/session/**").hasAnyRole(ROLE_STUDENT, ROLE_EXAMINER, ROLE_ADMIN)

                        .requestMatchers(HttpMethod.POST, "/violations/report/me").hasRole(ROLE_STUDENT)
                        .requestMatchers(HttpMethod.POST, "/violations/report").hasAnyRole(ROLE_EXAMINER, ROLE_CURATOR)
                        .requestMatchers(HttpMethod.GET, "/violations/**").hasAnyRole(ROLE_EXAMINER, ROLE_CURATOR, ROLE_ADMIN)

                        .requestMatchers(HttpMethod.POST, "/vote/secret").hasRole(ROLE_CURATOR)
                        .requestMatchers(HttpMethod.POST, "/vote/secret/*/finish").hasRole(ROLE_CURATOR)
                        .requestMatchers(HttpMethod.POST, "/vote/secret/*/votes/me").hasRole(ROLE_STUDENT)
                        .requestMatchers(HttpMethod.POST, "/vote/secret/*/votes").hasAnyRole(ROLE_CURATOR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/vote/secret").hasAnyRole(ROLE_STUDENT, ROLE_CURATOR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/vote/secret/my").hasAnyRole(ROLE_STUDENT, ROLE_CURATOR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/vote/secret/*/results").hasAnyRole(ROLE_CURATOR, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/vote/secret/**").hasAnyRole(ROLE_STUDENT, ROLE_CURATOR, ROLE_ADMIN)

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:3000",
                "http://localhost:8080",
                "http://localhost:8081"
        ));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type"
        ));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
