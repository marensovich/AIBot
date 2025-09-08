package org.marensovich.bot.bot.Callback.Callbacks.Settings;

import org.marensovich.bot.bot.Callback.Interface.CallbackHandler;
import org.marensovich.bot.bot.Command.Commands.SettingsCommand;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class SettingsHeaderHandler implements CallbackHandler {
    @Override
    public String getCallbackData() {
        return SettingsCommand.CALLBACK_HEADER;
    }

    @Override
    public void handle(Update update) {

    }
}
