package org.marensovich.bot.services.AI.Vision;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class YandexVisionService {

    private final WebClient webClient;
    private final String apiKey;
    private final String folderId;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public YandexVisionService(
            @Value("${yandex.vision.api.key}") String apiKey,
            @Value("${yandex.vision.api.folder.id}") String folderId,
            @Value("${yandex.vision.api.url}") String apiUrl) {

        this.apiKey = apiKey;
        this.folderId = folderId;

        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", "Api-Key " + apiKey)
                .defaultHeader("x-folder-id", folderId)
                .defaultHeader("x-data-logging-enabled", "true")
                .build();
    }

    public Mono<String> recognizeTextFromImage(byte[] imageData, String mimeType) {
        String encodedImage = Base64.getEncoder().encodeToString(imageData);

        Map<String, Object> requestBody = createRequestBody(encodedImage, mimeType);

        return webClient.post()
                .uri("/recognizeText")
                .bodyValue(requestBody)
                .exchangeToMono(response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(body -> {
                                if (response.statusCode().isError()) {
                                    return Mono.error(new RuntimeException(
                                            "API error: " + response.statusCode() + " - " + body));
                                }
                                return Mono.just(body);
                            });
                })
                .map(this::parseResponse)
                .onErrorResume(e -> {
                    return Mono.just("Ошибка: " + e.getMessage() +
                            (e.getCause() != null ? " (" + e.getCause().getMessage() + ")" : ""));
                });
    }

    private Map<String, Object> createRequestBody(String encodedImage, String mimeType) {
        return Map.of(
                "mimeType", mimeType,
                "languageCodes", List.of("ru", "en"),
                "model", "page",
                "content", encodedImage
        );
    }

    private String parseResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode blocks = root.path("result").path("textAnnotation").path("blocks");

            StringBuilder result = new StringBuilder();
            for (JsonNode block : blocks) {
                for (JsonNode line : block.path("lines")) {
                    for (JsonNode word : line.path("words")) {
                        result.append(word.path("text").asText()).append(" ");
                    }
                    result.append("\n");
                }
                result.append("\n");
            }
            return result.toString().trim();
        } catch (Exception e) {
            return "Ошибка парсинга ответа: " + e.getMessage();
        }
    }
}