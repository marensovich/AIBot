package org.marensovich.bot.bot.Command.Commands;

import org.marensovich.bot.bot.Bot;
import org.marensovich.bot.bot.Command.Interfaces.Command;
import org.marensovich.bot.db.models.User;
import org.marensovich.bot.db.repositories.UserRepository;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import java.util.Optional;

@Component
public class AiCommand implements Command {

    private final UserRepository userRepository;

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

        Optional<User> userByUserId = userRepository.getUserByUserId(chatId);

        if (userByUserId.isEmpty()) {
            sendMessage(chatId, "Пользователь не найден. Пожалуйста, зарегистрируйтесь.");
            return;
        }

        User user = userByUserId.get();
        String userModel;

        if (parts.length > 1) {
            String userInput = messageText.substring(commandKey.length()).trim();

            switch (user.getGptType()) {
                case YANDEX -> {
                    userModel = user.getYandexGptModel().getModel();
                    String aiResponse = Bot.getInstance().getYandexGptService().getAiResponse(userInput, userModel).block();

                    sendMessage(chatId, aiResponse);
                    return;
                }
                case DEEPSEEK -> {
                    userModel = user.getDeepseekGptModel().getModel();
                    String aiResponse = Bot.getInstance().getDeepSeekService().getAiResponse(userInput, userModel).block();

                    sendMessage(chatId, aiResponse);
                    return;
                }
                default -> {
                    sendMessage(chatId, "Неизвестный тип ИИ.");
                    return;
                }
            }
        }

        sendMessage(chatId, "Введите ваш запрос после команды /ai.");
        Bot.getInstance().getCommandManager().unsetActiveCommand(update.getMessage().getFrom().getId());
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.enableMarkdown(true);

        try {
            Bot.getInstance().execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
