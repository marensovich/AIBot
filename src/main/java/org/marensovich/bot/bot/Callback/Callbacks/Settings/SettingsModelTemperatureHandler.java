package org.marensovich.bot.bot.Callback.Callbacks.Settings;

import lombok.RequiredArgsConstructor;
import org.marensovich.bot.bot.Callback.Interface.PrefixCallbackHandler;
import org.marensovich.bot.bot.Command.Commands.SettingsCommand;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class SettingsModelTemperatureHandler implements PrefixCallbackHandler {

    private final SettingsCommand settingsCommand;
    @Override
    public String getPrefixCallbackData() {
        return SettingsCommand.CALLBACK_TEMP_PREFIX;
    }

    @Override
    public void handle(Update update) {
        settingsCommand.handleCallbackQuery(update);
    }
}
