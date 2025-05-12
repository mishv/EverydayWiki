package com.amazing.EverydayWiki.service;

import com.amazing.EverydayWiki.config.Language;
import com.amazing.EverydayWiki.database.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

@Service
public class WikipediaService {

    public String getAbsolutelyRandomArticle(String language, String systemLanguage) {
        String URLen = "https://en.wikipedia.org/wiki/Special:Random";
        String URLru = "https://ru.wikipedia.org/wiki/Служебная:Случайная_страница";
        String URLsimple = "https://simple.wikipedia.org/wiki/Special:Random";
        String URLbe = "https://be.wikipedia.org/wiki/Адмысловае:Random";
        String redirectURL = "";
        String message;

        try {
            HttpURLConnection connection;

            switch (systemLanguage) {
                case "be":
                case "ru": message = "Случайная статья:\n\n";
                    break;
                default: message = "Random article:\n\n";
            }

            switch (language) {
                case Language.SIMPLE_ENGLISH:
                    connection = (HttpURLConnection) new URL(URLsimple).openConnection();
                    message = "";
                    break;
                case Language.RUSSIAN:
                    connection = (HttpURLConnection) new URL(URLru).openConnection();
                    break;
                case Language.BELORUSSIAN:
                    connection = (HttpURLConnection) new URL(URLbe).openConnection();
                    break;
                default:
                    connection = (HttpURLConnection) new URL(URLen).openConnection();
            }


            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();

            if (responseCode >= 300 && responseCode < 400) {
                String redirectedUrl = connection.getHeaderField("Location");
                if (redirectedUrl != null) {
                    String decodedUrl = URLDecoder.decode(redirectedUrl, StandardCharsets.UTF_8);
                    String[] substrings = {"\"", "«", "»", "'"};
                    boolean contains = Arrays.stream(substrings).anyMatch(decodedUrl::contains);
                    if (contains) {
                        decodedUrl = sanitizeUrl(decodedUrl);
                    }
                    redirectURL = decodedUrl;
                } else {
                    throw new Exception("Please try again later or restart the bot.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Please try again later or restart the bot.";
        }

        return message + redirectURL;
    }

    public String getTodaysFeaturedArticle(String language, String systemLanguage) {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();
        String TFA_URLen = String.format("https://en.wikipedia.org/api/rest_v1/feed/featured/%d/%02d/%02d", year, month, day);
        String URLru = "https://ru.wikipedia.org/wiki/Шаблон:Текущая_избранная_статья";
        String message;
        DateTimeFormatter formatter;
        String formattedDate;
        String dayOfWeek = "";

        switch (systemLanguage) {
            case "be":
            case "ru":
                formatter = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"));
                dayOfWeek = today.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("ru"));
                formattedDate = today.format(formatter) + ", " + dayOfWeek;
                message = "<b>" + formattedDate + "</b>" + "\n\n⭐ <b>Статья Дня:</b>\n\n";
                break;
            default:
                formatter = DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH);
                dayOfWeek = today.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("en"));
                formattedDate = today.format(formatter) + ", " + dayOfWeek;
                message = "<b>" + formattedDate + "</b>" + "\n\n⭐ <b>Article of the Day:</b>\n\n";
        }

        switch (language) {
            case Language.RUSSIAN:
                return message + parseWikiPage(URLru);

            case Language.SIMPLE_ENGLISH:
                try {
                    // Загружаем страницу Main_Page с Simple Wikipedia
                    Document doc = Jsoup.connect("https://simple.wikipedia.org/wiki/Main_Page").get();
                    String url = "https://simple.wikipedia.org/wiki/";
                    String html = doc.html();

                    // Ищем элемент, который содержит ссылку на статью дня
                    String regex = "Selected article(\\n|.)+?<p><b><a href=\"\\/wiki\\/(.+?)\"";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(html);

                    if (matcher.find()) {
                        // Извлекаем ссылку на статью (путь будет начинаться с /wiki/)
                        String articleLink = matcher.group(2);
                        url += articleLink;
                    } else {
                        System.out.println("Please try again later or restart the bot.");
                    }

                    // Формируем ссылку на статью
                    return message + url;

                } catch (Exception e) {
                    e.printStackTrace();
                    return "Please try again later or restart the bot.";
                }

            case Language.ENGLISH:
                try {
                    HttpURLConnection connection;
                    connection = (HttpURLConnection) new URL(TFA_URLen).openConnection();
                    connection.setRequestMethod("GET");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String response = reader.lines().collect(Collectors.joining());
                    reader.close();

                    JsonNode rootNode = new ObjectMapper().readTree(response);
                    JsonNode featuredArticle = rootNode.at("/tfa");

                    String title = featuredArticle.get("title").asText();
                    return message + "https://en.wikipedia.org/wiki/" + title.replace(" ", "_");

                } catch (Exception e) {
                    e.printStackTrace();
                    return "Please try again later or restart the bot.";
                }

            case Language.BELORUSSIAN:
                try {
                    // Загружаем страницу Main_Page с Simple Wikipedia
                    Document doc = Jsoup.connect("https://be.wikipedia.org/wiki/Галоўная_старонка").get();
                    String url = "https://be.wikipedia.org/wiki/";
                    String html = doc.html();

                    // Ищем элемент, который содержит ссылку на статью дня
                    String regex = "Артыкул дня(\\n|.)+?<p><b><a href=\"\\/wiki\\/(.+?)\"";
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(html);

                    if (matcher.find()) {
                        // Извлекаем ссылку на статью (путь будет начинаться с /wiki/)
                        String articleLink = matcher.group(2);
                        articleLink = URLDecoder.decode(articleLink, StandardCharsets.UTF_8);
                        url += articleLink;
                    } else {
                        System.out.println("Please try again later or restart the bot.");
                    }

                    // Формируем ссылку на статью
                    return message + url;

                } catch (Exception e) {
                    e.printStackTrace();
                    return "Please try again later or restart the bot.";
                }


            default:
                return "Please try again later or restart the bot.";
        }


    }

