package com.amazing.EverydayWiki.service;

import com.amazing.EverydayWiki.config.BotConfig;
import com.amazing.EverydayWiki.database.UserService;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    final BotConfig config;
    @Getter
    private final String botToken;
    @Getter
    private String languageCode;
    private final UserService userService;
    private final WikipediaService wikipediaService;
    private long chatID;

    public TelegramBot(BotConfig config, UserService userService, WikipediaService wikipediaService) {
        this.config = config;
        botToken = config.getToken();
        this.userService = userService;
        this.wikipediaService = wikipediaService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String messageText = message.getText();
            chatID = message.getChatId();
            String username = message.getFrom().getUserName();

            switch (messageText) {
                case "/start":
                    startCommandReceived(chatID);
                    languageCode = message.getFrom().getLanguageCode();

                    // подумать над автообновлением часового пояса в БД, так как пользователь может сменить локацию
                    userService.saveUser(chatID, username);
                    break;
                case "/article":
                    //DELETE
                    break;
                default:
                    //sendMessage(chatID, "I don't know this command yet.");
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData(); // Получаем значение кнопки
            long chatID = update.getCallbackQuery().getMessage().getChatId();
            // Обработка нажатия кнопки
            if (callbackData.equals("get_article")) {
                //String article = wikipediaService.getRandomArticle(languageCode);
                String article = wikipediaService.getTodaysFeaturedArticle("ru");
                sendArticleWithButton(chatID, article);
            }
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    private void startCommandReceived(long chatID) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("Get a random article");
        button.setCallbackData("get_article"); // Уникальный идентификатор кнопки
        row.add(button);

        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(chatID);
        message.setText("Welcome to Everyday Wiki\uD83C\uDF3B\n\nThis bot will send you a featured Wiki article every day at 9 AM." +
                "\n\nIf you want to get some random article right now, just tap the \"Get a random article\" button below.");
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(long chatID, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatID));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            //e.printStackTrace();
        }
    }

    public void sendArticleWithButton(long chatID, String text) {
        // Создаем сообщение с текстом статьи
        SendMessage message = new SendMessage();
        message.setChatId(chatID);
        message.setText(text);
        message.setParseMode(ParseMode.HTML);


        // Добавляем клавиатуру с кнопкой "Get a random article"
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();

        button.setText("Get next article");

        button.setCallbackData("get_article");
        row.add(button);

        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        // Отправляем сообщение
        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    Структура проекта:

    package config:
    класс BotConfig /
    класс BotInitializer

    package database:
    User / класс, описывающий пользователя
    UserRepository / интерфейс, используется в UserService. Подписан на JpaRepository
    UserService / класс для взаимодействия с БД

    package service:
    DailyFeaturedArticleScheduler / класс для отправки статьи дня всем пользователям
    TelegramBot / основной класс, инициализация бота
    WikipediaService / методы получения статей с вики

     */

}
