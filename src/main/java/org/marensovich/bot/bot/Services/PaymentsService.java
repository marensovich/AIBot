package org.marensovich.bot.bot.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.marensovich.bot.bot.Bot;
import org.marensovich.bot.bot.Data.PaymentsStatus;
import org.marensovich.bot.bot.Database.Models.Payments;
import org.marensovich.bot.bot.Database.Models.User;
import org.marensovich.bot.bot.Database.Repositories.PaymentsRepository;
import org.marensovich.bot.bot.Database.Repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.payments.SuccessfulPayment;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentsService {

    private final PaymentsRepository paymentRepository;
    private final UserRepository userRepository;
    private final Bot bot;

    // Курс конвертации: 1 звезда = 1 рубль
    private static final BigDecimal STARS_TO_RUBLES_RATE = BigDecimal.ONE;

    public void handleStarsPayment(SuccessfulPayment successfulPayment, org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        // Конвертация звезд в рубли (1 звезда = 1 рубль)
        BigDecimal amountInRubles = new BigDecimal(successfulPayment.getTotalAmount())
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);


        // Получаем пользователя из базы
        Optional<User> userOptional = userRepository.getUserByUserId(telegramUser.getId());

        if (userOptional.isEmpty()) {
            log.error("User not found for payment: {}", telegramUser.getId());
            return;
        }

        User user = userOptional.get();

        // Создаем запись о платеже
        Payments payment = new Payments();
        payment.setUserId(user.getUserId());
        payment.setTelegramPaymentId(successfulPayment.getTelegramPaymentChargeId());
        payment.setAmount(amountInRubles);
        payment.setCurrency("RUB"); // Указываем рубли
        payment.setPayload(successfulPayment.getInvoicePayload());
        payment.setStatus(PaymentsStatus.COMPLETED);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setCompletedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        // Обновление баланса пользователя
        user.setBalance(user.getBalance().add(amountInRubles));
        userRepository.save(user);

        // Отправка подтверждения пользователю
        sendPaymentConfirmation(telegramUser, amountInRubles, user.getBalance());
    }

    private void sendPaymentConfirmation(org.telegram.telegrambots.meta.api.objects.User telegramUser,
                                         BigDecimal amount, BigDecimal newBalance) {
        String message = String.format(
                "✅ Ваш баланс пополнен на %.2f ₽!\n\n" +
                        "💎 Новый баланс: %.2f ₽\n" +
                        "🕐 Дата: %s",
                amount,
                newBalance,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        );

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(telegramUser.getId().toString());
        sendMessage.setText(message);

        try {
            bot.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error sending payment confirmation", e);
        }
    }

    public BigDecimal getCurrentBalance(Long userId) {
        return userRepository.getUserByUserId(userId)
                .map(User::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    // Метод для конвертации звезд в рубли (если нужен в других местах)
    public BigDecimal convertStarsToRubles(int stars) {
        return new BigDecimal(stars)
                .multiply(STARS_TO_RUBLES_RATE)
                .setScale(2, RoundingMode.HALF_UP);
    }

}