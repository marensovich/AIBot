package org.marensovich.bot.bot.Callback.Callbacks.AI;

import lombok.RequiredArgsConstructor;
import org.marensovich.bot.bot.Callback.Interface.CallbackHandler;
import org.marensovich.bot.bot.Command.Commands.AiCommand;
import org.marensovich.bot.bot.Command.Commands.SettingsCommand;
import org.marensovich.bot.bot.Database.Repositories.UserRepository;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class RunSettingsHandler implements CallbackHandler {

    private final UserRepository userRepository;
    @Override
    public String getCallbackData() {
        return AiCommand.CALLBACK_SETTINGS;
    }

    @Override
    public void handle(Update update) {
        SettingsCommand settingsCommand = new SettingsCommand(userRepository);
        settingsCommand.execute(update);
    }
}