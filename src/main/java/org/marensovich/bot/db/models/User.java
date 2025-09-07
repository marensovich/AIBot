package org.marensovich.bot.db.models;

import jakarta.persistence.*;
import lombok.Data;
import org.marensovich.bot.bot.AI.GPT.Data.AIModels;
import org.marensovich.bot.bot.AI.GPT.Data.TemperatureParameter;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "userId", unique = true)
    private Long userId;

    @Column(name = "isAdmin")
    private boolean isAdmin;

    @Column(name = "gptType")
    @Enumerated(EnumType.STRING)
    private AIModels gptType = AIModels.YANDEX;

    @Column(name = "yandexGptModel")
    @Enumerated(EnumType.STRING)
    private AIModels.YandexModels yandexGptModel = AIModels.YandexModels.YandexLite;

    @Column(name = "deepseekGptModel")
    @Enumerated(EnumType.STRING)
    private AIModels.DeepSeekModels deepseekGptModel = AIModels.DeepSeekModels.DeepSeek_V3;

    @Column(name = "temperature")
    @Enumerated(EnumType.STRING)
    private TemperatureParameter modelTemperature = TemperatureParameter.Default;

    @Column(name = "balance", nullable = false)
    private Integer balance = 50;

}
