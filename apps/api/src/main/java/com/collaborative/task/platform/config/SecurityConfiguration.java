package com.collaborative.task.platform.config;

import com.collaborative.task.platform.service.AuthenticationService;
import com.collaborative.task.platform.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Security configuration with JWT authentication and CORS.
 * Implements comprehensive security best practices.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfiguration.class);
    
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;
    
    public SecurityConfiguration(JwtService jwtService, AuthenticationService authenticationService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless API
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Stateless session management
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure authorization
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/health/**").permitAll()
                .requestMatchers("/api/v1/system/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                
                // WebAuthn endpoints (may need special handling)
                .requestMatchers("/api/v1/webauthn/**").permitAll()
                
                // API documentation
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // Add JWT filter
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            
            // Configure security headers
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.deny())
                .contentTypeOptions(contentTypeOptions -> {})
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                    .preload(true)
                )
            );
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow specific origins in development, restrict in production
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:3000",
            "http://localhost:3001",
            "https://*.taskmanager.com"
        ));
        
        configuration.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        configuration.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        configuration.setExposedHeaders(List.of(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));
        
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService, authenticationService);
    }
    
    /**
     * JWT Authentication Filter.
     * Validates JWT tokens and sets security context.
     */
    public static class JwtAuthenticationFilter extends OncePerRequestFilter {
        
        private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
        
        private final JwtService jwtService;
        private final AuthenticationService authenticationService;
        
        public JwtAuthenticationFilter(JwtService jwtService, AuthenticationService authenticationService) {
            this.jwtService = jwtService;
            this.authenticationService = authenticationService;
        }
        
        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            
            try {
                String authHeader = request.getHeader("Authorization");
                String token = jwtService.extractTokenFromHeader(authHeader);
                
                if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    var userOpt = authenticationService.validateToken(token);
                    
                    if (userOpt.isPresent()) {
                        var user = userOpt.get();
                        
                        // Create authorities from user roles
                        var authorities = user.getRoles().stream()
                                .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
                                .collect(Collectors.toList());
                        
                        // Create authentication token
                        var authToken = new PreAuthenticatedAuthenticationToken(
                                user.getEmail(),
                                null,
                                authorities
                        );
                        
                        // Set additional details
                        authToken.setDetails(user);
                        
                        // Set security context
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        
                        logger.debug("Authentication set for user: {}", user.getEmail());
                    }
                }
                
            } catch (Exception e) {
                logger.debug("JWT authentication failed: {}", e.getMessage());
                // Clear security context on authentication failure
                SecurityContextHolder.clearContext();
            }
            
            filterChain.doFilter(request, response);
        }
        
        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            String path = request.getRequestURI();
            
            // Skip filter for public endpoints
            return path.startsWith("/api/v1/auth/") ||
                   path.startsWith("/api/v1/health/") ||
                   path.startsWith("/api/v1/system/") ||
                   path.startsWith("/actuator/") ||
                   path.startsWith("/v3/api-docs/") ||
                   path.startsWith("/swagger-ui/");
        }
    }
}