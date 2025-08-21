package org.marensovich.bot.bot.Command.Commands;

import org.marensovich.bot.bot.Bot;
import org.marensovich.bot.bot.Command.Interfaces.Command;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class CancelCommand implements Command {
    @Override
    public String getName() {
        return "/cancel";
    }

    @Override
    public void execute(Update update) {
        if (Bot.getInstance().getCommandManager().hasActiveCommand(update.getMessage().getFrom().getId())){
            Bot.getInstance().getCommandManager().unsetActiveCommand(update.getMessage().getFrom().getId());
            SendMessage msg = new SendMessage();
            msg.setChatId(update.getMessage().getChatId().toString());
            msg.setReplyMarkup(Bot.getInstance().removeKeyboard());
            msg.setText("Активная команда была удалена.");
            try {
                Bot.getInstance().execute(msg);
            } catch (TelegramApiException e) {
                Bot.getInstance().sendErrorMessage(update.getMessage().getChatId(), "⚠️ Ошибка при работе бота, обратитесь к администратору");
                Bot.getInstance().getCommandManager().unsetActiveCommand(update.getMessage().getChatId());
                throw new RuntimeException(e);
            }
            return;
        }
        SendMessage msg = new SendMessage();
        msg.setChatId(update.getMessage().getChatId().toString());
        msg.setText("❌ Нет активных команд");
        try {
            Bot.getInstance().execute(msg);
        } catch (TelegramApiException e) {
            Bot.getInstance().sendErrorMessage(update.getMessage().getChatId(), "⚠️ Ошибка при работе бота, обратитесь к администратору");
            Bot.getInstance().getCommandManager().unsetActiveCommand(update.getMessage().getChatId());
            throw new RuntimeException(e);
        }
    }

}
