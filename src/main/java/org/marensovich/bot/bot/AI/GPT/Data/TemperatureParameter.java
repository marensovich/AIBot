package org.marensovich.bot.bot.AI.GPT.Data;

import lombok.Getter;

public enum TemperatureParameter {

    Default(1.0f),
    Coding(0.0f),
    Math(0.0f),
    DataAnalysis(1.0f),
    GeneralConversation(1.3f),
    Translation(1.3f),
    CreativeWriting(1.5f);

    @Getter
    private final float temperature;

    TemperatureParameter(float temperature) {
        this.temperature = temperature;
    }
}
