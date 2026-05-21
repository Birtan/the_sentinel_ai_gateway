package com.gateway.ai.sentinel.poc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.ai.sentinel.poc.ai.model.RiskAssessmentRequest;
import com.gateway.ai.sentinel.poc.ai.model.RiskAssessmentResponse;
import com.gateway.ai.sentinel.poc.ai.service.RiskAnalysisService;
import com.gateway.ai.sentinel.poc.config.TestContainerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Sentinel AI Gateway entegrasyon testleri.
 *
 * <p>Gerçek Redis ve Kafka container'ları ({@link TestContainerConfig}) ile tam Spring context
 * yüklenir. Spring AI ({@link RiskAnalysisService}) mock'lanarak Ollama bağımlılığı kaldırılır.
 *
 * <p>Test profili: {@code test} — rate limit gevşetilmiş, debug log aktif.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
@ActiveProfiles("test")
@DisplayName("Sentinel Controller Integration Tests")
@Disabled("Docker daemon is not available in the build/test environment")
class SentinelControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RiskAnalysisService riskAnalysisService;

    // ===== Happy Path =====

    @Test
    @DisplayName("ROLE_USER ile tam akış başarıyla tamamlanmalı (Rate Limit → AI → Kafka)")
    void shouldCompleteFullFlowSuccessfully() throws Exception {
        // Arrange
        RiskAssessmentResponse mockResponse = RiskAssessmentResponse.builder()
                .requestId("integration-req-001")
                .riskScore(7)
                .category("FRAUD")
                .reason("Suspicious transaction from blacklisted IP")
                .timestamp(Instant.now())
                .build();

        when(riskAnalysisService.analyze(any())).thenReturn(mockResponse);

        RiskAssessmentRequest request =
                new RiskAssessmentRequest("Transaction of 50000 USD from blacklisted IP 10.0.0.1");

        // Act & Assert
        mockMvc.perform(post("/api/v1/sentinel/analyze")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(b -> b.subject("integration-user-001")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("integration-req-001"))
                .andExpect(jsonPath("$.riskScore").value(7))
                .andExpect(jsonPath("$.category").value("FRAUD"))
                .andExpect(jsonPath("$.reason").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("BENIGN payload için düşük risk skoru dönmeli")
    void shouldReturnLowRiskForBenignPayload() throws Exception {
        // Arrange
        RiskAssessmentResponse mockResponse = RiskAssessmentResponse.builder()
                .requestId("integration-req-002")
                .riskScore(1)
                .category("BENIGN")
                .reason("Normal login from known device")
                .timestamp(Instant.now())
                .build();

        when(riskAnalysisService.analyze(any())).thenReturn(mockResponse);

        RiskAssessmentRequest request = new RiskAssessmentRequest("User logged in from registered device at 09:00 AM");

        // Act & Assert
        mockMvc.perform(post("/api/v1/sentinel/analyze")
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                                .jwt(b -> b.subject("integration-user-002")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore").value(1))
                .andExpect(jsonPath("$.category").value("BENIGN"));
    }

    // ===== Hata Durumları =====

    @Test
    @DisplayName("Token olmadan 401 dönmeli")
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/sentinel/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RiskAssessmentRequest("test payload content"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Çok kısa payload için 400 dönmeli")
    void shouldReturn400WhenPayloadTooShort() throws Exception {
        mockMvc.perform(post("/api/v1/sentinel/analyze")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RiskAssessmentRequest("short"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("ROLE_AUDITOR ile /analyze endpoint'i 403 dönmeli")
    void shouldReturn403WhenAuditorCallsAnalyze() throws Exception {
        mockMvc.perform(post("/api/v1/sentinel/analyze")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_AUDITOR")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RiskAssessmentRequest("Suspicious transaction payload"))))
                .andExpect(status().isForbidden());
    }
}
