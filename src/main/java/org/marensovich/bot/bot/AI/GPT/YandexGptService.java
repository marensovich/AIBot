package org.marensovich.bot.bot.AI.GPT;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@Slf4j
public class YandexGptService {

    private final WebClient webClient;
    private final String apiKey;
    private final String catalogId;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    // Новый метод с информацией о токенах
    public Mono<AiResponse> getAiResponseWithTokens(String userMessage, String model) {
        Map<String, Object> requestBody = createRequestBody(userMessage, model);

        return webClient.post()
                .uri("/foundationModels/v1/completion")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseResponseWithTokens)
                .onErrorResume(this::handleErrorWithTokens);
    }

    // Старый метод для обратной совместимости
    public Mono<String> getAiResponse(String userMessage, String model) {
        return getAiResponseWithTokens(userMessage, model)
                .map(AiResponse::getResponse);
    }

    private Map<String, Object> createRequestBody(String userMessage, String model) {
        return Map.of(
                "modelUri", "gpt://" + catalogId + "/" + model,
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

    private AiResponse parseResponseWithTokens(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode resultNode = rootNode.path("result");

            // Получаем текст ответа
            String assistantText = resultNode.path("alternatives")
                    .get(0)
                    .path("message")
                    .path("text")
                    .asText();

            // Получаем информацию о токенах
            JsonNode usageNode = resultNode.path("usage");
            int inputTokens = usageNode.path("inputTextTokens").asInt();
            int completionTokens = usageNode.path("completionTokens").asInt();
            int totalTokens = usageNode.path("totalTokens").asInt();

            TokenUsage tokenUsage = new TokenUsage(inputTokens, completionTokens, totalTokens);

            return new AiResponse(assistantText, tokenUsage);

        } catch (Exception e) {
            log.error("Error parsing Yandex GPT response: {}", e.getMessage());
            throw new RuntimeException("Ошибка при обработке ответа от Yandex GPT", e);
        }
    }

    private Mono<AiResponse> handleErrorWithTokens(Throwable error) {
        log.error("Yandex GPT request failed: {}", error.getMessage());
        return Mono.just(new AiResponse(
                "⚠️ Ошибка при обращении к Yandex GPT. Попробуйте позже.",
                new TokenUsage(0, 0, 0)
        ));
    }

    // DTO records
    public record TokenUsage(int inputTextTokens, int completionTokens, int totalTokens) {}

    public static class AiResponse {
        private final String response;
        private final TokenUsage tokenUsage;

        public AiResponse(String response, TokenUsage tokenUsage) {
            this.response = response;
            this.tokenUsage = tokenUsage;
        }

        public String getResponse() {
            return response;
        }

        public TokenUsage getTokenUsage() {
            return tokenUsage;
        }

        public int getPromptTokens() {
            return tokenUsage != null ? tokenUsage.inputTextTokens() : 0;
        }

        public int getCompletionTokens() {
            return tokenUsage != null ? tokenUsage.completionTokens() : 0;
        }

        public int getTotalTokens() {
            return tokenUsage != null ? tokenUsage.totalTokens() : 0;
        }
    }
}