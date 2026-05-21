package com.gateway.ai.sentinel.poc.ai.controller;

import jakarta.validation.Valid;
import java.util.Map;

import com.gateway.ai.sentinel.poc.ai.model.RiskAssessmentRequest;
import com.gateway.ai.sentinel.poc.ai.model.RiskAssessmentResponse;
import com.gateway.ai.sentinel.poc.audit.event.AuditEvent;
import com.gateway.ai.sentinel.poc.audit.producer.AuditEventProducer;
import com.gateway.ai.sentinel.poc.ai.service.RiskAnalysisService;
import com.gateway.ai.sentinel.poc.ratelimit.RateLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

/**
 * Sentinel AI Gateway ana REST controller'ı.
 *
 * <p>
 * Temel akış:
 * <ol>
 * <li>JWT token doğrulama (SecurityFilterChain)</li>
 * <li>Redis ile rate limit kontrolü</li>
 * <li>Spring AI + Ollama ile risk analizi</li>
 * <li>Kafka'ya asenkron audit event publish</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/v1/sentinel")
public class SentinelController {

    private static final Logger log = LoggerFactory.getLogger(SentinelController.class);

    private final RiskAnalysisService riskAnalysisService;
    private final RateLimitService rateLimitService;
    private final AuditEventProducer auditEventProducer;

    public SentinelController(
            RiskAnalysisService riskAnalysisService,
            RateLimitService rateLimitService,
            AuditEventProducer auditEventProducer) {
        this.riskAnalysisService = riskAnalysisService;
        this.rateLimitService = rateLimitService;
        this.auditEventProducer = auditEventProducer;
    }

    /**
     * Verilen metin payload'ını AI ile analiz eder ve risk değerlendirmesi döner.
     *
     * <p>
     * Gerekli roller: {@code ROLE_USER} veya {@code ROLE_ADMIN}
     *
     * @param request        Analiz edilecek payload içeren istek
     * @param authentication JWT token üzerinden çekilen kullanıcı kimliği
     * @return Risk skoru, kategori ve gerekçe içeren değerlendirme
     */
    @PostMapping("/analyze")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<RiskAssessmentResponse> analyze(
            @Valid @RequestBody RiskAssessmentRequest request, JwtAuthenticationToken authentication) {

        String userId = authentication.getName();
        log.info("Risk analysis requested by userId={}, payloadLength={}", userId, request.payload().length());

        // 1. Rate limit kontrolü
        rateLimitService.checkRateLimit(userId);

        // 2. AI risk analizi
        RiskAssessmentResponse response = riskAnalysisService.analyze(request.payload());

        // 3. Asenkron audit event yayını
        auditEventProducer.publishAuditEvent(AuditEvent.builder()
                .requestId(response.requestId())
                .userId(userId)
                .riskScore(response.riskScore())
                .category(response.category())
                .reason(response.reason())
                .timestamp(response.timestamp())
                .payloadSnippet(request.payload().substring(0, Math.min(request.payload().length(), 100)))
                .build());

        log.info(
                "Risk analysis completed: requestId={}, riskScore={}, category={}",
                response.requestId(),
                response.riskScore(),
                response.category());

        return ResponseEntity.ok(response);
    }

    /**
     * Yönetici sağlık kontrol endpoint'i.
     *
     * <p>
     * Gerekli roller: {@code ROLE_ADMIN}
     */
    @GetMapping("/admin/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminHealth() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "sentinel-ai-gateway", "version", "1.0.0"));
    }
}
