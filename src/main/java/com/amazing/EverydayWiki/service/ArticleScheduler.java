package com.amazing.EverydayWiki.service;

import com.amazing.EverydayWiki.config.Language;
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

import static com.amazing.EverydayWiki.service.WikipediaService.FEATURED_RANDOM_URLS;

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
    //@Scheduled(fixedRate = 10000)
    public void sendDailyArticleToAllUsers() {
        List<User> users = userService.getAllUsers();
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        DayOfWeek dayOfWeek = today.getDayOfWeek();

        for (User user : users) {
            String language = user.getLanguage();
            String systemLanguage = user.getSystemLanguage();
            String article;

            if (language.equals(Language.ENGLISH)) {
                // Английская вики — статья дня каждый день
                article = wikipediaService.getTodaysFeaturedArticle(language, systemLanguage);
            } else {
                if (dayOfWeek == DayOfWeek.MONDAY) {
                    // Для других языков — статья дня только по понедельникам
                    article = wikipediaService.getTodaysFeaturedArticle(language, systemLanguage);
                } else {
                    // В остальные дни — случайная избранная
                    article = wikipediaService.fetchRedirectUrl(FEATURED_RANDOM_URLS.getOrDefault(language, FEATURED_RANDOM_URLS.get(Language.ENGLISH)));

                    DateTimeFormatter formatter;
                    String formattedDate;
                    String message;
                    String day;

                    switch (systemLanguage) {
                        case "be":
                            formatter = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("be"));
                            day = today.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("be"));
                            formattedDate = today.format(formatter) + ", " + day;
                            message = "<b>" + formattedDate + "</b>" + "\n\n⭐ <b>Артыкул Дня:</b>\n\n";
                            article = message + article;
                            break;
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
