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

                        .requestMatchers(HttpMethod.POST, "/users/register").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/users/create").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/users/classes").hasAnyRole("ADMIN", "CURATOR", "EXAMINER")
                        .requestMatchers(HttpMethod.GET, "/users/**").hasAnyRole("ADMIN", "CURATOR", "EXAMINER")

                        .requestMatchers(HttpMethod.POST, "/exam/session").hasRole("EXAMINER")
                        .requestMatchers(HttpMethod.POST, "/exam/session/create").hasRole("EXAMINER")
                        .requestMatchers(HttpMethod.POST, "/exam/session/start/**").hasRole("EXAMINER")
                        .requestMatchers(HttpMethod.POST, "/exam/session/end/**").hasRole("EXAMINER")
                        .requestMatchers(HttpMethod.POST, "/exam/session/*/grades").hasRole("EXAMINER")
                        .requestMatchers(HttpMethod.POST, "/exam/session/*/answers").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/exam/session/*/monitor").hasAnyRole("EXAMINER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/exam/session/students/*/results").hasAnyRole("STUDENT", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/exam/session/**").hasAnyRole("STUDENT", "EXAMINER", "ADMIN")

                        .requestMatchers(HttpMethod.POST, "/violations/report").hasAnyRole("EXAMINER", "CURATOR")
                        .requestMatchers(HttpMethod.GET, "/violations/**").hasAnyRole("EXAMINER", "CURATOR", "ADMIN")

                        .requestMatchers(HttpMethod.POST, "/vote/secret").hasRole("CURATOR")
                        .requestMatchers(HttpMethod.POST, "/vote/secret/*/finish").hasRole("CURATOR")
                        .requestMatchers(HttpMethod.POST, "/vote/secret/*/votes").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/vote/secret/**").hasAnyRole("STUDENT", "CURATOR", "ADMIN")

                        .requestMatchers("/student/**").hasAnyRole("STUDENT", "ADMIN")
                        .requestMatchers("/curator/**").hasAnyRole("CURATOR", "ADMIN")
                        .requestMatchers("/examiner/**").hasAnyRole("EXAMINER", "ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")

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
