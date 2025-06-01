package com.amazing.EverydayWiki.service;

import com.amazing.EverydayWiki.config.Language;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import org.jsoup.nodes.Document;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

@Service
public class WikipediaService {
    private static final Map<String, String> RANDOM_URLS = Map.of(
            Language.ENGLISH, "https://en.wikipedia.org/wiki/Special:Random",
            Language.RUSSIAN, "https://ru.wikipedia.org/wiki/Служебная:Случайная_страница",
            Language.SIMPLE_ENGLISH, "https://simple.wikipedia.org/wiki/Special:Random",
            Language.BELORUSSIAN, "https://be.wikipedia.org/wiki/Адмысловае:Random"
    );

    public static final Map<String, String> FEATURED_RANDOM_URLS = Map.of(
            Language.ENGLISH, "https://en.wikipedia.org/wiki/Special:RandomInCategory/Featured_articles",
            Language.RUSSIAN, "https://ru.wikipedia.org/wiki/Служебная:RandomInCategory/Википедия:Избранные_статьи_по_алфавиту",
            Language.SIMPLE_ENGLISH, "https://simple.wikipedia.org/wiki/Special:RandomInCategory/Very_good_articles",
            Language.BELORUSSIAN, "https://be.wikipedia.org/wiki/Адмысловае:RandomInCategory/Вікіпедыя:Выдатныя_артыкулы_паводле_алфавіта"
    );

    private static final Map<String, String> GOOD_RANDOM_URLS = Map.of(
            Language.ENGLISH, "https://en.wikipedia.org/wiki/Special:RandomInCategory/Good_articles",
            Language.RUSSIAN, "https://ru.wikipedia.org/wiki/Служебная:RandomInCategory/Википедия:Хорошие_статьи_по_алфавиту",
            Language.BELORUSSIAN, "https://be.wikipedia.org/wiki/Адмысловае:RandomInCategory/Вікіпедыя:Добрыя_артыкулы_паводле_алфавіта"
    );

    private static final Map<String, String> VITAL_RANDOM_URLS = Map.of(
            Language.ENGLISH, "https://en.wikipedia.org/wiki/Special:RandomInCategory/All_Wikipedia_vital_articles",
            Language.RUSSIAN, "https://ru.wikipedia.org/wiki/Служебная:RandomInCategory/Википедия:Добротные_статьи_по_алфавиту"
    );

    private static final String ERROR_MESSAGE = "Please try again later or restart the bot.";

    private record MessageInfo(String message, String formattedDate) {}

    public String getAbsolutelyRandomArticle(String language, String systemLanguage) {
        String message = switch (systemLanguage) {
            case "ru" -> "Случайная статья:\n\n";
            case "be" -> "Выпадковы артыкул:\n\n";
            default -> "Random article:\n\n";
        };

        if (language.equals(Language.SIMPLE_ENGLISH)) {
            message = "";
        }

        return message + fetchRedirectUrl(RANDOM_URLS.getOrDefault(language, RANDOM_URLS.get(Language.ENGLISH)));
    }

    public String getTodaysFeaturedArticle(String language, String systemLanguage) {
        MessageInfo messageInfo = buildMessageInfo(systemLanguage);
        return switch (language) {
            case Language.RUSSIAN -> messageInfo.message() + parseWikiPage("https://ru.wikipedia.org/wiki/Шаблон:Текущая_избранная_статья");
            case Language.SIMPLE_ENGLISH -> messageInfo.message() + parseSimpleEnglishFeatured();
            case Language.ENGLISH -> messageInfo.message() + fetchEnglishFeaturedArticle(messageInfo.formattedDate());
            case Language.BELORUSSIAN -> messageInfo.message() + parseBelorussianFeatured();
            default -> ERROR_MESSAGE;
        };
    }

    public String getRandomFeaturedArticle(String language, String systemLanguage) {
        String category = switch (language) {
            case Language.RUSSIAN -> "Избранные статьи";
            case Language.BELORUSSIAN -> "Выдатныя артыкулы";
            default -> "Featured Articles";
        };
        String message = switch (systemLanguage) {
            case "ru" -> "Статья из категории <b>\"" + category + "\"</b>:\n\n";
            case "be" -> "Артыкул з катэгорыі <b>\""  + category + "\"</b>:\n\n";
            default -> "This article is from the <b>'" + category + "'</b> category:\n\n";
        };

        if (language.equals(Language.SIMPLE_ENGLISH)) {
            message = "";
        }

        return message + fetchRedirectUrl(FEATURED_RANDOM_URLS.getOrDefault(language, FEATURED_RANDOM_URLS.get(Language.ENGLISH)));
    }

    public String getRandomGoodArticle(String language, String systemLanguage) {
        String category = switch (language) {
            case Language.RUSSIAN -> "Хорошие статьи";
            case Language.BELORUSSIAN -> "Добрыя артыкулы";
            default -> "Good Articles";
        };
        String message = switch (systemLanguage) {
            case "ru" -> "Статья из категории <b>\"" + category + "\"</b>:\n\n";
            case "be" -> "Артыкул з катэгорыі <b>\"" + category + "\"</b>:\n\n";
            default -> "This article is from the <b>'" + category + "'</b> category:\n\n";
        };
        return message + fetchRedirectUrl(GOOD_RANDOM_URLS.getOrDefault(language, GOOD_RANDOM_URLS.get(Language.ENGLISH)));
    }

