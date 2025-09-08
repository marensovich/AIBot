package org.marensovich.bot.bot.Services;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PricingService {
    public int convertTokensToStars(BigDecimal tokens) {
        BigDecimal stars = tokens.divide(new BigDecimal("1000"), 10, RoundingMode.HALF_UP);
        int starsInt = stars.setScale(0, RoundingMode.UP).intValue();
        return Math.max(1, starsInt);
    }
    public BigDecimal convertStarsToTokens(int stars) {
        return BigDecimal.valueOf(stars)
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }
}