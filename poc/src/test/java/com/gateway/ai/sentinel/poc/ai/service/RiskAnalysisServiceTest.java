package com.gateway.ai.sentinel.poc.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;

import com.gateway.ai.sentinel.poc.ai.model.RiskAssessmentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

/**
 * {@link RiskAnalysisService} unit testleri.
 *
 * <p>
 * Spring context yüklenmez; {@link ChatClient} Mockito ile mock'lanır.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskAnalysisService Unit Tests")
class RiskAnalysisServiceTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClientRequestSpec requestSpec;

    @Mock
    private CallResponseSpec callResponseSpec;

    private RiskAnalysisService riskAnalysisService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        riskAnalysisService = new RiskAnalysisService(chatClientBuilder);
    }

    @Test
    @DisplayName("SQL injection payload için yüksek risk skoru döndürmeli")
    void shouldReturnHighRiskForSqlInjection() {
        // Arrange
        String sqlInjectionPayload = "SELECT * FROM users WHERE id=1; DROP TABLE users;--";
        String llmJsonResponse = """
                {"riskScore": 10, "category": "INJECTION_ATTACK", "reason": "SQL injection detected"}
                """;

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(sqlInjectionPayload)).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(llmJsonResponse);

        // Act
        RiskAssessmentResponse response = riskAnalysisService.analyze(sqlInjectionPayload);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.requestId()).isNotBlank();
        assertThat(response.riskScore()).isEqualTo(10);
        assertThat(response.category()).isEqualTo("INJECTION_ATTACK");
        assertThat(response.reason()).contains("SQL injection");
        assertThat(response.timestamp()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("Normal aktivite için düşük risk skoru döndürmeli")
    void shouldReturnLowRiskForNormalActivity() {
        // Arrange
        String normalPayload = "User logged in from usual IP 192.168.1.1 at 09:00 AM";
        String llmJsonResponse = """
                {"riskScore": 1, "category": "BENIGN", "reason": "Normal login from known IP"}
                """;

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(normalPayload)).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(llmJsonResponse);

        // Act
        RiskAssessmentResponse response = riskAnalysisService.analyze(normalPayload);

        // Assert
        assertThat(response.riskScore()).isEqualTo(1);
        assertThat(response.category()).isEqualTo("BENIGN");
    }

    @Test
    @DisplayName("Her analiz için benzersiz requestId üretmeli")
    void shouldGenerateUniqueRequestIds() {
        // Arrange
        String payload = "Test payload for uniqueness check";
        String llmResponse = """
                {"riskScore": 3, "category": "LOW", "reason": "Minor anomaly detected"}
                """;

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(payload)).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(llmResponse);

        // Act
        RiskAssessmentResponse response1 = riskAnalysisService.analyze(payload);
        RiskAssessmentResponse response2 = riskAnalysisService.analyze(payload);

        // Assert
        assertThat(response1.requestId()).isNotEqualTo(response2.requestId());
    }

    @Test
    @DisplayName("ChatClient prompt() tam olarak bir kez çağrılmalı")
    void shouldCallChatClientExactlyOnce() {
        // Arrange
        String payload = "Suspicious user activity detected";
        String llmResponse = """
                {"riskScore": 7, "category": "FRAUD", "reason": "Suspicious pattern detected"}
                """;

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(payload)).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(llmResponse);

        // Act
        riskAnalysisService.analyze(payload);

        // Assert
        verify(chatClient, times(1)).prompt();
        verify(requestSpec, times(1)).system(any(String.class));
        verify(requestSpec, times(1)).user(payload);
        verify(requestSpec, times(1)).call();
    }
}
