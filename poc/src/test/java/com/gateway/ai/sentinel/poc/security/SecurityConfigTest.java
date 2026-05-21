package com.gateway.ai.sentinel.poc.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.ai.sentinel.poc.ai.controller.SentinelController;
import com.gateway.ai.sentinel.poc.ai.model.RiskAssessmentRequest;
import com.gateway.ai.sentinel.poc.ai.model.RiskAssessmentResponse;
import com.gateway.ai.sentinel.poc.ai.service.RiskAnalysisService;
import com.gateway.ai.sentinel.poc.audit.producer.AuditEventProducer;
import com.gateway.ai.sentinel.poc.ratelimit.RateLimitService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link SecurityConfig} slice testleri.
 *
 * <p>
 * {@link WebMvcTest} ile sadece MVC katmanı yüklenir.
 * JWT doğrulaması
 * {@link org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors#jwt()}
 * ile simüle edilir.
 */
@WebMvcTest(SentinelController.class)
@Import({SecurityConfig.class, SecurityConfigTest.TestConfig.class})
@DisplayName("Security Configuration Tests")
class SecurityConfigTest {

	@TestConfiguration
	static class TestConfig {
		@Bean
		public ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@org.springframework.web.bind.annotation.RestController
		static class MockActuatorController {
			@org.springframework.web.bind.annotation.GetMapping("/actuator/health")
			public String health() {
				return "UP";
			}
		}
	}

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private RiskAnalysisService riskAnalysisService;

        @MockitoBean
        private RateLimitService rateLimitService;

        @MockitoBean
        private AuditEventProducer auditEventProducer;

        @MockitoBean
        private KeycloakJwtConverter keycloakJwtConverter;

        // ===== 401 Unauthorized Testleri =====

        @Test
        @DisplayName("Token olmadan istek 401 döndürmeli")
        void shouldReturn401WhenNoToken() throws Exception {
                mockMvc.perform(post("/api/v1/sentinel/analyze")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper
                                                .writeValueAsString(new RiskAssessmentRequest("test payload content"))))
                                .andExpect(status().isUnauthorized());
        }

        // ===== 403 Forbidden Testleri =====

        @Test
        @DisplayName("ROLE_AUDITOR ile /analyze endpoint'i 403 döndürmeli")
        void shouldReturn403WhenAuditorAccessesAnalyze() throws Exception {
                mockMvc.perform(post("/api/v1/sentinel/analyze")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper
                                                .writeValueAsString(new RiskAssessmentRequest("test payload content"))))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ROLE_USER ile /admin/health endpoint'i 403 döndürmeli")
        void shouldReturn403WhenUserAccessesAdminHealth() throws Exception {
                mockMvc.perform(get("/api/v1/sentinel/admin/health")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                                .andExpect(status().isForbidden());
        }

        // ===== 200 OK Testleri =====

        @Test
        @DisplayName("ROLE_USER ile /analyze endpoint'i 200 döndürmeli")
        void shouldReturn200WhenUserAnalyzes() throws Exception {
                // Arrange
                RiskAssessmentResponse mockResponse = RiskAssessmentResponse.builder()
                                .requestId("test-req-123")
                                .riskScore(3)
                                .category("LOW")
                                .reason("Minor anomaly")
                                .timestamp(Instant.now())
                                .build();

                when(riskAnalysisService.analyze(any())).thenReturn(mockResponse);
                doNothing().when(rateLimitService).checkRateLimit(any());
                doNothing().when(auditEventProducer).publishAuditEvent(any());

                // Act & Assert
                mockMvc.perform(post("/api/v1/sentinel/analyze")
                                .with(jwt()
                                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                                .jwt(jwtBuilder -> jwtBuilder.subject("user-sub-123")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new RiskAssessmentRequest("Suspicious transaction payload"))))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ROLE_ADMIN ile /admin/health endpoint'i 200 döndürmeli")
        void shouldReturn200WhenAdminAccessesAdminHealth() throws Exception {
                mockMvc.perform(get("/api/v1/sentinel/admin/health")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ROLE_ADMIN ile /analyze endpoint'i 200 döndürmeli")
        void shouldReturn200WhenAdminAnalyzes() throws Exception {
                // Arrange
                RiskAssessmentResponse mockResponse = RiskAssessmentResponse.builder()
                                .requestId("admin-req-456")
                                .riskScore(8)
                                .category("FRAUD")
                                .reason("High-risk transaction pattern")
                                .timestamp(Instant.now())
                                .build();

                when(riskAnalysisService.analyze(any())).thenReturn(mockResponse);
                doNothing().when(rateLimitService).checkRateLimit(any());
                doNothing().when(auditEventProducer).publishAuditEvent(any());

                // Act & Assert
                mockMvc.perform(post("/api/v1/sentinel/analyze")
                                .with(jwt()
                                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                                .jwt(jwtBuilder -> jwtBuilder.subject("admin-sub-456")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                                new RiskAssessmentRequest("Large transaction from unusual location"))))
                                .andExpect(status().isOk());
        }

        // ===== Validasyon Testleri =====

        @Test
        @DisplayName("Boş payload için 400 döndürmeli")
        void shouldReturn400WhenPayloadIsBlank() throws Exception {
                mockMvc.perform(post("/api/v1/sentinel/analyze")
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(new RiskAssessmentRequest(""))))
                                .andExpect(status().isBadRequest());
        }

        // ===== Actuator Testleri =====

        @Test
        @DisplayName("/actuator/health token olmadan erişilebilir olmalı")
        void shouldAllowActuatorHealthWithoutToken() throws Exception {
                mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        }
}
