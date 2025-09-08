package org.marensovich.bot.bot.Callback.Callbacks.Balance;

import lombok.RequiredArgsConstructor;
import org.marensovich.bot.bot.Callback.Interface.PrefixCallbackHandler;
import org.marensovich.bot.bot.Command.Commands.BalanceCommand;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class BalanceTopupHandler implements PrefixCallbackHandler {

    private final BalanceCommand balanceCommand;
    @Override
    public String getPrefixCallbackData() {
        return BalanceCommand.CALLBACK_TOPUP;
    }

    @Override
    public void handle(Update update) {
        balanceCommand.handleCallback(update);
    }
}
