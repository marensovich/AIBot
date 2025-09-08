package org.marensovich.bot.bot.Update;

import org.marensovich.bot.bot.Bot;
import org.marensovich.bot.bot.Services.UserService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class UpdateManager {

    private final UserService userService;

    public UpdateManager(UserService userService) {
        this.userService = userService;
    }

    public void updateHandler(Update update) throws TelegramApiException {

        if (update.hasMessage() || update.hasCallbackQuery()){
            if (update.hasMessage()){
                if (userService.isUserExists(update.getMessage().getFrom().getId())){
                    if (!Bot.getInstance().getCommandManager().hasActiveCommand(update.getMessage().getFrom().getId())){
                        if (update.getMessage().hasText()){
                            if (update.getMessage().getText().startsWith("/")){
                                if (!Bot.getInstance().getCommandManager().executeCommand(update)) {
                                    String text = "Команда не распознана, проверьте правильность написания команды. \n\n" +
                                            "Команды с доп. параметрами указаны отдельной графой в информации. Подробнее в /help.";
                                    SendMessage message = new SendMessage();
                                    message.setChatId(update.getMessage().getChatId().toString());
                                    message.setText(text);
                                    try {
                                        Bot.getInstance().execute(message);
                                    } catch (TelegramApiException e) {
                                        throw new RuntimeException(e);
                                    }
                                    return;
                                }
                            }
                        }
                    } else {
                        Bot.getInstance().getCommandManager().executeCommand(update);
                        return;
                    }
                } else {
                    userService.createUser(update.getMessage().getFrom().getId());
                }
            }
            if (update.hasCallbackQuery()) {
                if (userService.isUserExists(update.getCallbackQuery().getFrom().getId())){
                    boolean handled = Bot.getInstance().getCallbackManager().handleCallback(update);
                    if (!handled) {
                        SendMessage errorMsg = new SendMessage();
                        errorMsg.setChatId(update.getCallbackQuery().getMessage().getChatId().toString());
                        errorMsg.setText("Действие не распознано, попробуйте ещё раз");
                        try {
                            Bot.getInstance().execute(errorMsg);
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    userService.createUser(update.getCallbackQuery().getFrom().getId());
                }
            }
        }
    }

}