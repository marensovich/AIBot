package org.marensovich.bot.bot.Command.Commands;

import lombok.RequiredArgsConstructor;
import org.marensovich.bot.bot.Bot;
import org.marensovich.bot.bot.Command.Interfaces.Command;
import org.marensovich.bot.bot.Database.Repositories.UserRepository;
import org.marensovich.bot.bot.Services.InvoiceService;
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
        if (Bot.getInstance().getCommandManager().hasActiveCommand(update.getMessage().getFrom().getId())) {
            try {
                BigDecimal amount = new BigDecimal(update.getMessage().getText());
                invoiceService.sendInvoice(update.getMessage().getFrom().getId(), amount);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            Bot.getInstance().getCommandManager().unsetActiveCommand(update.getMessage().getFrom().getId());
            return;
        }
        Bot.getInstance().getCommandManager().setActiveCommand(update.getMessage().getFrom().getId(), this);

        Long userId = update.getMessage().getFrom().getId();
        Long chatId = update.getMessage().getChatId();

        BigDecimal balance = userRepository.getUserByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getTokens();

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(String.format(
                "💎 Ваш баланс: %s токенов\n\n" +
                        "✨ Пополнить баланс можно через Telegram Stars\n" +
                        "🎯 1 токен - 0,001 ₽\n\n" +
                        "Выберите сумму для пополнения:",
                balance
        ));

        InlineKeyboardMarkup keyboard = createBalanceKeyboard();
        message.setReplyMarkup(keyboard);

        try {
            Bot.getInstance().execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        Bot.getInstance().getCommandManager().unsetActiveCommand(update.getMessage().getFrom().getId());
    }

    private InlineKeyboardMarkup createBalanceKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(Arrays.asList(
                createButton("1 млн. - 300 ₽", "topup:300"),
                createButton("2 млн. - 500 ₽", "topup:500")
        ));

        rows.add(Arrays.asList(
                createButton("5 млн. - 800 ₽", "topup:800"),
                createButton("10 млн. - 1000 ₽", "topup:1000")
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
            askForCustomAmount(update.getCallbackQuery().getFrom().getId());
        } else {
            try {
                BigDecimal amount = new BigDecimal(amountStr);
                invoiceService.sendInvoice(update.getCallbackQuery().getFrom().getId(), amount);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    private void askForCustomAmount(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Введите кол-во токенов для пополнения:");

        try {
            Bot.getInstance().execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        Bot.getInstance().getCommandManager().setActiveCommand(chatId, this);
    }
}