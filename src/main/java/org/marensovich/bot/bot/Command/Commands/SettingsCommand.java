package org.marensovich.bot.bot.Command.Commands;

import org.marensovich.bot.bot.AI.GPT.Data.AIModels;
import org.marensovich.bot.bot.Bot;
import org.marensovich.bot.db.models.User;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import org.marensovich.bot.bot.Command.Interfaces.Command;
import org.marensovich.bot.db.repositories.UserRepository;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class SettingsCommand implements Command {

    public static final String CALLBACK_PREFIX = "set_model:";

    private final UserRepository userRepository;

    public SettingsCommand(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public String getName() {
        return "/settings";
    }

    @Override
    public void execute(Update update) {
        Bot.getInstance().getCommandManager().setActiveCommand(update.getMessage().getFrom().getId(), this);

        Optional<User> user = userRepository.getUserByUserId(update.getMessage().getFrom().getId());

        if (user.isPresent()) {
            // Get the active model for the user
            AIModels activeModel = getActiveModelForUser(user.get());

            // Build the inline keyboard
            InlineKeyboardMarkup keyboard = buildInlineKeyboard(activeModel);

            // Send the keyboard to the user
            sendMessageWithInlineKeyboard(update.getMessage().getChatId(), "Select an AI Model:", keyboard);
        } else {
            // Handle case where user is not found
            sendMessageWithInlineKeyboard(update.getMessage().getChatId(), "User not found.", null);
        }

        Bot.getInstance().getCommandManager().unsetActiveCommand(update.getMessage().getFrom().getId());
    }

    private InlineKeyboardMarkup buildInlineKeyboard(AIModels activeModel) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Add Yandex models
        List<InlineKeyboardButton> yandexRow = new ArrayList<>();
        for (AIModels.YandexModels model : AIModels.YandexModels.values()) {
            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(model.getModel() + (activeModel == AIModels.YANDEX && model.getModel().equals(activeModel.name()) ? " ✔" : ""))
                    .callbackData("set_model:" + model.getModel())
                    .build();
            yandexRow.add(button);
        }
        rows.add(yandexRow);

        // Add DeepSeek models
        List<InlineKeyboardButton> deepSeekRow = new ArrayList<>();
        for (AIModels.DeepSeekModels model : AIModels.DeepSeekModels.values()) {
            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(model.getModel() + (activeModel == AIModels.DEEPSEEK && model.getModel().equals(activeModel.name()) ? " ✔" : ""))
                    .callbackData("set_model:" + model.getModel())
                    .build();
            deepSeekRow.add(button);
        }
        rows.add(deepSeekRow);

        keyboardMarkup.setKeyboard(rows);
        return keyboardMarkup;
    }

    private AIModels getActiveModelForUser(User user) {
        AIModels gptType = AIModels.valueOf(user.getGptType().name());
        return switch (gptType) {
            case DEEPSEEK -> AIModels.DEEPSEEK; // Return the DeepSeek model
            case YANDEX -> AIModels.YANDEX; // Return the Yandex model
            default -> null; // Handle unknown types
        };
    }

    private void sendMessageWithInlineKeyboard(Long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(keyboard); // Set the inline keyboard markup

        try {
            Bot.getInstance().execute(message); // Send the message
        } catch (TelegramApiException e) {
            e.printStackTrace(); // Handle the exception (you may want to log this)
        }
    }

    // Method to handle callback queries (to save the selected model)
    public void handleCallbackQuery(Update update) {
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();


        }
    }
}