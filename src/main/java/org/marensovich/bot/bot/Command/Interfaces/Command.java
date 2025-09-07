package org.marensovich.bot.bot.Command.Interfaces;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface Command {
    String getName();
    void execute(Update update);
}
