package com.intellishare.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellishare.audit.AuditService;
import com.intellishare.dto.intelligence.IntelligenceRequest;
import com.intellishare.dto.intelligence.IntelligenceResponse;
import com.intellishare.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class IntelligenceService {
    private final ObjectMapper mapper;
    private final AuditService audit;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String apiKey;
    private final String model;
    private final String apiBase;

    @Autowired
    public IntelligenceService(ObjectMapper mapper, AuditService audit,
            @Value("${intellishare.ai.gemini-api-key:}") String apiKey,
            @Value("${intellishare.ai.gemini-model:gemini-3.8-flash}") String model) {
        this(mapper, audit, apiKey, model, "https://generativelanguage.googleapis.com");
    }

    IntelligenceService(ObjectMapper mapper, AuditService audit, String apiKey, String model, String apiBase) {
        this.mapper = mapper; this.audit = audit; this.apiKey = apiKey; this.model = model; this.apiBase = apiBase;
    }

    public IntelligenceResponse analyze(User actor, IntelligenceRequest request) {
        if (apiKey == null || apiKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI provider is not configured on the server");
        if (model == null || !model.matches("[A-Za-z0-9._-]{1,100}")) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI provider configuration is invalid");
        try {
            var parts = mapper.createArrayNode().addObject().put("text", request.prompt());
            var body = mapper.createObjectNode();
            body.putArray("contents").addObject().set("parts", parts);
            if (request.webSearch()) body.putArray("tools").addObject().putObject("google_search");
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(apiBase + "/v1beta/models/" + model + ":generateContent"))
                    .timeout(Duration.ofSeconds(45)).header("Content-Type", "application/json").header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "AI provider rate limit reached; try again later");
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI provider could not complete the request");
            JsonNode root = mapper.readTree(response.body());
            StringBuilder answer = new StringBuilder();
            for (JsonNode part : root.path("candidates").path(0).path("content").path("parts")) {
                if (part.hasNonNull("text")) { if (!answer.isEmpty()) answer.append('\n'); answer.append(part.path("text").asText()); }
            }
            if (answer.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI provider returned no usable text");
            var sources = new ArrayList<IntelligenceResponse.Source>();
            for (JsonNode chunk : root.path("candidates").path(0).path("groundingMetadata").path("groundingChunks")) {
                JsonNode web = chunk.path("web");
                String uri = web.path("uri").asText("");
                if (uri.startsWith("https://")) sources.add(new IntelligenceResponse.Source(web.path("title").asText("Web source"), uri));
            }
            audit.record(actor, "AI_ANALYSIS", "intelligence", "Prompt text sent to Gemini; prompt omitted from audit");
            return new IntelligenceResponse(answer.toString(), "Google Gemini", !sources.isEmpty(),
                    "AI-generated; verify all information independently. Only your prompt text was sent to Google Gemini; no case records or files were included.", List.copyOf(sources));
        } catch (ResponseStatusException e) { throw e; }
        catch (java.net.http.HttpTimeoutException e) { throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "AI provider timed out; try again later"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "AI request was interrupted"); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI provider response could not be processed"); }
    }
}
