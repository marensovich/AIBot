package org.marensovich.bot.bot.AI.Data;

import lombok.Getter;

public enum AIModels {

    YandexLite("yandexgpt-lite"),
    DeepSeek_V3("deepseek-chat"),
    DeepSeek_R1("deepseek-reasoner");


    @Getter
    private final String model;

    AIModels(String model) {
        this.model = model;
    }
}
