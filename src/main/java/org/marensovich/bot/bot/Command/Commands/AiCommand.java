package org.marensovich.bot.bot.Command.Commands;

import org.marensovich.bot.bot.Services.AI.GPT.Data.AIModels;
import org.marensovich.bot.bot.Services.AI.GPT.DeepSeekService;
import org.marensovich.bot.bot.Services.AI.GPT.YandexGptService;
import org.marensovich.bot.bot.Bot;
import org.marensovich.bot.bot.Command.Interfaces.Command;
import org.marensovich.bot.bot.Database.Models.User;
import org.marensovich.bot.bot.Database.Repositories.UserRepository;
import org.marensovich.bot.bot.Services.ResponceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class AiCommand implements Command {

    private final UserRepository userRepository;
    @Autowired private ResponceService responceService;

    public AiCommand(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public String getName() {
        return "/ai";
    }

    @Override
    public void execute(Update update) {
        Bot.getInstance().getCommandManager().setActiveCommand(update.getMessage().getFrom().getId(), this);

        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText().trim();
        String[] parts = messageText.split(" ");
        String commandKey = parts[0];

        Optional<User> user = userRepository.getUserByUserId(chatId);

        if (user.isEmpty()) {
            Bot.getInstance().showBotAction(chatId, ActionType.TYPING);
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("Пользователь не найден. Пожалуйста, зарегистрируйтесь.");
            message.enableMarkdown(true);
            try {
                Bot.getInstance().execute(message);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }

            Bot.getInstance().getCommandManager().unsetActiveCommand(update.getMessage().getFrom().getId());
            return;
        }

        String userModel;
        if (parts.length > 1) {
            Bot.getInstance().showBotAction(chatId, ActionType.TYPING);
            String userInput = messageText.substring(commandKey.length()).trim();
            String userInputClear = userInput;
            userInput = userInput + "\n Ответь мне текстом в котором нету какого либо форматирования.";
            switch (user.get().getGptType()) {
                case YANDEX -> {
                    userModel = user.get().getYandexGptModel().getModel();

                    YandexGptService.AiResponse aiResponseObj = Bot.getInstance().getYandexGptService()
                            .getAiResponseWithTokens(userInput, userModel)
                            .block();

                    int tokens = aiResponseObj.getTotalTokens();

                    sendMessage(chatId, aiResponseObj.getResponse(), "Yandex", userModel, tokens, user.get().getTokens().intValue() - tokens);
                    user.get().setTokens(BigDecimal.valueOf(user.get().getTokens().intValue() - tokens));
                    userRepository.save(user.get());

                    responceService.saveResponce(
                            user.get().getUserId(),
                            userInputClear,
                            aiResponseObj.getResponse(),
                            aiResponseObj.getPromptTokens(),
                            aiResponseObj.getCompletionTokens(),
                            user.get().getGptType(),
                            user.get().getDeepseekGptModel(),
                            AIModels.YandexModels.valueOf(user.get().getYandexGptModel().toString())
                    );

                    Bot.getInstance().getCommandManager().unsetActiveCommand(update.getMessage().getFrom().getId());
                    return;
                }
                case DEEPSEEK -> {
                    userModel = user.get().getDeepseekGptModel().getModel();

                    DeepSeekService.AiResponse aiResponseObj = Bot.getInstance().getDeepSeekService()
                            .getAiResponseWithTokens(userInput, userModel)
                            .block();

                    int tokens = aiResponseObj.getTotalTokens();

                    sendMessage(chatId, aiResponseObj.getResponse(), "DeepSeek", userModel, tokens, user.get().getTokens().intValue() - tokens);
                    user.get().setTokens(BigDecimal.valueOf(user.get().getTokens().intValue() - tokens));
                    userRepository.save(user.get());

                    responceService.saveResponce(
                            user.get().getUserId(),
                            userInputClear,
                            aiResponseObj.getResponse(),
                            aiResponseObj.getPromptTokens(),
                            aiResponseObj.getCompletionTokens(),
                            user.get().getGptType(),
                            AIModels.DeepSeekModels.valueOf(user.get().getDeepseekGptModel().toString()),
                            user.get().getYandexGptModel()
                    );

                    Bot.getInstance().getCommandManager().unsetActiveCommand(update.getMessage().getFrom().getId());
                    return;
                }
                default -> {
                    SendMessage message = new SendMessage();
                    message.setChatId(String.valueOf(chatId));
                    message.setText("Неизвестный тип ИИ.");
                    message.enableMarkdown(true);
                    try {
                        Bot.getInstance().execute(message);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }

                    Bot.getInstance().getCommandManager().unsetActiveCommand(update.getMessage().getFrom().getId());
                    return;
                }
            }
        }

        Bot.getInstance().showBotAction(chatId, ActionType.TYPING);
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Введите ваш запрос после команды /ai.");
        message.enableMarkdown(true);
        try {
            Bot.getInstance().execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

        Bot.getInstance().getCommandManager().unsetActiveCommand(update.getMessage().getFrom().getId());
    }

    private void sendMessage(long chatId, String text, String ai, String model, int tokens, int currentTokens) {

        String response = """
                Ответ от %ai% - %model%
                
                ```
                %text%
                ```
                Было использовано %tokens% токена(ов). 
                Текущий баланс токенов составляет %current_tokens% токена(ов)
                """.replace("%ai%", ai)
                .replace("%model%", model)
                .replace("%text%", text)
                .replace("%tokens%", String.valueOf(tokens))
                .replace("%current_tokens%", String.valueOf(currentTokens));


        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(response);
        message.enableMarkdown(true);

        try {
            Bot.getInstance().execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