    public String getRandomVitalArticle(String language, String systemLanguage) {
        String category = switch (language) {
            case Language.RUSSIAN -> "Добротные статьи";
            default -> "Vital Articles";
        };
        String message = switch (systemLanguage) {
            case "ru" -> "Статья из категории <b>\"" + category + "\"</b>:\n\n";
            case "be" -> "Артыкул з катэгорыі <b>\"" + category + "\"</b>:\\n\\n";
            default -> "This article is from the <b>'" + category + "'</b> category:\n\n";
        };
        String url = VITAL_RANDOM_URLS.getOrDefault(language, VITAL_RANDOM_URLS.get(Language.ENGLISH));
        String redirectUrl = fetchRedirectUrl(url);
        return message + (language.equals(Language.ENGLISH) ? redirectUrl.replace("/Talk:", "/") : redirectUrl);
    }

    public String getRandomArticle(String language, String systemLanguage) {
        List<BiFunction<String, String, String>> methods = switch (language) {
            case Language.SIMPLE_ENGLISH -> List.of(
                    this::getRandomFeaturedArticle,
                    this::getAbsolutelyRandomArticle);
            case Language.BELORUSSIAN -> List.of(
                    this::getRandomFeaturedArticle,
                    this::getRandomGoodArticle,
                    this::getAbsolutelyRandomArticle);
            default -> List.of(
                    this::getRandomVitalArticle,
                    this::getRandomFeaturedArticle,
                    this::getRandomGoodArticle,
                    this::getAbsolutelyRandomArticle);
        };
        return methods.get(new Random().nextInt(methods.size())).apply(language, systemLanguage);
    }

    public String fetchRedirectUrl(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() >= 300 && conn.getResponseCode() < 400) {
                String redirectedUrl = conn.getHeaderField("Location");
                if (redirectedUrl != null) {
                    String decodedUrl = URLDecoder.decode(redirectedUrl, StandardCharsets.UTF_8);
                    return needsSanitization(decodedUrl) ? sanitizeUrl(decodedUrl) : decodedUrl;
                }
            }
        } catch (Exception e) {
            logError(e);
        }
        return ERROR_MESSAGE;
    }

    private MessageInfo buildMessageInfo(String systemLanguage) {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter;
        Locale locale;
        String articleLabel;

        switch (systemLanguage) {
            case "ru":
                formatter = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"));
                locale = new Locale("ru");
                articleLabel = "Статья Дня";
                break;
            case "be":
                formatter = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("be"));
                locale = new Locale("be");
                articleLabel = "Артыкул Дня";
                break;
            default:
                formatter = DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH);
                locale = Locale.ENGLISH;
                articleLabel = "Article of the Day";
        }

        String dayOfWeek = today.getDayOfWeek().getDisplayName(TextStyle.FULL, locale);
        String formattedDate = today.format(formatter) + ", " + dayOfWeek;
        String message = "<b>" + formattedDate + "</b>\n\n⭐ <b>" + articleLabel + ":</b>\n\n";
        return new MessageInfo(message, formattedDate);
    }

    private String parseSimpleEnglishFeatured() {
        try {
            Document doc = Jsoup.connect("https://simple.wikipedia.org/wiki/Main_Page").get();
            String regex = "Selected article(\\n|.)+?<p><b><a href=\"\\/wiki\\/(.+?)\"";
            Matcher matcher = Pattern.compile(regex).matcher(doc.html());
            return matcher.find() ? "https://simple.wikipedia.org/wiki/" + matcher.group(2) : ERROR_MESSAGE;
        } catch (Exception e) {
            logError(e);
            return ERROR_MESSAGE;
        }
    }

    private String fetchEnglishFeaturedArticle(String date) {
        try {
            String url = String.format("https://en.wikipedia.org/api/rest_v1/feed/featured/%d/%02d/%02d",
                    LocalDate.now().getYear(), LocalDate.now().getMonthValue(), LocalDate.now().getDayOfMonth());
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");

            String response = new BufferedReader(new InputStreamReader(conn.getInputStream()))
                    .lines().collect(Collectors.joining());
            JsonNode tfa = new ObjectMapper().readTree(response).at("/tfa");
            return "https://en.wikipedia.org/wiki/" + tfa.get("title").asText().replace(" ", "_");
        } catch (Exception e) {
            logError(e);
            return ERROR_MESSAGE;
        }
    }

    private String parseBelorussianFeatured() {
        try {
            Document doc = Jsoup.connect("https://be.wikipedia.org/wiki/Галоўная_старонка").get();
            String regex = "Артыкул дня(\\n|.)+?<p><b><a href=\"\\/wiki\\/(.+?)\"";
            Matcher matcher = Pattern.compile(regex).matcher(doc.html());
            if (matcher.find()) {
                String path = URLDecoder.decode(matcher.group(2), StandardCharsets.UTF_8);
                return "https://be.wikipedia.org/wiki/" + (needsSanitization(path) ? sanitizeUrl(path) : path);
            }
            return ERROR_MESSAGE;
        } catch (Exception e) {
            logError(e);
            return ERROR_MESSAGE;
        }
    }

    private String parseWikiPage(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            Element link = doc.selectFirst("div.hatnote.navigation-not-searchable a[href^=\"/wiki/\"]");
            if (link != null) {
                String path = URLDecoder.decode(link.attr("href"), StandardCharsets.UTF_8);
                return "https://ru.wikipedia.org" + (needsSanitization(path) ? sanitizeUrl(path) : path);
            }
        } catch (Exception e) {
            logError(e);
        }
        return ERROR_MESSAGE;
    }

    private String sanitizeUrl(String url) {
        return url.replace("\"", "%22")
                .replace("«", "%C2%AB")
                .replace("»", "%C2%BB")
                .replace("'", "%27");
    }

    private boolean needsSanitization(String url) {
        return url.contains("\"") || url.contains("«") || url.contains("»") || url.contains("'");
    }

    private void logError(Exception e) {
        // В продакшене заменить на логгер, например, SLF4J
        e.printStackTrace();
    }
}
