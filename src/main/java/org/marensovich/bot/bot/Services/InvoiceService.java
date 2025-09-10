package org.marensovich.bot.bot.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.marensovich.bot.bot.Bot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendInvoice;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final PricingService pricingService;
    @Autowired TextService textService;

    @Value("${telegram.bot.providerInvoiceToken}")
    private String providerToken;

    public void sendInvoice(Long chatId, BigDecimal amountTokens) {
        int starsAmount = pricingService.convertTokensToStars(amountTokens);

        SendInvoice sendInvoice = new SendInvoice();
        sendInvoice.setChatId(chatId.toString());
        sendInvoice.setTitle("Пополнение баланса");
        sendInvoice.setDescription("Пополнение баланса на " + formatNumberWithDots(amountTokens) + " " + textService.tokensFormat(amountTokens.intValue()));
        sendInvoice.setPayload("topup_" + System.currentTimeMillis());
        sendInvoice.setCurrency("XTR");
        sendInvoice.setProviderToken(providerToken);
        sendInvoice.setPrices(Collections.singletonList(
                new LabeledPrice("Рубли", starsAmount)
        ));

        try {
            Bot.getInstance().execute(sendInvoice);
        } catch (TelegramApiException e) {
            log.error("Error sending invoice", e);
        }
    }


    private String formatNumberWithDots(BigDecimal number) {
        NumberFormat formatter = NumberFormat.getInstance(Locale.GERMAN);
        formatter.setGroupingUsed(true);
        return formatter.format(number);
    }
}