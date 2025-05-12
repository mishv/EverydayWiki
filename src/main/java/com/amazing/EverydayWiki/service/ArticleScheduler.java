package com.amazing.EverydayWiki.service;

import com.amazing.EverydayWiki.database.User;
import com.amazing.EverydayWiki.database.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Component
public class ArticleScheduler {
    private final UserService userService;
    private final WikipediaService wikipediaService;
    private final TelegramBot telegramBot;

    @Autowired
    public ArticleScheduler(UserService userService,
                            WikipediaService wikipediaService,
                            TelegramBot telegramBot) {
        this.userService = userService;
        this.wikipediaService = wikipediaService;
        this.telegramBot = telegramBot;
    }

    @Scheduled(cron = "0 0 06 * * *", zone = "UTC") // 9 AM по UTC
    //@Scheduled(cron = "0 0 * * * *", zone = "UTC") // каждый час
    public void sendDailyArticleToAllUsers() {
        List<User> users = userService.getAllUsers();
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        DayOfWeek dayOfWeek = today.getDayOfWeek();

        for (User user : users) {
            String language = user.getLanguage();
            String systemLanguage = user.getSystemLanguage();
            String article;

            if (language.equals("en")) {
                // Английская вики — статья дня каждый день
                article = wikipediaService.getTodaysFeaturedArticle(language, systemLanguage);
            } else {
                if (dayOfWeek == DayOfWeek.MONDAY) {
                    // Для других языков — статья дня только по понедельникам
                    article = wikipediaService.getTodaysFeaturedArticle(language, systemLanguage);
                } else {
                    // В остальные дни — случайная избранная
                    article = wikipediaService.getRandomFeaturedArticle(language, systemLanguage);
                    DateTimeFormatter formatter;
                    String formattedDate;
                    String message;
                    String day;

                    switch (systemLanguage) {
                        case "be":
                        case "ru":
                            formatter = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"));
                            day = today.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru"));
                            formattedDate = today.format(formatter) + ", " + day;
                            message = "<b>" + formattedDate + "</b>" + "\n\n⭐ <b>Статья Дня:</b>\n\n";
                            article = message + article;
                            break;
                        default:
                            formatter = DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH);
                            day = today.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("en"));
                            formattedDate = today.format(formatter) + ", " + day;
                            message = "<b>" + formattedDate + "</b>" + "\n\n⭐ <b>Article of the Day:</b>\n\n";
                            article = message + article;
                    }
                }
            }

            telegramBot.sendArticleWithButton(user.getChatID(), article);
        }
    }
}
