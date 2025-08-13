package org.marensovich.bot.bot.AI;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class AiService {

    private final WebClient webClient;

    public AiService(){
        this.webClient = WebClient.create("http://localhost:9090");
    }

    public Mono<String> getAiResponse(String text){
        return webClient.post()
                .uri("/ask_ai")
                .bodyValue(new AiRequest(text))
                .retrieve()
                .bodyToMono(AiResponse.class)
                .map(AiResponse::getResponse);
    }

    private static record AiRequest(String text){}
    private static record AiResponse(String response) {
        public String getResponse() {
            return response;
        }
    }

}
