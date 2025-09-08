package org.marensovich.bot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.marensovich.bot.bot.Bot;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendInvoice;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final PricingService pricingService;

    public void sendInvoice(Long chatId, BigDecimal amountRubles) {
        // Конвертация рублей в звезды
        int starsAmount = pricingService.convertRublesToStars(amountRubles);

        SendInvoice sendInvoice = new SendInvoice();
        sendInvoice.setChatId(chatId.toString());
        sendInvoice.setTitle("Пополнение баланса");
        sendInvoice.setDescription("Пополнение баланса на " + amountRubles + " ₽");
        sendInvoice.setPayload("topup_" + System.currentTimeMillis());
        sendInvoice.setProviderToken("YOUR_PROVIDER_TOKEN");
        sendInvoice.setCurrency("XTR"); // Валюта для Stars - "XTR"
        sendInvoice.setPrices(Collections.singletonList(
                new LabeledPrice("Рубли", starsAmount) // Но сумма в звездах
        ));

        try {
            Bot.getInstance().execute(sendInvoice);
        } catch (TelegramApiException e) {
            log.error("Error sending invoice", e);
        }
    }

    private InlineKeyboardMarkup createInvoiceKeyboard() {
        // Клавиатура с вариантами пополнения в рублях
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(
                createInvoiceButton("50 ₽", "topup:50"),
                createInvoiceButton("100 ₽", "topup:100")
        ));

        rows.add(List.of(
                createInvoiceButton("200 ₽", "topup:200"),
                createInvoiceButton("500 ₽", "topup:500")
        ));

        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private InlineKeyboardButton createInvoiceButton(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }
}