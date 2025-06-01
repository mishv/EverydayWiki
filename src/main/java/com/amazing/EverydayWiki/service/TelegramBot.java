package com.amazing.EverydayWiki.service;

import com.amazing.EverydayWiki.config.BotConfig;
import com.amazing.EverydayWiki.config.Language;
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

    private final BotConfig config;
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
        super(config.getToken());
        this.config = config;
        this.botToken = config.getToken();
        this.userService = userService;
        this.wikipediaService = wikipediaService;
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String messageText = message.getText();
            chatID = message.getChatId();
            String username = message.getFrom().getUserName();
            systemLanguage = update.getMessage().getFrom().getLanguageCode();

            switch (messageText) {
                case "/start", "/restart" -> startCommandReceived(chatID, username, systemLanguage);
                case "/language" -> updateArticlesLanguage(chatID);
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData(); // Получаем значение кнопки
            chatID = update.getCallbackQuery().getMessage().getChatId();

            // Обработка нажатия кнопки
            if (callbackData.equals("get_article")) {
                String article = wikipediaService.getRandomArticle(userService.getLanguage(chatID), userService.getSystemLanguage(chatID));
                sendArticleWithButton(chatID, article);
            } else if (callbackData.startsWith("lang_")) {
                articleLanguage = callbackData.split("_")[1];
                SendMessage message = new SendMessage();
                String messageText = switch (userService.getSystemLanguage(chatID)) {
                    case "ru" -> "Выбран язык статей: ";
                    case "be" -> "Мова артыкулаў: ";
                    default -> "Article language: ";
                };

                message.setText(messageText + articleLanguage + ".");
                sendMessageWithButton(chatID, message, setRandomArticleButton());
                userService.setLanguage(chatID, articleLanguage);
            }
        }
    }

    private void startCommandReceived(long chatID, String username, String systemLanguage) {
        createUser(chatID, username, systemLanguage);
        createMenu();
        SendMessage message = new SendMessage();

        switch (userService.getSystemLanguage(chatID)) {
            case "be":
                articleLanguage = Language.BELORUSSIAN;
                userService.setLanguage(chatID, articleLanguage);
                message.setText("Сардэчна запрашаем у \uD83C\uDF3B<b>Everyday Wiki!</b>\n\nКожны дзень бот будзе дасылаць вам адну абраную артыкул з Вікіпедыі." +
                        "\n\nХочаце пачытаць нешта прама цяпер?\nНацісніце на кнопку ніжэй.");
                break;
            case "ru":
                articleLanguage = Language.RUSSIAN;
                userService.setLanguage(chatID, articleLanguage);
                message.setText("Добро пожаловать в \uD83C\uDF3B<b>Everyday Wiki!</b>\n\nКаждый день бот будет отправлять вам одну избранную статью из Википедии." +
                        "\n\nХотите почитать что-то прямо сейчас?\nНажмите на кнопку ниже.");
                break;
            default:
                articleLanguage = Language.ENGLISH;
                userService.setLanguage(chatID, articleLanguage);
                message.setText("Welcome to \uD83C\uDF3B<b>Everyday Wiki!</b>\n\nThis bot will send you a featured Wiki article every day." +
                        "\n\nIf you want to get some random article right now, just tap the button below.");
        }

        sendMessageWithButton(chatID, message, setRandomArticleButton());
    }

    private InlineKeyboardMarkup setRandomArticleButton() {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setCallbackData("get_article");

        switch (userService.getSystemLanguage(chatID)) {
            case "ru" -> button.setText("Случайная статья");
            case "be" -> button.setText("Выпадковы артыкул");
            default -> button.setText("Get a random article");
        }

        return setInlineKeyboardMarkup(button);
    }

    private InlineKeyboardMarkup setNextArticleButton(Long chatID) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setCallbackData("get_article");

        switch (userService.getSystemLanguage(chatID)) {
            case "ru" -> button.setText("Следующая статья");
            case "be" -> button.setText("Наступная артыкул");
            default -> button.setText("Next article");
        }

        return setInlineKeyboardMarkup(button);
    }

    private InlineKeyboardMarkup setInlineKeyboardMarkup(InlineKeyboardButton button) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        row.add(button);

        return keyboardMarkup;
    }

    private void sendMessageWithButton(long chatID, SendMessage message, InlineKeyboardMarkup button) {
        message.setChatId(String.valueOf(chatID));
        message.setParseMode(ParseMode.HTML);
        message.setReplyMarkup(button);
        send(message);
    }

    public void sendArticleWithButton(long chatID, String text) {
        SendMessage message = new SendMessage();
        message.setText(text);
        sendMessageWithButton(chatID, message, setNextArticleButton(chatID));
    }

    public void createMenu() {
        //создаем меню
        List<BotCommand> listOfCommands = new ArrayList<>();

        switch (systemLanguage) {
            case "ru":
                listOfCommands.add(new BotCommand("/language", "Язык статей"));
                listOfCommands.add(new BotCommand("/restart", "Перезапустить бот"));
                break;
            case "be":
                listOfCommands.add(new BotCommand("/language", "Мова артыкулаў"));
                listOfCommands.add(new BotCommand("/restart", "Перазапусціць бота"));
                break;
            default:
                listOfCommands.add(new BotCommand("/language", "Set articles language"));
                listOfCommands.add(new BotCommand("/restart", "Restart bot"));
        }

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            //log.error("ERROR (setting bots command list): " + e);
        }
    }

    public void updateArticlesLanguage(Long chatID) {
        // Создаем сообщение с текстом статьи
        SendMessage message = new SendMessage();
        message.setChatId(chatID);

        switch (userService.getSystemLanguage(chatID)) {
            case "ru" -> message.setText("Выберите язык статей:");
            case "be" -> message.setText("Абярыце мову артыкулаў:");
            default -> message.setText("Choose article language:");
        }

        // Добавляем клавиатуру с кнопкой "Get a random article"
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        //List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton buttonRu = new InlineKeyboardButton();
        buttonRu.setText("Русский");
        buttonRu.setCallbackData("lang_Русский");
        keyboard.add(List.of(buttonRu));

        InlineKeyboardButton buttonBel = new InlineKeyboardButton();
        buttonBel.setText("Беларуская");
        buttonBel.setCallbackData("lang_Беларуская");
        keyboard.add(List.of(buttonBel));

        InlineKeyboardButton buttonEng = new InlineKeyboardButton();
        buttonEng.setText("English");
        buttonEng.setCallbackData("lang_English");
        keyboard.add(List.of(buttonEng));

        InlineKeyboardButton buttonSimpEng = new InlineKeyboardButton();
        buttonSimpEng.setText("Simple English");
        buttonSimpEng.setCallbackData("lang_Simple English");
        keyboard.add(List.of(buttonSimpEng));

        //keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        // Отправляем сообщение
        send(message);
    }

    public void send(SendMessage message) {
        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createUser(long chatID, String username, String systemLanguage) {
        if (userService.getUserByChatId(chatID).isEmpty()) {
            userService.saveUser(chatID, username, systemLanguage);
        } else {
            userService.setSystemLanguage(chatID, systemLanguage);
            userService.setLanguage(chatID, systemLanguage);
        }
    }
}
