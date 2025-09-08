package org.marensovich.bot.db.models;

import jakarta.persistence.*;
import lombok.Data;
import org.marensovich.bot.services.AI.GPT.Data.AIModels;
import org.marensovich.bot.services.AI.GPT.Data.TemperatureParameter;

import java.math.BigDecimal;

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

    @Column(precision = 10, scale = 2, name = "balance", nullable = false)
    private BigDecimal balance = BigDecimal.valueOf(10);

}
