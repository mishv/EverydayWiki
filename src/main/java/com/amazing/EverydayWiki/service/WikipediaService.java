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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Function;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

@Service
public class WikipediaService {
    String message; // ?

    public String getAbsolutelyRandomArticle(String language) {
        String API_URLen = "https://en.wikipedia.org/w/api.php?action=query&format=json&list=random&rnnamespace=0&rnlimit=1";
        String API_URLru = "https://ru.wikipedia.org/w/api.php?action=query&format=json&list=random&rnnamespace=0&rnlimit=1";
        try {
            HttpURLConnection connection;

            if (language.equals("ru")) {
                connection = (HttpURLConnection) new URL(API_URLru).openConnection();
            } else {
                connection = (HttpURLConnection) new URL(API_URLen).openConnection();
            }

            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String response = reader.lines().collect(java.util.stream.Collectors.joining());
            reader.close();

            JsonNode rootNode = new ObjectMapper().readTree(response);
            JsonNode randomArticle = rootNode.at("/query/random/0");

            String title = randomArticle.get("title").asText();
            if (language.equals("ru")) {
                return "https://ru.wikipedia.org/wiki/" + title.replace(" ", "_");
            } else {
                return "https://en.wikipedia.org/wiki/" + title.replace(" ", "_");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Не удалось получить статью. Попробуйте позже.";
        }
    }

    public String getTodaysFeaturedArticle(String language) {
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();
        String TFA_URLen = String.format("https://en.wikipedia.org/api/rest_v1/feed/featured/%d/%02d/%02d", year, month, day);
        String URLru = "https://ru.wikipedia.org/wiki/Шаблон:Текущая_избранная_статья";
        String message;

        if (language.equals(Language.RUSSIAN.getDisplayName())) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"));
            String formattedDate = today.format(formatter);
            message = "<b>" + formattedDate + ".</b>" + "\n\n⭐ <b>Статья Дня:</b>\n\n";
            return message + parseWikiPage(URLru);
        } else {
            try {
                HttpURLConnection connection;
                connection = (HttpURLConnection) new URL(TFA_URLen).openConnection();
                connection.setRequestMethod("GET");

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH);
                String formattedDate = today.format(formatter);
                message = "<b>" + formattedDate + ".</b>" + "\n\n⭐ <b>Article of the Day:</b>\n\n";

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String response = reader.lines().collect(java.util.stream.Collectors.joining());
                reader.close();

                JsonNode rootNode = new ObjectMapper().readTree(response);
                JsonNode featuredArticle = rootNode.at("/tfa");

                String title = featuredArticle.get("title").asText();
                return message + "https://en.wikipedia.org/wiki/" + title.replace(" ", "_");

            } catch (Exception e) {
                e.printStackTrace();
                return "Не удалось получить статью. Попробуйте позже.";
            }
        }


    }

    public String getRandomFeaturedArticle(String language) {
        String API_URLru = "https://ru.wikipedia.org/wiki/Служебная:RandomInCategory/Википедия:Избранные_статьи_по_алфавиту";
        String API_URLen = "https://en.wikipedia.org/wiki/Special:RandomInCategory/Featured_articles";
        String redirectURL = "";
        String message;

        try {
            HttpURLConnection connection;

            if (language.equals("ru")) {
                connection = (HttpURLConnection) new URL(API_URLru).openConnection();
                message = "Статья из категории <b>\"Избранные статьи\"</b>:\n\n";
            } else {
                connection = (HttpURLConnection) new URL(API_URLen).openConnection();
                message = "This article is from the <b>'Featured Articles'</b> category:\n\n";
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

    public String getRandomGoodArticle(String language) {
        String API_URLru = "https://ru.wikipedia.org/wiki/Служебная:RandomInCategory/Википедия:Хорошие_статьи_по_алфавиту";
        String API_URLen = "https://en.wikipedia.org/wiki/Special:RandomInCategory/Good_articles";
        String redirectURL = "";
        String message;

        try {
            HttpURLConnection connection;

            if (language.equals("ru")) {
                connection = (HttpURLConnection) new URL(API_URLru).openConnection();
                message = "Статья из категории <b>\"Хорошие статьи\"</b>:\n\n";
            } else {
                connection = (HttpURLConnection) new URL(API_URLen).openConnection();
                message = "This article is from the <b>'Good Articles'</b> category:\n\n";
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

    public String getRandomVitalArticle(String language) {
        String API_URLru = "https://ru.wikipedia.org/wiki/Служебная:RandomInCategory/Википедия:Добротные_статьи_по_алфавиту";
        String API_URLen = "https://en.wikipedia.org/wiki/Special:RandomInCategory/All_Wikipedia_vital_articles";
        String redirectURL = "";
        String message;

        try {
            HttpURLConnection connection;

            if (language.equals("ru")) {
                connection = (HttpURLConnection) new URL(API_URLru).openConnection();
                message = "Статья из категории <b>\"Добротные статьи\"</b>:\n\n";
            } else {
                connection = (HttpURLConnection) new URL(API_URLen).openConnection();
                message = "This article is from the <b>'Vital Articles'</b> category:\n\n";
            }

            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();

            if (responseCode >= 300 && responseCode < 400) {
                String redirectedUrl = connection.getHeaderField("Location");
                if (redirectedUrl != null && language.equals("ru")) {
                    String decodedUrl = URLDecoder.decode(redirectedUrl, StandardCharsets.UTF_8);
                    String[] substrings = {"\"", "«", "»", "'"};
                    boolean contains = Arrays.stream(substrings).anyMatch(decodedUrl::contains);
                    if (contains) {
                        decodedUrl = sanitizeUrl(decodedUrl);
                    }
                    redirectURL = decodedUrl;
                } else if (redirectedUrl != null && language.equals("en")) {
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

    public String getRandomArticle(String language) {
        List<Function<String, String>> methods = List.of(
                this::getRandomVitalArticle,
                this::getRandomFeaturedArticle,
                this::getRandomGoodArticle
        );

        Random random = new Random();
        int randomIndex = random.nextInt(methods.size());
        return methods.get(randomIndex).apply(language);
    }

    public String sanitizeUrl(String url) {
        return url.replace("\"", "%22")
                .replace("«", "%AB")
                .replace("»", "%BB")
                .replace("'", "%27");
    }

    public String parseWikiPage(String url) {
        String title = "";
        try {
            // Загружаем HTML
            Document doc = Jsoup.connect(url).get();

            // Ищем div с нужным классом
            Element hatnoteDiv = doc.selectFirst("div.hatnote.navigation-not-searchable");

            if (hatnoteDiv != null) {
                // Ищем ссылку внутри div
                Element articleLink = hatnoteDiv.selectFirst("b > a");

                if (articleLink != null) {
                    // Получаем текст ссылки - это заголовок статьи
                    title = articleLink.text();
                } else {
                    System.out.println("Ссылка на статью не найдена.");
                }
            } else {
                System.out.println("Div с классом hatnote не найден.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String[] substrings = {"\"", "«", "»", "'"};
        boolean contains = Arrays.stream(substrings).anyMatch(title::contains);
        if (contains) {
            title = sanitizeUrl(title);
        }

        return "https://ru.wikipedia.org/wiki/" + title.replace(" ", "_");
    }

}
