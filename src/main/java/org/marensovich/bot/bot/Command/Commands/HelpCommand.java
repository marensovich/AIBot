package org.marensovich.bot.bot.Command.Commands;

import org.marensovich.bot.bot.Bot;
import org.marensovich.bot.bot.Command.Interfaces.Command;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class HelpCommand implements Command {
    @Override
    public String getName() {
        return "/help";
    }

    @Override
    public void execute(Update update) {
        Bot.getInstance().getCommandManager().setActiveCommand(update.getMessage().getFrom().getId(), this);

        String reply = """
                Информация о боте
                """;

        SendMessage message = new SendMessage();
        message.setChatId(update.getMessage().getChatId());
        message.setText(reply);
        message.enableHtml(true);

        Bot.getInstance().showBotAction(update.getMessage().getFrom().getId(), ActionType.TYPING);

        try {
            Bot.getInstance().execute(message);
        } catch (TelegramApiException e){
            e.printStackTrace();
        }

        Bot.getInstance().getCommandManager().unsetActiveCommand(update.getMessage().getFrom().getId());
    }
}
