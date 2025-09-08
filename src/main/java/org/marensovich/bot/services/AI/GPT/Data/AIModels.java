package org.marensovich.bot.services.AI.GPT.Data;

import lombok.Getter;

public enum AIModels {

    YANDEX,
    DEEPSEEK;

    public enum YandexModels {
        YandexLite("yandexgpt-lite"),
        YandexGPTPro("yandexgpt"),
        YandexGPTPro32k("yandexgpt-32k"),
        Llama8B("llama-lite"),
        Llama70B("llama");

        @Getter
        private final String model;

        YandexModels(String model) {
            this.model = model;
        }
    }


    public enum DeepSeekModels {
        DeepSeek_V3("deepseek-chat"),
        DeepSeek_R1("deepseek-reasoner");

        @Getter
        private final String model;

        DeepSeekModels(String model) {
            this.model = model;
        }
    }
}






