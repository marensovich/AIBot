package org.marensovich.bot.bot.AI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class YandexGptService {

    private final WebClient webClient;
    private final String apiKey;
    private final String catalogId;

    public YandexGptService(
            @Value("${yandex.gpt.api.key}") String apiKey,
            @Value("${yandex.gpt.api.catalog.id}") String catalogId,
            @Value("${yandex.gpt.api.url}") String apiUrl) {

        this.apiKey = apiKey;
        this.catalogId = catalogId;

        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", "Api-Key " + apiKey)
                .build();
    }

    public Mono<String> getAiResponse(String userMessage) {
        Map<String, Object> requestBody = createRequestBody(userMessage);

        return webClient.post()
                .uri("/foundationModels/v1/completion")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseAndFormatResponse);
    }

    private Map<String, Object> createRequestBody(String userMessage) {
        return Map.of(
                "modelUri", "gpt://" + catalogId + "/yandexgpt-lite",
                "completionOptions", Map.of(
                        "stream", false,
                        "temperature", 0.6,
                        "maxTokens", "2000"
                ),
                "messages", new Object[]{
                        Map.of(
                                "role", "system",
                                "text", "Ты — умный ассистент."
                        ),
                        Map.of(
                                "role", "user",
                                "text", userMessage
                        )
                }
        );
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String parseAndFormatResponse(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode resultNode = rootNode.path("result");

            String assistantText = resultNode.path("alternatives")
                    .get(0)
                    .path("message")
                    .path("text")
                    .asText();

            String modelVersion = rootNode.path("modelVersion").asText();

            return String.format(
                    """
                    💬 Ответ от Yandex GPT (версия от %s):
                    
                    %s              
                    """,
                    modelVersion,
                    assistantText
            );

        } catch (Exception e) {
            return "⚠️ Ошибка при обработке ответа от Yandex GPT: " + e.getMessage();
        }
    }

}