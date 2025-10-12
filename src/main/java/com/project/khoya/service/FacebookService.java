package com.project.khoya.service;

import com.project.khoya.entity.MissingAlert;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Slf4j
public class FacebookService {

    private static final String API_VERSION = "v23.0";
    private final WebClient webClient;
    private final String pageId;
    private final String accessToken;

    public FacebookService(WebClient webClient) {
        this.webClient = webClient;
        Dotenv dotenv = Dotenv.load();
        this.pageId = dotenv.get("FACEBOOK_PAGE_ID");
        this.accessToken = dotenv.get("FACEBOOK_PAGE_ACCESS_TOKEN");
    }

    public boolean postAlertToFacebook(MissingAlert alert) {
        if (alert.getImageUrl() == null || alert.getImageUrl().isEmpty()) {
            return false;
        }

        String caption = generateCaption(alert);

        try {
            return createPhotoPost(alert.getImageUrl(), caption);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean createPhotoPost(String imageUrl, String caption) {
        String postUrl = String.format("https://graph.facebook.com/%s/%s/photos", API_VERSION, pageId);

        BodyInserters.FormInserter<String> body = BodyInserters.fromFormData("url", imageUrl).with("message", caption).with("access_token", accessToken);
        try {
            Map response = webClient.post().uri(postUrl).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(body).retrieve().bodyToMono(Map.class).block();

            return response != null && response.containsKey("id");
        } catch (Exception e) {
            return false;
        }
    }

    private String generateCaption(MissingAlert alert) {

        String caption = "🚨 URGENT MISSING PERSON ALERT 🚨\n\n" + "We urgently need your help to find: " + alert.getTitle() + "\n\n" + "👤 Reported Missing: " + alert.getTitle() + "\n" + "📍 Last Seen Location: " + alert.getLocation() + "\n\n" + "Please read the details:\n" + alert.getDescription() + "\n\n" + "If you have *any* information, please contact the authorities or use the in-app reporting feature immediately.\n" + "Every share helps! Thank you.\n\n" + "#MissingPerson #Missing #HelpFind #" + alert.getLocation().replaceAll("[^a-zA-Z0-9]", "") + " #" + alert.getTitle().replaceAll("[^a-zA-Z0-9]", "") + " #KhoyaApp";

        return caption;
    }
}