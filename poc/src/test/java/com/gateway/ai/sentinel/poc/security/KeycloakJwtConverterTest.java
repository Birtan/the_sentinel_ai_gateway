package com.gateway.ai.sentinel.poc.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayName("KeycloakJwtConverter Unit Tests")
class KeycloakJwtConverterTest {

    private final KeycloakJwtConverter converter = new KeycloakJwtConverter();

    @Test
    @DisplayName("realm_access claim'i yoksa boş rol listesi döndürmeli")
    void shouldReturnEmptyAuthoritiesWhenNoRealmAccess() {
        // Arrange
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("realm_access")).thenReturn(null);
        when(jwt.getSubject()).thenReturn("user-sub-123");

        // Act
        AbstractAuthenticationToken token = converter.convert(jwt);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("realm_access var ama roles claim'i yoksa boş rol listesi döndürmeli")
    void shouldReturnEmptyAuthoritiesWhenNoRolesInRealmAccess() {
        // Arrange
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("realm_access")).thenReturn(Map.of("other", "field"));
        when(jwt.getSubject()).thenReturn("user-sub-123");

        // Act
        AbstractAuthenticationToken token = converter.convert(jwt);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("roles claim'i varsa rolleri ROLE_ prefix'i ile büyük harfe çevirerek eklemeli")
    void shouldConvertRolesToGrantedAuthorities() {
        // Arrange
        Jwt jwt = mock(Jwt.class);
        Map<String, Object> realmAccess = Map.of("roles", List.of("user", "admin", "auditor"));
        when(jwt.getClaim("realm_access")).thenReturn(realmAccess);
        when(jwt.getSubject()).thenReturn("user-sub-123");

        // Act
        AbstractAuthenticationToken token = converter.convert(jwt);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN", "ROLE_AUDITOR");
    }
}
