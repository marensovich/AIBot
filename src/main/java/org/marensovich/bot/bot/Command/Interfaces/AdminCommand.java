package org.marensovich.bot.bot.Command.Interfaces;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface AdminCommand extends Command {
    boolean isAdminRequired();
    void execute(Update update);
}