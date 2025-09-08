package org.marensovich.bot.services.AI.Voice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class YandexVoiceService {
    private final WebClient webClient;
    private final String folderId;
    private final String language;

    public YandexVoiceService(
            @Value("${yandex.speechkit.api.key}") String apiKey,
            @Value("${yandex.speechkit.folder.id}") String folderId,
            @Value("${yandex.speechkit.api.url}") String apiUrl,
            @Value("${yandex.speechkit.language:ru-RU}") String language) {

        this.folderId = folderId;
        this.language = language;
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .defaultHeader("Authorization", "Api-Key " + apiKey)
                .build();
    }

    public Mono<String> recognizeSpeech(byte[] audioData) {
        log.info("Recognizing speech ({} bytes)", audioData.length);

        if (audioData.length > 1_000_000) {
            return Mono.error(new IllegalArgumentException("Audio file too large (max 1MB)"));
        }

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/stt:recognize")
                        .queryParam("folderId", folderId)
                        .queryParam("lang", language)
                        .build())
                .bodyValue(audioData)
                .retrieve()
                .bodyToMono(SpeechRecognitionResponse.class)
                .map(SpeechRecognitionResponse::getResult)
                .doOnSuccess(result -> log.info("Recognition successful: {}", result))
                .onErrorResume(e -> {
                    log.error("Recognition failed", e);
                    return Mono.just("Ошибка распознавания речи");
                });
    }

    private static class SpeechRecognitionResponse {
        private String result;

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }
}
