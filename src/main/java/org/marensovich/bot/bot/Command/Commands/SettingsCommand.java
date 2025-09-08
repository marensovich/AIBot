package org.marensovich.bot.bot.Command.Commands;

import lombok.RequiredArgsConstructor;
import org.marensovich.bot.bot.AI.GPT.Data.AIModels;
import org.marensovich.bot.bot.AI.GPT.Data.TemperatureParameter;
import org.marensovich.bot.bot.Bot;
import org.marensovich.bot.db.models.User;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
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
    public static final String CALLBACK_SEPARATOR = "settings_separator";
    public static final String CALLBACK_HEADER = "settings_header";

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
            AIModels activeModel = getActiveModelForUser(user.get());

            TemperatureParameter userTempValue = user.get().getModelTemperature();

            InlineKeyboardMarkup keyboard = buildInlineKeyboard(activeModel, userTempValue, user.get());

            sendMessageWithInlineKeyboard(update.getMessage().getChatId(), "Выберите модель ИИ и его температуру:", keyboard);
        } else {
            sendMessageWithInlineKeyboard(update.getMessage().getChatId(), "User not found.", null);
        }

        Bot.getInstance().getCommandManager().unsetActiveCommand(update.getMessage().getFrom().getId());
    }

    private InlineKeyboardMarkup buildInlineKeyboard(AIModels activeModel, TemperatureParameter activeTemperature, User user) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(Collections.singletonList(
                InlineKeyboardButton.builder()
                        .text("🤖 МОДЕЛИ")
                        .callbackData(CALLBACK_HEADER)
                        .build()
        ));

        List<InlineKeyboardButton> currentModelRow = new ArrayList<>();
        for (AIModels.YandexModels model : AIModels.YandexModels.values()) {
            boolean isActive = activeModel == AIModels.YANDEX &&
                    user.getYandexGptModel() == model;
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

        rows.add(Collections.singletonList(
                InlineKeyboardButton.builder()
                        .text("──────────────")
                        .callbackData(CALLBACK_SEPARATOR)
                        .build()
        ));

        rows.add(Collections.singletonList(
                InlineKeyboardButton.builder()
                        .text("🌡️ ТЕМПЕРАТУРЫ")
                        .callbackData(CALLBACK_HEADER)
                        .build()
        ));

        List<InlineKeyboardButton> currentTempRow = new ArrayList<>();
        for (TemperatureParameter temp : TemperatureParameter.values()) {
            boolean isActive = temp == activeTemperature;
            String buttonText = temp.name() + " (" + temp.getTemperature() + ")" + (isActive ? " ✅" : "");

            InlineKeyboardButton button = InlineKeyboardButton.builder()
                    .text(buttonText)
                    .callbackData(CALLBACK_TEMP_PREFIX + temp.name())
                    .build();

            if (currentTempRow.size() >= 2) {
                rows.add(new ArrayList<>(currentTempRow));
                currentTempRow.clear();
            }

            currentTempRow.add(button);
        }
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
        message.setReplyMarkup(keyboard);

        try {
            Bot.getInstance().execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void updateMessageWithInlineKeyboard(Long chatId, Integer messageId, String text, InlineKeyboardMarkup keyboard) {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(String.valueOf(chatId));
        editMessage.setMessageId(messageId);
        editMessage.setText(text);
        editMessage.setReplyMarkup(keyboard);

        try {
            Bot.getInstance().execute(editMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void handleCallbackQuery(Update update) {
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            String[] parts = callbackData.split(":");
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();

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
                        throw new IllegalArgumentException("Неизвестная модель ИИ: " + modelString);
                    }
                }

                userRepository.save(user);
                AIModels activeModel = getActiveModelForUser(user);
                TemperatureParameter userTempValue = user.getModelTemperature();
                InlineKeyboardMarkup updatedKeyboard = buildInlineKeyboard(activeModel, userTempValue, user);

                updateMessageWithInlineKeyboard(chatId, messageId, "Выберите модель ИИ и его температуру:", updatedKeyboard);


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

                    AIModels activeModel = getActiveModelForUser(user);
                    TemperatureParameter userTempValue = user.getModelTemperature();
                    InlineKeyboardMarkup updatedKeyboard = buildInlineKeyboard(activeModel, userTempValue, user);

                    updateMessageWithInlineKeyboard(chatId, messageId, "Выберите модель ИИ и его температуру:", updatedKeyboard);


                } else {
                    throw new IllegalArgumentException("Unknown model temp string: " + modelTemp);
                }
            }
        }
    }
}