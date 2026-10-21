package com.intellishare.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellishare.audit.AuditService;
import com.intellishare.dto.intelligence.IntelligenceRequest;
import com.intellishare.model.User;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IntelligenceServiceTest {
    @Test
    void sendsOnlyPromptAndReturnsGroundingCitations() throws Exception {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> key = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1beta/models/gemini-3.8-flash:generateContent", exchange -> {
            key.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"AI result\"}]},\"groundingMetadata\":{\"groundingChunks\":[{\"web\":{\"uri\":\"https://example.org/source\",\"title\":\"Example\"}}]}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json"); exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response); exchange.close();
        });
        server.start();
        try {
            AuditService audit = mock(AuditService.class);
            var service = new IntelligenceService(new ObjectMapper(), audit, "server-secret", "gemini-3.8-flash", "http://127.0.0.1:" + server.getAddress().getPort());
            User user = new User("Test", "ai@test.invalid", "hash", "AI-1", "Test Agency", null, null);
            var result = service.analyze(user, new IntelligenceRequest("prompt only", true));
            assertEquals("server-secret", key.get());
            assertTrue(body.get().contains("prompt only"));
            assertFalse(body.get().contains("caseContext"));
            assertTrue(body.get().contains("google_search"));
            assertEquals("AI result", result.text());
            assertEquals("https://example.org/source", result.sources().getFirst().uri());
            verify(audit).record(user, "AI_ANALYSIS", "intelligence", "Prompt text sent to Gemini; prompt omitted from audit");
        } finally { server.stop(0); }
    }

    @Test
    void missingProviderKeyFailsClosed() {
        var service = new IntelligenceService(new ObjectMapper(), mock(AuditService.class), "", "gemini-3.8-flash", "https://example.invalid");
        var user = new User("Test", "ai@test.invalid", "hash", "AI-1", "Test Agency", null, null);
        var error = assertThrows(ResponseStatusException.class, () -> service.analyze(user, new IntelligenceRequest("prompt only", false)));
        assertEquals(503, error.getStatusCode().value());
    }
}
