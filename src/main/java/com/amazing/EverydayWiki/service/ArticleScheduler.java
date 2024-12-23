package com.amazing.EverydayWiki.service;

import com.amazing.EverydayWiki.database.User;
import com.amazing.EverydayWiki.database.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.amazing.EverydayWiki.service.WikipediaService;

import java.util.List;

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
    public void sendDailyArticleToAllUsers() {
        System.out.println("scheduler");
        String featuredArticle = wikipediaService.getTodaysFeaturedArticle(telegramBot.getLanguageCode());

        List<User> users = userService.getAllUsers();
        for (User user : users) {
            telegramBot.sendArticleWithButton(user.getChatID(), featuredArticle);
        }
    }
}
