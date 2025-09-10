package org.marensovich.bot.bot.Services;

import org.springframework.stereotype.Service;

@Service
public class TextService {

    public String tokensFormat(int tokens) {
        tokens = Math.abs(tokens);

        int lastTwoDigits = tokens % 100;
        int lastDigit = tokens % 10;

        if (lastTwoDigits >= 11 && lastTwoDigits <= 14) {
            return "токенов";
        }

        return switch (lastDigit) {
            case 1 -> "токен";
            case 2, 3, 4 -> "токена";
            default -> "токенов";
        };
    }


}
