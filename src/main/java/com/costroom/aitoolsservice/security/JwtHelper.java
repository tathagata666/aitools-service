package com.costroom.aitoolsservice.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Extracts commonly-needed fields from the Cognito JWT.
 * Same Cognito pool as auth-service, org-service, toolservice.
 */
@Component
public class JwtHelper {

    /** Cognito sub — stable UUID that identifies the user in auth-service. */
    public String getSubject(Jwt jwt) {
        return jwt.getSubject();
    }

    public String getEmail(Jwt jwt) {
        return jwt.getClaimAsString("email");
    }

    public List<String> getGroups(Jwt jwt) {
        List<String> groups = jwt.getClaimAsStringList("cognito:groups");
        return groups != null ? groups : List.of();
    }

    public boolean hasRole(Jwt jwt, String role) {
        return getGroups(jwt).stream().anyMatch(g -> g.equalsIgnoreCase(role));
    }
}
