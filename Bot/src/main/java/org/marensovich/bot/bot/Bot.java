package org.marensovich.bot.bot;

import lombok.Getter;
import org.marensovich.bot.bot.AI.DeepSeekService;
import org.marensovich.bot.bot.AI.YandexGptService;
import org.marensovich.bot.bot.Callback.CallbackManager;
import org.marensovich.bot.bot.Command.CommandManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Bot extends TelegramLongPollingBot {

    @Autowired
    @Getter
    private CommandManager commandManager;


    @Autowired
    @Getter
    private CallbackManager callbackManager;

    @Getter
    private static Bot instance;

    private final String botToken;
    private final String botUsername;

    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private YandexGptService yandexGptService;

    public Bot(String botToken, String botUsername) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        instance = this;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText() && update.getMessage().getText().startsWith("/")) {
            commandManager.executeCommand(update);
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
        if (update.hasCallbackQuery()){
            callbackManager.handleCallback(update);
        }

    }
    
    public void sendNoAccessMessage(Update update){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId());
        sendMessage.setText("⛔ У вас нет прав для выполнения этой команды!");
        try {
            execute(sendMessage);
        } catch (TelegramApiException e){
            throw new RuntimeException();
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
