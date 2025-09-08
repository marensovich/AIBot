package org.marensovich.bot.bot.Command.Commands;

import lombok.RequiredArgsConstructor;
import org.marensovich.bot.bot.Bot;
import org.marensovich.bot.bot.Command.Interfaces.Command;
import org.marensovich.bot.db.repositories.UserRepository;
import org.marensovich.bot.services.InvoiceService;
import org.marensovich.bot.services.PaymentsService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BalanceCommand implements Command {

    public static final String CALLBACK_TOPUP = "topup:";

    private final UserRepository userRepository;
    private final InvoiceService invoiceService;

    @Override
    public String getName() {
        return "/balance";
    }

    @Override
    public void execute(Update update) {
        Long userId = update.getMessage().getFrom().getId();
        Long chatId = update.getMessage().getChatId();

        BigDecimal balance = userRepository.getUserByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getBalance();

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(String.format(
                "💎 Ваш баланс: %.2f ₽\n\n" +
                        "✨ Пополнить баланс можно через Telegram Stars\n" +
                        "🎯 100 звезд = 1 рубль\n\n" +
                        "Выберите сумму для пополнения:",
                balance
        ));

        // Клавиатура с вариантами пополнения
        InlineKeyboardMarkup keyboard = createBalanceKeyboard();
        message.setReplyMarkup(keyboard);

        try {
            Bot.getInstance().execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private InlineKeyboardMarkup createBalanceKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(Arrays.asList(
                createButton("50 ₽", "topup:50"),
                createButton("100 ₽", "topup:100")
        ));

        rows.add(Arrays.asList(
                createButton("200 ₽", "topup:200"),
                createButton("500 ₽", "topup:500")
        ));

        rows.add(Collections.singletonList(
                createButton("💎 Другая сумма", "topup:custom")
        ));

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }

    public void handleCallback(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        String amountStr = callbackData.split(":")[1];

        if ("custom".equals(amountStr)) {
            // Запрос произвольной суммы
            askForCustomAmount(update.getCallbackQuery().getFrom().getId());
        } else {
            try {
                BigDecimal amount = new BigDecimal(amountStr);
                invoiceService.sendInvoice(update.getCallbackQuery().getFrom().getId(), amount);
            } catch (NumberFormatException e) {
                // Обработка ошибки
            }
        }
    }

    private void askForCustomAmount(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Введите сумму в рублях для пополнения:");

        try {
            Bot.getInstance().execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}