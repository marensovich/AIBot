package org.marensovich.bot.bot.Callback.Interface;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface PrefixCallbackHandler extends CallbackHandler {
    String getPrefixCallbackData();
    void handle(Update update);
}