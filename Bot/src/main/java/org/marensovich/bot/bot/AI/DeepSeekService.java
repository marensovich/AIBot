package org.marensovich.bot.bot.AI;

import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DeepSeekService {

    private final WebClient webClient;
    private final String apiKey;
    private final Duration timeout;

    public DeepSeekService(
            @Value("${deepseek.api.key}") String apiKey,
            @Value("${deepseek.api.url}") String apiUrl,
            @Value("${deepseek.api.timeout:30000}") long timeoutMs) {

        this.apiKey = apiKey;
        this.timeout = Duration.ofMillis(timeoutMs);

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(30))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS)));

        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    public Mono<String> getAiResponse(String text) {
        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(createRequest(text))
                .retrieve()
                .bodyToMono(DeepSeekResponse.class)
                .timeout(timeout)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500)))
                .map(this::processResponse)
                .onErrorResume(this::handleError);
    }

    private DeepSeekRequest createRequest(String text) {
        return new DeepSeekRequest(
                "deepseek-chat",
                new Message[]{
                        new Message("system", "You are a helpful assistant."),
                        new Message("user", text)
                },
                false
        );
    }

    private String processResponse(DeepSeekResponse response) {
        if (response.choices() == null || response.choices().length == 0) {
            throw new RuntimeException("Empty response from AI");
        }
        return response.choices()[0].message().content();
    }

    private Mono<String> handleError(Throwable error) {
        log.error("AI request failed: {}", error.getMessage());
        return Mono.just("Извините, сервис ИИ временно недоступен. Попробуйте позже.");
    }

    // DTO records
    private record DeepSeekRequest(String model, Message[] messages, boolean stream) {}
    private record Message(String role, String content) {}
    private record DeepSeekResponse(Choice[] choices) {}
    private record Choice(Message message) {}
}