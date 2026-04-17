package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class FinGuardBot extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {
        return "FinParent";
    }

    @Override
    public String getBotToken() {
        return "8591173717:AAEeEF42bAacwHGW7v2HLvVzjowqhoWJtOk";
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Проверяем, что пришло текстовое сообщение
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            // Простейшее эхо для проверки связи
            sendText(chatId, "Ты написал: " + messageText + ". Скоро я научусь считать твои деньги!");
        }
    }

    // Вспомогательный метод для отправки сообщений
    public void sendText(Long who, String what) {
        SendMessage sm = SendMessage.builder()
                .chatId(who.toString())
                .text(what).build();
        try {
            execute(sm);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}