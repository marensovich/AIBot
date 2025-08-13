package org.marensovich.bot.bot;

import org.marensovich.bot.bot.AI.DeepSeekService;
import org.marensovich.bot.bot.AI.YandexGptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVoice;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Bot extends TelegramLongPollingBot {

    private final String botToken;
    private final String botUsername;

    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private YandexGptService yandexGptService;

    public Bot(String botToken, String botUsername) {
        this.botToken = botToken;
        this.botUsername = botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.getMessage().hasVoice()) {
            SendVoice sendVoice = new SendVoice();
            sendVoice.setChatId(update.getMessage().getChatId().toString());

            // Option 1: Send using just the file_id (recommended for simple cases)
            InputFile voiceInputFile = new InputFile(update.getMessage().getVoice().getFileId());
            sendVoice.setVoice(voiceInputFile);

            try {
                execute(sendVoice); // Execute the sendVoice action
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }
        if (update.getMessage().getText().startsWith("/deepseek")) {
            String userText = update.getMessage().getText().replace("/deepseek", "").trim();
            try {

                //TODO: Сделать выполнение задачи ассинхронным.
                String aiResponse = deepSeekService.getAiResponse(userText).block();
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
            return;
        }
        if (update.getMessage().getText().startsWith("/yandex")) {
            String userText = update.getMessage().getText().replace("/yandex", "").trim();
            try {
                //TODO: Сделать выполнение задачи ассинхронным.
                String aiResponse = yandexGptService.getAiResponse(userText).block();
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(update.getMessage().getChatId().toString());
                assert aiResponse != null;
                sendMessage.setText(aiResponse);
                sendMessage.enableMarkdown(true);
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
            return;
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
