package org.marensovich.bot.bot;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.marensovich.bot.bot.AI.GPT.DeepSeekService;
import org.marensovich.bot.bot.AI.GPT.YandexGptService;
import org.marensovich.bot.bot.AI.Vision.YandexVisionService;
import org.marensovich.bot.bot.AI.Voice.YandexVoiceService;
import org.marensovich.bot.bot.Callback.CallbackManager;
import org.marensovich.bot.bot.Command.CommandManager;
import org.marensovich.bot.bot.Yandex.Storage.YandexStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;

@Slf4j
public class Bot extends TelegramLongPollingBot {

    @Autowired @Getter private CommandManager commandManager;
    @Autowired @Getter private CallbackManager callbackManager;
    @Getter private static Bot instance;

    private final String botToken;
    private final String botUsername;

    @Autowired private DeepSeekService deepSeekService;
    @Autowired private YandexGptService yandexGptService;
    @Autowired private YandexVisionService visionService;
    @Autowired private YandexVoiceService voiceService;
    @Autowired private YandexStorageService storageService;

    public Bot(String botToken, String botUsername) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        instance = this;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasVoice()) {
            saveAudioFile(update.getMessage());
            //handleVoiceMessage(update.getMessage());
        }
        if (update.hasMessage() && update.getMessage().hasPhoto()) {
            handlePhotoMessage(update);
            return;
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            if (update.getMessage().getText().startsWith("/")) {
                commandManager.executeCommand(update);
            } else if (update.getMessage().getText().startsWith("/deepseek")) {
                handleDeepSeekCommand(update);
            } else if (update.getMessage().getText().startsWith("/yandex")) {
                handleYandexCommand(update);
            }
        }
        if (update.hasCallbackQuery()) {
            callbackManager.handleCallback(update);
        }
    }

    private void saveAudioFile(Message message){
        Long chatId = message.getChatId();
        Voice voice = message.getVoice();

        try {
            // Получаем файл голосового сообщения
            GetFile getFile = new GetFile(voice.getFileId());
            File file = execute(getFile);
            String fileUrl = "https://api.telegram.org/file/bot" + getBotToken() + "/" + file.getFilePath();

            // Создаем папку resources/voice_messages если ее нет
            Path voiceMessagesDir = Paths.get("Bot/src/main/resources/voice_messages");
            if (!Files.exists(voiceMessagesDir)) {
                Files.createDirectories(voiceMessagesDir);
            }

            // Формируем имя файла
            String fileName = "voice_" + System.currentTimeMillis() + ".ogg";
            Path filePath = voiceMessagesDir.resolve(fileName);

            // Скачиваем и сохраняем файл
            try (InputStream in = new URL(fileUrl).openStream()) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
                log.info("Voice message saved to: {}", filePath);
                sendTextMessage(chatId, "✅ Голосовое сообщение сохранено для отладки");
            }

            try (InputStream stream = new FileInputStream("Bot/src/main/resources/voice_messages/" + fileName)) {
                storageService.uploadVoiceMessage(
                        "botvoicedata",
                        "voicedata/" + fileName,
                        stream);
            }

        } catch (Exception e) {
            log.error("Error saving voice message", e);
            sendTextMessage(chatId, "❌ Ошибка при сохранении голосового сообщения");
        }
    }

    private void handleVoiceMessage(Message message) {
        Long chatId = message.getChatId();
        Voice voice = message.getVoice();

        // Проверка длительности аудио (не более 30 секунд)
        if (voice.getDuration() > 30) {
            sendTextMessage(chatId, "⚠️ Аудио слишком длинное (максимум 30 секунд)");
            return;
        }

        sendTextMessage(chatId, "🔊 Обрабатываю голосовое сообщение...");

        getVoiceFile(voice.getFileId())
                .flatMap(audioData -> {
                    // Проверка размера файла (не более 1 МБ)
                    if (audioData.length > 1_000_000) {
                        return Mono.just("⚠️ Аудио файл слишком большой (максимум 1 МБ)");
                    }
                    return voiceService.recognizeSpeech(audioData);
                })
                .subscribe(
                        text -> sendTextMessage(chatId, "🔊 Распознанный текст:\n\n" + text),
                        error -> sendTextMessage(chatId, "❌ Ошибка распознавания: " + error.getMessage())
                );
    }

    private Mono<byte[]> getVoiceFile(String fileId) {
        return Mono.fromCallable(() -> {
            GetFile getFile = new GetFile(fileId);
            String filePath = execute(getFile).getFilePath();
            String fileUrl = "https://api.telegram.org/file/bot" + getBotToken() + "/" + filePath;

            try (InputStream in = new URL(fileUrl).openStream()) {
                return in.readAllBytes();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private void handleDeepSeekCommand(Update update) {
        String userText = update.getMessage().getText().replace("/deepseek", "").trim();
        try {
            String aiResponse = deepSeekService.getAiResponse(userText).block();
            sendTextMessage(update.getMessage().getChatId(), "Ответ от ИИ: " + aiResponse);
        } catch (Exception e) {
            sendTextMessage(update.getMessage().getChatId(), "Ошибка: " + e.getMessage());
        }
    }

    private void handleYandexCommand(Update update) {
        String userText = update.getMessage().getText().replace("/yandex", "").trim();
        try {
            String aiResponse = yandexGptService.getAiResponse(userText).block();
            SendMessage message = new SendMessage();
            message.setChatId(update.getMessage().getChatId().toString());
            message.setText(aiResponse);
            message.enableMarkdown(true);
            execute(message);
        } catch (Exception e) {
            sendTextMessage(update.getMessage().getChatId(), "Ошибка: " + e.getMessage());
        }
    }

    private void handlePhotoMessage(Update update) {
        List<PhotoSize> photos = update.getMessage().getPhoto();
        PhotoSize photo = photos.stream()
                .max(Comparator.comparing(PhotoSize::getFileSize))
                .orElse(null);

        if (photo != null) {
            processPhoto(update.getMessage().getChatId(), photo);
        } else {
            sendTextMessage(update.getMessage().getChatId(), "Не удалось получить фото для обработки.");
        }
    }

    private void processPhoto(Long chatId, PhotoSize photo) {
        try {
            String fileId = photo.getFileId();
            String filePath = execute(new GetFile(fileId)).getFilePath();
            String fileUrl = "https://api.telegram.org/file/bot" + getBotToken() + "/" + filePath;
            String mimeType = filePath.toLowerCase().endsWith(".png") ? "PNG" : "JPEG";

            byte[] imageData;
            try (InputStream in = new URL(fileUrl).openStream()) {
                imageData = in.readAllBytes();
                if (imageData.length > 10 * 1024 * 1024) {
                    sendTextMessage(chatId, "⚠️ Изображение слишком большое (макс. 10MB)");
                    return;
                }
            }

            sendTextMessage(chatId, "🔍 Обрабатываю изображение...");

            visionService.recognizeTextFromImage(imageData, mimeType)
                    .subscribe(
                            text -> sendTextMessage(chatId, text.isBlank() ?
                                    "Не удалось распознать текст" : "✅ Распознанный текст:\n\n" + text),
                            error -> sendTextMessage(chatId, "❌ Ошибка: " + getErrorMessage(error))
                    );
        } catch (Exception e) {
            sendTextMessage(chatId, "⛔ Ошибка обработки: " + e.getMessage());
        }
    }

    private String getErrorMessage(Throwable error) {
        String msg = error.getMessage();
        return msg.contains("400") ?
                "Некорректный запрос. Проверьте формат изображения (JPEG/PNG)" : msg;
    }

    private void sendTextMessage(Long chatId, String text) {
        try {
            execute(new SendMessage(chatId.toString(), text));
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки сообщения: " + e.getMessage());
        }
    }

    public void sendNoAccessMessage(Update update) {
        sendTextMessage(update.getMessage().getChatId(), "⛔ У вас нет прав для выполнения этой команды!");
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}