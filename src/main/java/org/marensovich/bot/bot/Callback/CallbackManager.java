package org.marensovich.bot.bot.Callback;

import org.marensovich.bot.bot.Callback.Interface.CallbackHandler;
import org.marensovich.bot.bot.Callback.Interface.PrefixCallbackHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CallbackManager {

    private final Map<String, CallbackHandler> handlers = new HashMap<>();

    private final Map<String, PrefixCallbackHandler> prefixHandlers = new HashMap<>();

    @Autowired
    public CallbackManager(List<CallbackHandler> handlers, List<PrefixCallbackHandler> prefixHandlers) {
        handlers.forEach(this::registerHandler);
        prefixHandlers.forEach(this::registerPrefixHandler);
    }

    private void registerHandler(CallbackHandler handler) {
        handlers.put(handler.getCallbackData().toLowerCase(), handler);
    }

    private void registerPrefixHandler(PrefixCallbackHandler handler) {
        prefixHandlers.put(handler.getPrefixCallbackData().toLowerCase(), handler);
    }

    public boolean handleCallback(Update update){
        if (!update.hasCallbackQuery()) {
            return false;
        }
        String callbackData = update.getCallbackQuery().getData();

        CallbackHandler handler = handlers.get(callbackData);
        if (handler != null) {
            handler.handle(update);
            return true;
        }


        for (Map.Entry<String, PrefixCallbackHandler> entry : prefixHandlers.entrySet()) {
            if (callbackData.startsWith(entry.getKey())) {
                entry.getValue().handle(update);
                return true;
            }
        }
        return false;
    }
}