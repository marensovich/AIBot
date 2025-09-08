package org.marensovich.bot.bot.AI.GPT;

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

    public Mono<AiResponse> getAiResponseWithTokens(String userMessage, String model) {
        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(createRequest(userMessage, model))
                .retrieve()
                .bodyToMono(DeepSeekResponse.class)
                .timeout(timeout)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500)))
                .map(this::processResponseWithTokens)
                .onErrorResume(this::handleErrorWithTokens);
    }
    public Mono<String> getAiResponse(String userMessage, String model) {
        return getAiResponseWithTokens(userMessage, model)
                .map(AiResponse::getResponse);
    }

    private DeepSeekRequest createRequest(String text, String model) {
        return new DeepSeekRequest(
                model,
                new Message[]{
                        new Message("system", "You are a helpful assistant."),
                        new Message("user", text)
                },
                false
        );
    }

    private AiResponse processResponseWithTokens(DeepSeekResponse response) {
        if (response.choices() == null || response.choices().length == 0) {
            throw new RuntimeException("Empty response from AI");
        }

        String aiResponse = response.choices()[0].message().content();
        TokenUsage tokenUsage = response.usage();

        return new AiResponse(aiResponse, tokenUsage);
    }

    private Mono<AiResponse> handleErrorWithTokens(Throwable error) {
        log.error("AI request failed: {}", error.getMessage());
        return Mono.just(new AiResponse(
                "Извините, сервис ИИ временно недоступен. Попробуйте позже.",
                new TokenUsage(0, 0, 0)
        ));
    }

    // DTO records
    private record DeepSeekRequest(String model, Message[] messages, boolean stream) {}
    private record Message(String role, String content) {}

    // Обновленный Response с usage
    private record DeepSeekResponse(Choice[] choices, TokenUsage usage) {}
    private record Choice(Message message) {}
    private record TokenUsage(int prompt_tokens, int completion_tokens, int total_tokens) {}

    // Класс для возврата ответа и информации о токенах
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
            return tokenUsage != null ? tokenUsage.prompt_tokens() : 0;
        }

        public int getCompletionTokens() {
            return tokenUsage != null ? tokenUsage.completion_tokens() : 0;
        }

        public int getTotalTokens() {
            return tokenUsage != null ? tokenUsage.total_tokens() : 0;
        }
    }
}