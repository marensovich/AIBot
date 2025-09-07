package org.marensovich.bot.bot.Callback.Interface;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface CallbackHandler {
    String getCallbackData();
    void handle(Update update);

}
