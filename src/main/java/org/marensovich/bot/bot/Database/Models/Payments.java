package org.marensovich.bot.bot.Database.Models;

import jakarta.persistence.*;
import lombok.Data;
import org.marensovich.bot.bot.Data.PaymentsStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
public class Payments {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private String telegramPaymentId;
    private BigDecimal amount;
    private String currency = "RUB";
    private String payload;

    @Enumerated(EnumType.STRING)
    private PaymentsStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}



