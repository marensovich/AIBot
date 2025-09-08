package org.marensovich.bot.bot.Services;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PricingService {

    // Цены на услуги в рублях
    public static final BigDecimal PRICE_GENERATE_IMAGE = BigDecimal.valueOf(50.00);
    public static final BigDecimal PRICE_PREMIUM_CHAT = BigDecimal.valueOf(10.00);
    public static final BigDecimal PRICE_VOICE_MESSAGE = BigDecimal.valueOf(25.00);
    public static final BigDecimal PRICE_AI_RESPONSE = BigDecimal.valueOf(5.00);

    public BigDecimal getPriceForService(String serviceType) {
        return switch (serviceType) {
            case "generate_image" -> PRICE_GENERATE_IMAGE;
            case "premium_chat" -> PRICE_PREMIUM_CHAT;
            case "voice_message" -> PRICE_VOICE_MESSAGE;
            case "ai_response" -> PRICE_AI_RESPONSE;
            default -> BigDecimal.ZERO;
        };
    }

    // Конвертация рублей в звезды для инвойсов (1 рубль = 100 звезд)
    public int convertRublesToStars(BigDecimal rubles) {
        return rubles.multiply(BigDecimal.valueOf(100)).intValue();
    }

    // Конвертация звезд в рубли (100 звезд = 1 рубль)
    public BigDecimal convertStarsToRubles(int stars) {
        return BigDecimal.valueOf(stars)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }
}