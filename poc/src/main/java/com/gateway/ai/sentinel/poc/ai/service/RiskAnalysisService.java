package com.gateway.ai.sentinel.poc.ai.service;

import java.time.Instant;
import java.util.UUID;

import com.gateway.ai.sentinel.poc.ai.model.LlmRiskOutput;
import com.gateway.ai.sentinel.poc.ai.model.RiskAssessmentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

/**
 * Spring AI ve Ollama kullanarak metin tabanlı risk analizi yapan servis.
 *
 * <p>
 * Few-Shot Prompting yöntemi ile LLM'e kesin bir JSON şeması dayatılır.
 * {@link BeanOutputConverter} çıktıyı {@link LlmRiskOutput}'a dönüştürür.
 */
@Service
public class RiskAnalysisService {

    private static final String SYSTEM_PROMPT = """
            You are an expert security risk analysis AI for a financial gateway system.
            Your sole purpose is to analyze the provided text payload for potential security threats.

            Respond ONLY with a valid JSON object. Do not add any explanation, markdown, or preamble.

            The JSON must strictly follow this schema:
            %s

            Risk Score Guidelines:
            - 1-2: BENIGN (normal, expected activity)
            - 3-4: LOW (minor anomalies, likely benign)
            - 5-6: MEDIUM (suspicious patterns detected)
            - 7-8: HIGH (strong indicators of fraud/attack)
            - 9-10: CRITICAL (confirmed malicious activity)

            Categories: FRAUD, PHISHING, MALWARE, SPAM, INJECTION_ATTACK, DATA_EXFILTRATION, BENIGN

            Examples:
            Input: "User logged in from usual IP 192.168.1.1"
            Output: {"riskScore": 1, "category": "BENIGN", "reason": "Login from known IP with normal pattern"}

            Input: "SELECT * FROM users WHERE id=1; DROP TABLE users;--"
            Output: {"riskScore": 10, "category": "INJECTION_ATTACK", "reason": "SQL injection attempt detected with DROP TABLE command"}

            Now analyze the following payload:
            """;

    private static final Logger log = LoggerFactory.getLogger(RiskAnalysisService.class);

    private final ChatClient chatClient;

    public RiskAnalysisService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Verilen payload metnini analiz ederek yapılandırılmış bir risk
     * değerlendirmesi döner.
     *
     * @param payload Analiz edilecek ham metin
     * @return Risk skoru, kategori ve gerekçe içeren değerlendirme
     */
    public RiskAssessmentResponse analyze(String payload) {
        BeanOutputConverter<LlmRiskOutput> outputConverter = new BeanOutputConverter<>(LlmRiskOutput.class);

        String systemPromptWithSchema = SYSTEM_PROMPT.formatted(outputConverter.getFormat());

        log.debug("Starting risk analysis for payload of length: {}", payload.length());

        String llmResponse = chatClient
                .prompt()
                .system(systemPromptWithSchema)
                .user(payload)
                .call()
                .content();

        log.debug("LLM raw response: {}", llmResponse);

        LlmRiskOutput llmOutput = outputConverter.convert(llmResponse);

        return RiskAssessmentResponse.builder()
                .requestId(UUID.randomUUID().toString())
                .riskScore(llmOutput.riskScore())
                .category(llmOutput.category())
                .reason(llmOutput.reason())
                .timestamp(Instant.now())
                .build();
    }
}
