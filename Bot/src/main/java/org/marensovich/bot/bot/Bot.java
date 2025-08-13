package org.marensovich.bot.bot;

import org.marensovich.bot.bot.AI.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Bot extends TelegramLongPollingBot {

    private final String botToken;
    private final String botUsername;

    @Autowired
    private AiService aiService;

    public Bot(String botToken, String botUsername) {
        this.botToken = botToken;
        this.botUsername = botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.getMessage().getText().startsWith("/ask")) {
            String userText = update.getMessage().getText().replace("/ask", "").trim();
            try {

                //TODO:Сделать выполнение задачи ассинхронным.
                String aiResponse = aiService.getAiResponse(userText).block();
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(update.getMessage().getChatId().toString());
                sendMessage.setText("Ответ от ИИ: " + aiResponse);
                execute(sendMessage);
            } catch (Exception e) {
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(update.getMessage().getChatId().toString());
                errorMessage.setText("Ошибка: " + e.getMessage());
                try {
                    execute(errorMessage);
                } catch (TelegramApiException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        SendMessage message = new SendMessage();
        message.setText("Вы написали: " + update.getMessage().getText());
        message.setChatId(update.getMessage().getChatId().toString());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
