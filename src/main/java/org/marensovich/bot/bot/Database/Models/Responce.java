package org.marensovich.bot.bot.Database.Models;

import jakarta.persistence.*;
import lombok.Data;
import org.marensovich.bot.bot.Services.AI.GPT.Data.AIModels;

import java.math.BigDecimal;

@Data
@Entity
@Table (name = "responce")
public class Responce {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true)
    private Long id;

    @Column(name = "userId", nullable = false)
    private Long userId;

    @Lob
    @Column(name = "requestMessage", nullable = false, columnDefinition = "TEXT")
    private String requestMessage;

    @Lob
    @Column(name = "responceMessage", nullable = false, columnDefinition = "TEXT")
    private String responceMessage;

    @Column(name = "requestTokens", nullable = false)
    private BigDecimal requestTokens;

    @Column(name = "responseTokens", nullable = false)
    private BigDecimal responseTokens;

    @Column(name = "modelType", nullable = false)
    @Enumerated(EnumType.STRING)
    private AIModels modelType;

    @Column(name = "deepSeekModels", nullable = false)
    @Enumerated(EnumType.STRING)
    private AIModels.DeepSeekModels deepSeekModels;

    @Column(name = "yandexModels", nullable = false)
    @Enumerated(EnumType.STRING)
    private AIModels.YandexModels yandexModels;

}
