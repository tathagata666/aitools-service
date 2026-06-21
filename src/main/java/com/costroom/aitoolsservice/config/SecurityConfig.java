package com.costroom.aitoolsservice.config;

import com.costroom.aitoolsservice.security.CognitoGroupsGrantedAuthoritiesConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.HttpStatusAccessDeniedHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final CognitoGroupsGrantedAuthoritiesConverter authoritiesConverter;

    public SecurityConfig(CognitoGroupsGrantedAuthoritiesConverter authoritiesConverter) {
        this.authoritiesConverter = authoritiesConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/actuator/health", "/health").permitAll()
                .requestMatchers("/api/admin/**").hasRole("PLATFORM_ADMIN")
                // AI tool CRUD: any authenticated customer role
                .requestMatchers("/api/ai-tools/**").hasAnyRole(
                    "CUSTOMER_ADMIN", "CUSTOMER_REQUESTOR", "CUSTOMER_APPROVER")
                // Snapshot read: accessible to analytics service (internal) or customer
                .requestMatchers(HttpMethod.GET, "/api/snapshots/**").hasAnyRole(
                    "CUSTOMER_ADMIN", "CUSTOMER_REQUESTOR", "CUSTOMER_APPROVER")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    String authHeader = request.getHeader("Authorization");
                    boolean bearerPresent = authHeader != null && authHeader.startsWith("Bearer ");
                    log.warn("401 Unauthorized: method={} path={} bearerPresent={} reason={}",
                        request.getMethod(), request.getRequestURI(), bearerPresent,
                        authException.getMessage());
                    new BearerTokenAuthenticationEntryPoint()
                        .commence(request, response, authException);
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    log.warn("403 Forbidden: method={} path={} reason={}",
                        request.getMethod(), request.getRequestURI(),
                        accessDeniedException.getMessage());
                    new HttpStatusAccessDeniedHandler(org.springframework.http.HttpStatus.FORBIDDEN)
                        .handle(request, response, accessDeniedException);
                })
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "https://release-aws-amplify.d1fcmvv9d39ret.amplifyapp.com"
        ));
        configuration.setAllowedMethods(
            Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
