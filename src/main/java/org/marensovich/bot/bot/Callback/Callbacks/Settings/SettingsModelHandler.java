package org.marensovich.bot.bot.Callback.Callbacks.Settings;

import org.marensovich.bot.bot.Callback.Interface.PrefixCallbackHandler;
import org.marensovich.bot.bot.Command.Commands.SettingsCommand;
import org.telegram.telegrambots.meta.api.objects.Update;

public class SettingsModelHandler implements PrefixCallbackHandler {
    @Override
    public String getPrefixCallbackData() {
        return SettingsCommand.CALLBACK_PREFIX;
    }

    @Override
    public void handle(Update update) {

    }
}