    public String getRandomFeaturedArticle(String language, String systemLanguage) {
        String API_URLru = "https://ru.wikipedia.org/wiki/Служебная:RandomInCategory/Википедия:Избранные_статьи_по_алфавиту";
        String API_URLen = "https://en.wikipedia.org/wiki/Special:RandomInCategory/Featured_articles";
        String URLbe = "https://be.wikipedia.org/wiki/Адмысловае:RandomInCategory/Вікіпедыя:Выдатныя_артыкулы_паводле_алфавіта";
        String URLsimple = "https://simple.wikipedia.org/wiki/Special:RandomInCategory/Very_good_articles";
        String redirectURL = "";
        String message;

        try {
            HttpURLConnection connection;

            switch (systemLanguage) {
                case "be":
                case "ru":
                    message = "Статья из категории <b>\"Избранные статьи\"</b>:\n\n";
                    break;
                default:
                    message = "This article is from the <b>'Featured Articles'</b> category:\n\n";
            }

            switch (language) {
                case Language.RUSSIAN:
                    connection = (HttpURLConnection) new URL(API_URLru).openConnection();
                    break;
                case Language.BELORUSSIAN:
                    connection = (HttpURLConnection) new URL(URLbe).openConnection();
                    break;
                case Language.SIMPLE_ENGLISH:
                    connection = (HttpURLConnection) new URL(URLsimple).openConnection();
                    message = "";
                    break;
                default:
                    connection = (HttpURLConnection) new URL(API_URLen).openConnection();
            }


            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();

            if (responseCode >= 300 && responseCode < 400) {
                String redirectedUrl = connection.getHeaderField("Location");
                if (redirectedUrl != null) {
                    String decodedUrl = URLDecoder.decode(redirectedUrl, StandardCharsets.UTF_8);
                    String[] substrings = {"\"", "«", "»", "'"};
                    boolean contains = Arrays.stream(substrings).anyMatch(decodedUrl::contains);
                    if (contains) {
                        decodedUrl = sanitizeUrl(decodedUrl);
                    }
                    redirectURL = decodedUrl;
                } else {
                    throw new Exception("Please try again later or restart the bot.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Please try again later or restart the bot.";
        }

        return message + redirectURL;
    }

    public String getRandomGoodArticle(String language, String systemLanguage) {
        String API_URLru = "https://ru.wikipedia.org/wiki/Служебная:RandomInCategory/Википедия:Хорошие_статьи_по_алфавиту";
        String API_URLen = "https://en.wikipedia.org/wiki/Special:RandomInCategory/Good_articles";
        String URLbe = "https://be.wikipedia.org/wiki/Адмысловае:RandomInCategory/Вікіпедыя:Добрыя_артыкулы_паводле_алфавіта";
        String redirectURL = "";
        String message;

        try {
            HttpURLConnection connection;

            switch (systemLanguage) {
                case "be":
                case "ru":
                    message = "Статья из категории <b>\"Хорошие статьи\"</b>:\n\n";
                    break;
                default:
                    message = "This article is from the <b>'Good Articles'</b> category:\n\n";

            }

            switch (language) {
                case Language.RUSSIAN:
                    connection = (HttpURLConnection) new URL(API_URLru).openConnection();
                    break;
                case Language.BELORUSSIAN:
                    connection = (HttpURLConnection) new URL(URLbe).openConnection();
                    break;
                default:
                    connection = (HttpURLConnection) new URL(API_URLen).openConnection();
            }

            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();

            if (responseCode >= 300 && responseCode < 400) {
                String redirectedUrl = connection.getHeaderField("Location");
                if (redirectedUrl != null) {
                    String decodedUrl = URLDecoder.decode(redirectedUrl, StandardCharsets.UTF_8);
                    String[] substrings = {"\"", "«", "»", "'"};
                    boolean contains = Arrays.stream(substrings).anyMatch(decodedUrl::contains);
                    if (contains) {
                        decodedUrl = sanitizeUrl(decodedUrl);
                    }
                    redirectURL = decodedUrl;
                } else {
                    throw new Exception("Please try again later or restart the bot.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Please try again later or restart the bot.";
        }

        return message + redirectURL;
    }

    public String getRandomVitalArticle(String language, String systemLanguage) {
        String API_URLru = "https://ru.wikipedia.org/wiki/Служебная:RandomInCategory/Википедия:Добротные_статьи_по_алфавиту";
        String API_URLen = "https://en.wikipedia.org/wiki/Special:RandomInCategory/All_Wikipedia_vital_articles";
        String redirectURL = "";
        String message;

        try {
            HttpURLConnection connection;

            if (language.equals(Language.RUSSIAN)) {
                connection = (HttpURLConnection) new URL(API_URLru).openConnection();
            } else {
                connection = (HttpURLConnection) new URL(API_URLen).openConnection();
            }

            switch (systemLanguage) {
                case "be":
                case "ru":
                    message = "Статья из категории <b>\"Добротные статьи\"</b>:\n\n";
                    break;
                default:
                    message = "This article is from the <b>'Vital Articles'</b> category:\n\n";
            }

            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();

            if (responseCode >= 300 && responseCode < 400) {
                String redirectedUrl = connection.getHeaderField("Location");
                if (redirectedUrl != null && language.equals(Language.RUSSIAN)) {
                    String decodedUrl = URLDecoder.decode(redirectedUrl, StandardCharsets.UTF_8);
                    String[] substrings = {"\"", "«", "»", "'"};
                    boolean contains = Arrays.stream(substrings).anyMatch(decodedUrl::contains);
                    if (contains) {
                        decodedUrl = sanitizeUrl(decodedUrl);
                    }
                    redirectURL = decodedUrl;
                } else if (redirectedUrl != null && language.equals(Language.ENGLISH)) {
                    redirectURL = redirectedUrl.replace("/Talk:", "/");
                } else {
                    throw new Exception("Please try again later or restart the bot.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Please try again later or restart the bot.";
        }

        return message + redirectURL;
    }

    public String getRandomArticle(String language, String systemLanguage) {
        List<BiFunction<String, String, String>> methods;

        switch (language) {
            case Language.SIMPLE_ENGLISH:
                methods = List.of(
                        this::getRandomFeaturedArticle,
                        this::getAbsolutelyRandomArticle
                );
                break;

            case Language.BELORUSSIAN:
                methods = List.of(
                        this::getRandomFeaturedArticle,
                        this::getRandomGoodArticle,
                        this::getAbsolutelyRandomArticle
                );
                break;

            case Language.RUSSIAN:
            default:
                methods = List.of(
                        this::getRandomVitalArticle,
                        this::getRandomFeaturedArticle,
                        this::getRandomGoodArticle,
                        this::getAbsolutelyRandomArticle
                );

        }

        Random random = new Random();
        int randomIndex = random.nextInt(methods.size());
        return methods.get(randomIndex).apply(language, systemLanguage);
    }


    public String sanitizeUrl(String url) {
        return url.replace("\"", "%22")
                .replace("«", "%C2%AB")
                .replace("»", "%C2%BB")
                .replace("'", "%27");
    }

    public String parseWikiPage(String url) {
        String path = "";
        try {
            // Загружаем HTML
            Document doc = Jsoup.connect(url).get();

            // Ищем div с нужным классом
            Element hatnoteDiv = doc.selectFirst("div.hatnote.navigation-not-searchable");

            if (hatnoteDiv != null) {
                // Ищем первую ссылку с нужным href
                Element articleLink = hatnoteDiv.selectFirst("a[href^=\"/wiki/\"]");

                if (articleLink != null) {
                    path = articleLink.attr("href"); // /wiki/Красная_планета_(роман)
                } else {
                    System.out.println("Please try again later or restart the bot.");
                }
            } else {
                System.out.println("Please try again later or restart the bot.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8);

        String[] substrings = {"\"", "«", "»", "'"};
        boolean contains = Arrays.stream(substrings).anyMatch(decodedPath::contains);
        if (contains) {
            decodedPath = sanitizeUrl(decodedPath);
        }

        return "https://ru.wikipedia.org" + decodedPath;
    }

}
