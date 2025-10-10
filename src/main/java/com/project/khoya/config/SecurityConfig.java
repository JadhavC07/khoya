package com.project.khoya.config;

import com.project.khoya.security.JwtAuthenticationFilter;
import com.project.khoya.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public auth endpoints (login, register, refresh token)
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh").permitAll()

                        // Swagger UI endpoints
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-ui/index.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // Health check
                        .requestMatchers("/actuator/health").permitAll()

                        .requestMatchers("/uploads/**").permitAll()

                        // Public read access to alerts and search
                        .requestMatchers(HttpMethod.GET, "/api/alerts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/search/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/alerts/search/similar/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/search/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/alerts-social/**").permitAll()

                        // Public GET endpoints for vote counts and comment stats
                        .requestMatchers(HttpMethod.GET, "/api/social/alerts/*/votes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/social/comments/*/votes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/social/comments/*/stats").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/social/alerts/*/comments").permitAll()

                        .requestMatchers("/debug/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/social-api/webhook").permitAll()

                        // Logout endpoints require authentication
                        .requestMatchers("/api/auth/logout", "/api/auth/logout-all").authenticated()

                        // Admin only endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Protected endpoints - any authenticated user
                        .requestMatchers(HttpMethod.POST, "/api/alerts/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/alerts/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/alerts/**").authenticated()
                        .requestMatchers("/api/reports/**").authenticated()
                        .requestMatchers("/api/social/**").authenticated()

                        // Any other request must be authenticated
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}