package com.gateway.ai.sentinel.poc.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

/**
 * Keycloak JWT token'larından realm rollerini çıkaran dönüştürücü.
 *
 * <p>Keycloak, rolleri standart JWT claim'i olarak değil, {@code realm_access.roles}
 * alanı içinde depolar. Bu converter, bu rolleri Spring Security
 * {@link GrantedAuthority} nesnelerine dönüştürür.
 *
 * <p>Örnek Keycloak token payload:
 * <pre>
 * {
 *   "realm_access": {
 *     "roles": ["user", "admin", "auditor"]
 *   }
 * }
 * </pre>
 */
@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> standardAuthorities = defaultConverter.convert(jwt);
        Collection<GrantedAuthority> realmRoles = extractRealmRoles(jwt);

        Collection<GrantedAuthority> allAuthorities = Stream.concat(
                        standardAuthorities != null ? standardAuthorities.stream() : Stream.empty(),
                        realmRoles.stream())
                .collect(Collectors.toUnmodifiableList());

        return new JwtAuthenticationToken(jwt, allAuthorities, jwt.getSubject());
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (realmAccess == null) {
            return Collections.emptyList();
        }

        List<String> roles = (List<String>) realmAccess.get(ROLES_CLAIM);
        if (roles == null) {
            return Collections.emptyList();
        }

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
                .collect(Collectors.toUnmodifiableList());
    }
}
