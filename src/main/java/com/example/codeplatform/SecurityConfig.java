package com.example.codeplatform;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final List<String> allowedOrigins;

    public SecurityConfig(JwtUtil jwtUtil, @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        this.jwtUtil = jwtUtil;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // The API is stateless and token-authenticated, so there is no session cookie for a
            // CSRF token to protect.
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                // Boot forwards every error response through /error on a fresh dispatch where the
                // JWT filter does not run. Locking it down turns every 403/404 into a 401.
                .requestMatchers("/error").permitAll()

                // Anyone may browse problems; only admins may author them.
                .requestMatchers(HttpMethod.GET, "/api/problems", "/api/problems/*").permitAll()
                .requestMatchers("/api/problems/**").hasRole("ADMIN")
                .requestMatchers("/api/testcases/**").hasRole("ADMIN")
                .requestMatchers("/api/users/**").hasRole("ADMIN")

                // Running code costs a third-party API quota, so require a signed-in user.
                .requestMatchers("/api/execute/**").authenticated()
                .requestMatchers("/api/submissions/**").authenticated()

                .anyRequest().authenticated()
            )
            // Without this the default entry point answers 403 for everything, so the client cannot
            // tell "you need to sign in" from "you are signed in but not allowed".
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required"))
                .accessDeniedHandler((request, response, deniedException) ->
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Insufficient permissions"))
            )
            .addFilterBefore(new JwtAuthFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
