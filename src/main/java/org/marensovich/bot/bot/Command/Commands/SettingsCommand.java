package org.marensovich.bot.bot.Command.Commands;

import lombok.RequiredArgsConstructor;
import org.marensovich.bot.bot.AI.GPT.Data.AIModels;
import org.marensovich.bot.bot.AI.GPT.Data.TemperatureParameter;
import org.marensovich.bot.bot.Bot;
import org.marensovich.bot.bot.Callback.Callbacks.Settings.SettingsModelTemperatureHandler;
import org.marensovich.bot.db.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import org.marensovich.bot.bot.Command.Interfaces.Command;
import org.marensovich.bot.db.repositories.UserRepository;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.*;

@Component
@RequiredArgsConstructor
public class SettingsCommand implements Command {

    public static final String CALLBACK_MODEL_PREFIX = "set_model:";

    public static final String CALLBACK_TEMP_PREFIX = "set_temperature:";

    private final UserRepository userRepository;

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
            TemperatureParameter userTempValue = user.get().getModelTemperature();

            InlineKeyboardMarkup keyboard = buildInlineKeyboard(activeModel, userTempValue, user.get());

            // Send the keyboard to the user
            sendMessageWithInlineKeyboard(update.getMessage().getChatId(), "Select an AI Model:", keyboard);
        } else {
            // Handle case where user is not found
            sendMessageWithInlineKeyboard(update.getMessage().getChatId(), "User not found.", null);
        }

        Bot.getInstance().getCommandManager().unsetActiveCommand(update.getMessage().getFrom().getId());
    }

    private InlineKeyboardMarkup buildInlineKeyboard(AIModels activeModel, TemperatureParameter activeTemperature, User user) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Заголовок для моделей
        rows.add(Collections.singletonList(
                InlineKeyboardButton.builder()
                        .text("🤖 МОДЕЛИ")
                        .callbackData("header_models")
                        .build()
        ));

        // Модели Yandex - автоматическое распределение по строкам
        List<InlineKeyboardButton> currentModelRow = new ArrayList<>();
        for (AIModels.YandexModels model : AIModels.YandexModels.values()) {
            boolean isActive = activeModel == AIModels.YANDEX &&
                    user.getYandexGptModel() == model;
            String buttonText = model.getModel() + (isActive ? " ✅" : "");

            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(buttonText)
                    .callbackData(CALLBACK_MODEL_PREFIX + model.getModel())
                    .build();

            // Если текст длинный или в текущей строке уже есть кнопка - начинаем новую строку
            if (buttonText.length() > 8 && !currentModelRow.isEmpty()) {
                rows.add(new ArrayList<>(currentModelRow));
                currentModelRow.clear();
            }

            currentModelRow.add(button);

            // Если текст короткий и в строке уже 2 кнопки - начинаем новую строку
            if (buttonText.length() <= 8 && currentModelRow.size() >= 2) {
                rows.add(new ArrayList<>(currentModelRow));
                currentModelRow.clear();
            }
        }
        // Добавляем оставшиеся кнопки
        if (!currentModelRow.isEmpty()) {
            rows.add(currentModelRow);
        }

        // Модели DeepSeek - автоматическое распределение по строкам
        currentModelRow.clear();
        for (AIModels.DeepSeekModels model : AIModels.DeepSeekModels.values()) {
            boolean isActive = activeModel == AIModels.DEEPSEEK &&
                    user.getDeepseekGptModel() == model;
            String buttonText = model.getModel() + (isActive ? " ✅" : "");

            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(buttonText)
                    .callbackData(CALLBACK_MODEL_PREFIX + model.getModel())
                    .build();

            if (buttonText.length() > 8 && !currentModelRow.isEmpty()) {
                rows.add(new ArrayList<>(currentModelRow));
                currentModelRow.clear();
            }

            currentModelRow.add(button);

            if (buttonText.length() <= 8 && currentModelRow.size() >= 2) {
                rows.add(new ArrayList<>(currentModelRow));
                currentModelRow.clear();
            }
        }
        if (!currentModelRow.isEmpty()) {
            rows.add(currentModelRow);
        }

        // Разделитель
        rows.add(Collections.singletonList(
                InlineKeyboardButton.builder()
                        .text("──────────────")
                        .callbackData("separator")
                        .build()
        ));

        // Заголовок для температур
        rows.add(Collections.singletonList(
                InlineKeyboardButton.builder()
                        .text("🌡️ ТЕМПЕРАТУРЫ")
                        .callbackData("header_temperature")
                        .build()
        ));

        // Температуры - автоматическое распределение по строкам
        List<InlineKeyboardButton> currentTempRow = new ArrayList<>();
        for (TemperatureParameter temp : TemperatureParameter.values()) {
            boolean isActive = temp == activeTemperature;
            String buttonText = temp.name() + " (" + temp.getTemperature() + ")" + (isActive ? " ✅" : "");

            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(buttonText)
                    .callbackData(CALLBACK_TEMP_PREFIX + temp.name())
                    .build();

            // Для температур всегда максимум 2 кнопки в строке из-за длинных названий
            if (currentTempRow.size() >= 2) {
                rows.add(new ArrayList<>(currentTempRow));
                currentTempRow.clear();
            }

            currentTempRow.add(button);
        }
        // Добавляем оставшиеся температурные кнопки
        if (!currentTempRow.isEmpty()) {
            rows.add(currentTempRow);
        }

        keyboardMarkup.setKeyboard(rows);
        return keyboardMarkup;
    }

    private AIModels getActiveModelForUser(User user) {
        AIModels gptType = AIModels.valueOf(user.getGptType().name());
        return switch (gptType) {
            case DEEPSEEK -> AIModels.DEEPSEEK;
            case YANDEX -> AIModels.YANDEX;
            default -> null;
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
            String[] parts = callbackData.split(":");

            if (callbackData.startsWith(CALLBACK_MODEL_PREFIX)){
                String modelString = parts[1];


                User user = userRepository.findUserByUserId(update.getCallbackQuery().getFrom().getId());

                Optional<AIModels.YandexModels> yandexModelOpt = Arrays.stream(AIModels.YandexModels.values())
                        .filter(model -> model.getModel().equals(modelString))
                        .findFirst();

                Optional<AIModels.DeepSeekModels> deepseekModelOpt = Arrays.stream(AIModels.DeepSeekModels.values())
                        .filter(model -> model.getModel().equals(modelString))
                        .findFirst();

                if (yandexModelOpt.isPresent()) {
                    user.setGptType(AIModels.YANDEX);
                    user.setYandexGptModel(yandexModelOpt.get());
                    user.setDeepseekGptModel(AIModels.DeepSeekModels.DeepSeek_V3);
                }
                else {

                    if (deepseekModelOpt.isPresent()) {
                        user.setGptType(AIModels.DEEPSEEK);
                        user.setDeepseekGptModel(deepseekModelOpt.get());
                        user.setYandexGptModel(AIModels.YandexModels.YandexLite);
                    } else {
                        throw new IllegalArgumentException("Unknown model string: " + modelString);
                    }
                }

                userRepository.save(user);

                SendMessage message = new SendMessage();
                message.setChatId(update.getCallbackQuery().getFrom().getId());
                message.setText("Модель <b>" + modelString + "</b> была успешно выбрана");
                message.enableHtml(true);

                try {
                    Bot.getInstance().execute(message);
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else if (callbackData.startsWith(CALLBACK_TEMP_PREFIX)) {
                String modelTemp = parts[1];

                User user = userRepository.findUserByUserId(update.getCallbackQuery().getFrom().getId());

                float temperatureValue = Float.parseFloat(String.valueOf(TemperatureParameter.valueOf(modelTemp).getTemperature()));
                Optional<TemperatureParameter> modelTempOpt = Arrays.stream(TemperatureParameter.values())
                        .filter(temperatureParameter -> temperatureParameter.getTemperature() == temperatureValue)
                        .findFirst();

                if (modelTempOpt.isPresent()) {
                    user.setModelTemperature(TemperatureParameter.valueOf(modelTemp));

                    userRepository.save(user);

                    SendMessage message = new SendMessage();
                    message.setChatId(update.getCallbackQuery().getFrom().getId());
                    message.setText("Температура <b>" + modelTemp + "(" + TemperatureParameter.valueOf(modelTemp).getTemperature() + ")" + "</b> была успешно выбрана");
                    message.enableHtml(true);

                    try {
                        Bot.getInstance().execute(message);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }

                } else {
                    throw new IllegalArgumentException("Unknown model temp string: " + modelTemp);
                }

            }

        }
    }
}