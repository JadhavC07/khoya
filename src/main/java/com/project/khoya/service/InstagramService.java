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
public class InstagramService {


    private final WebClient webClient;
    private final String igUserId;
    private final String accessToken;


    public InstagramService(WebClient webClient) {
        this.webClient = webClient;
        Dotenv dotenv = Dotenv.load();
        this.igUserId = dotenv.get("INSTAGRAM_API_USER_ID");
        this.accessToken = dotenv.get("INSTAGRAM_API_ACCESS_TOKEN");
    }


    public boolean postAlertToInstagram(MissingAlert alert) {

        if (alert.getImageUrl() == null || alert.getImageUrl().isEmpty()) {
            log.warn("Skipping Instagram post for alert ID {} as no image URL is present.", alert.getId());
            return false;
        }

        log.info("Attempting to post alert ID {} to Instagram.", alert.getId());

        String caption = generateCaption(alert);

        try {

            String containerId = createMediaContainer(alert.getImageUrl(), caption);

            if (containerId == null) {
                log.error("Failed to create Instagram media container for alert ID: {}", alert.getId());
                return false;
            }


            return publishMediaContainer(containerId);

        } catch (Exception e) {
            log.error("A critical error occurred during Instagram posting for alert ID: {}. Error: {}", alert.getId(), e.getMessage(), e);
            return false;
        }
    }


    private String createMediaContainer(String imageUrl, String caption) {

        final String containerApiVersion = "v21.0";
        String containerUrl = String.format("https://graph.instagram.com/%s/%s/media", containerApiVersion, igUserId);


        BodyInserters.FormInserter<String> body = BodyInserters.fromFormData("image_url", imageUrl)
                .with("caption", caption)
                .with("media_type", "IMAGE")
                .with("access_token", accessToken);

        log.debug("Sending request to create Instagram container at: {}", containerUrl);

        try {

            Map response = webClient.post()
                    .uri(containerUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("id")) {
                String containerId = String.valueOf(response.get("id"));
                log.info("Successfully created Instagram media container with ID: {}", containerId);
                return containerId;
            } else {
                log.error("Instagram Container API call failed or response was missing 'id'. Response: {}", response);
                return null;
            }
        } catch (Exception e) {
            log.error("Error creating Instagram media container: {}", e.getMessage());
            return null;
        }
    }


    private boolean publishMediaContainer(String containerId) {

        final String publishApiVersion = "v23.0";
        String publishUrl = String.format("https://graph.instagram.com/%s/%s/media_publish", publishApiVersion, igUserId);


        BodyInserters.FormInserter<String> body = BodyInserters.fromFormData("creation_id", containerId)
                .with("access_token", accessToken);

        log.debug("Sending request to publish Instagram container ID: {}", containerId);

        try {
            // Send POST request
            Map response = webClient.post()
                    .uri(publishUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(); // Blocking call

            if (response != null && response.containsKey("id")) {
                log.info("Successfully published Instagram post. Post ID: {}", response.get("id"));
                return true;
            } else {
                log.error("Instagram Publish API call failed or response was missing 'id'. Response: {}", response);
                return false;
            }
        } catch (Exception e) {

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
