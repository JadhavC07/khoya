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

    private final WebClient webClient;
    private final String pageId;
    private final String accessToken;
    private static final String API_VERSION = "v23.0";

    public FacebookService(WebClient webClient) {
        this.webClient = webClient;
        Dotenv dotenv = Dotenv.load();
        this.pageId = dotenv.get("FACEBOOK_PAGE_ID");
        this.accessToken = dotenv.get("FACEBOOK_PAGE_ACCESS_TOKEN");
    }

    public boolean postAlertToFacebook(MissingAlert alert) {
        if (alert.getImageUrl() == null || alert.getImageUrl().isEmpty()) {
            log.warn("Skipping Facebook post for alert ID {} as no image URL is present.", alert.getId());
            return false;
        }

        log.info("Attempting to post alert ID {} to Facebook Page.", alert.getId());

        String caption = generateCaption(alert);

        try {
            return createPhotoPost(alert.getImageUrl(), caption);
        } catch (Exception e) {
            log.error("A critical error occurred during Facebook posting for alert ID: {}. Error: {}", alert.getId(), e.getMessage(), e);
            return false;
        }
    }

    private boolean createPhotoPost(String imageUrl, String caption) {
        String postUrl = String.format("https://graph.facebook.com/%s/%s/photos", API_VERSION, pageId);

        BodyInserters.FormInserter<String> body = BodyInserters.fromFormData("url", imageUrl)
                .with("message", caption)
                .with("access_token", accessToken);

        log.debug("Sending request to post photo to Facebook at: {}", postUrl);

        try {
            Map response = webClient.post()
                    .uri(postUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("id")) {
                log.info("Successfully posted to Facebook. Post ID: {}", response.get("id"));
                return true;
            } else {
                log.error("Facebook Post API call failed or response was missing 'id'. Response: {}", response);
                return false;
            }
        } catch (Exception e) {
            log.error("Error posting to Facebook: {}", e.getMessage());
            return false;
        }
    }

    private String generateCaption(MissingAlert alert) {
        StringBuilder caption = new StringBuilder();
        caption.append("🚨 URGENT MISSING PERSON ALERT 🚨\n\n");
        caption.append("We urgently need your help to find: ").append(alert.getTitle()).append("\n\n");
        caption.append("👤 Reported Missing: ").append(alert.getTitle()).append("\n");
        caption.append("📍 Last Seen Location: ").append(alert.getLocation()).append("\n\n");

        caption.append("Please read the details:\n").append(alert.getDescription()).append("\n\n");

        caption.append("If you have *any* information, please contact the authorities or use the in-app reporting feature immediately.\n");
        caption.append("Every share helps! Thank you.\n\n");
        caption.append("#MissingPerson #Missing #HelpFind #").append(alert.getLocation().replaceAll("[^a-zA-Z0-9]", "")).append(" #").append(alert.getTitle().replaceAll("[^a-zA-Z0-9]", "")).append(" #KhoyaApp");

        return caption.toString();
    }
}