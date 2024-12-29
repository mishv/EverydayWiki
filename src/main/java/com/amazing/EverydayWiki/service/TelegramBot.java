package com.amazing.EverydayWiki.service;

import com.amazing.EverydayWiki.config.BotConfig;
import com.amazing.EverydayWiki.config.Language;
import com.amazing.EverydayWiki.database.User;
import com.amazing.EverydayWiki.database.UserService;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
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
    private String systemLanguage;
    @Getter
    private String articleLanguage;
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

            if (userService.getSystemLanguage(chatID) == null || systemLanguage == null) {
                systemLanguage = update.getMessage().getFrom().getLanguageCode();
                userService.setSystemLanguage(chatID, systemLanguage);
            }

            switch (messageText) {
                case "/start":
                    startCommandReceived(chatID);
                    createMenu();

                    if (systemLanguage.equals("ru")) {
                        articleLanguage = Language.RUSSIAN.getDisplayName();
                        userService.setLanguage(chatID, articleLanguage);
                    } else {
                        articleLanguage = Language.ENGLISH.getDisplayName();
                        userService.setLanguage(chatID, articleLanguage);
                    }
                    // подумать над автообновлением часового пояса в БД, так как пользователь может сменить локацию
                    userService.saveUser(chatID, username, systemLanguage);
                    break;
                case "/language":
                    updateArticlesLanguage(chatID);
                    break;
                case "/bot":
                    //
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
                String article = wikipediaService.getTodaysFeaturedArticle(userService.getLanguage(chatID));
                sendArticleWithButton(chatID, article);
            } else if (callbackData.startsWith("lang_")) {
                articleLanguage = callbackData.split("_")[1];
                sendMessageWithButton(chatID, "Выбран язык статей: " + articleLanguage);
                userService.setLanguage(chatID, articleLanguage);
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
        button.setCallbackData("get_article");

        SendMessage message = new SendMessage();
        message.setChatId(chatID);
        message.setReplyMarkup(keyboardMarkup);

        if (userService.getSystemLanguage(chatID).equals("ru")) {
            message.setText("Добро пожаловать в Everyday Wiki\uD83C\uDF3B\n\nКаждый день бот будет отправлять вам одну избранную статью из Википедии." +
                    "\n\nХотите почитать что-то прямо сейчас?\nНажмите на кнопку \"Случайная статья\" ниже.");
            button.setText("Случайная статья");
        } else {
            message.setText("Welcome to Everyday Wiki\uD83C\uDF3B\n\nThis bot will send you a featured Wiki article every day." +
                    "\n\nIf you want to get some random article right now, just tap the \"Get a random article\" button below.");
            button.setText("Get a random article");
        }

        row.add(button);
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessageWithButton(long chatID, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatID));
        message.setText(textToSend);

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();
        if (userService.getSystemLanguage(chatID).equals("ru")) {
            button.setText("Случайная статья");
        } else {
            button.setText("Get a random article");
        }

        button.setCallbackData("get_article"); // Уникальный идентификатор кнопки
        row.add(button);

        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

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

        if (userService.getSystemLanguage(chatID).equals("ru")) {
            button.setText("Следующая статья");
        } else {
            button.setText("Get next article");
        }

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

    public void createMenu() {
        //создаем меню
        List<BotCommand> listofCommands = new ArrayList<>();
        if (systemLanguage.equals("ru")) {
            listofCommands.add(new BotCommand("/about", "Информация о боте"));
            listofCommands.add(new BotCommand("/language", "Язык статей"));
        } else {
            listofCommands.add(new BotCommand("/about", "Bot info"));
            listofCommands.add(new BotCommand("/language", "Set articles language"));
        }


        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            //log.error("ERROR (setting bot's command list): " + e);
        }
    }

    public void updateArticlesLanguage(Long chatID) {
        // Создаем сообщение с текстом статьи
        SendMessage message = new SendMessage();
        message.setChatId(chatID);

        if (systemLanguage.equals("ru")) {
            message.setText("Выберите язык статей:");
        } else {
            message.setText("Select the language of the articles:");
        }


        // Добавляем клавиатуру с кнопкой "Get a random article"
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        //List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton buttonRu = new InlineKeyboardButton();
        buttonRu.setText("Русский");
        buttonRu.setCallbackData("lang_Русский");
        keyboard.add(List.of(buttonRu));

        InlineKeyboardButton buttonEng = new InlineKeyboardButton();
        buttonEng.setText("English");
        buttonEng.setCallbackData("lang_English");
        keyboard.add(List.of(buttonEng));

        InlineKeyboardButton buttonSimpEng = new InlineKeyboardButton();
        buttonSimpEng.setText("Simple English");
        buttonSimpEng.setCallbackData("lang_Simple English");
        keyboard.add(List.of(buttonSimpEng));

        InlineKeyboardButton buttonBel = new InlineKeyboardButton();
        buttonBel.setText("Беларуская");
        buttonBel.setCallbackData("lang_Беларуская");
        keyboard.add(List.of(buttonBel));

        //keyboard.add(row);
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
