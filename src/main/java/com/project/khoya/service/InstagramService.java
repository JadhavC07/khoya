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
            return false;
        }

        String caption = generateCaption(alert);

        try {

            String containerId = createMediaContainer(alert.getImageUrl(), caption);

            if (containerId == null) {
                return false;
            }


            return publishMediaContainer(containerId);

        } catch (Exception e) {
            return false;
        }
    }


    private String createMediaContainer(String imageUrl, String caption) {

        final String containerApiVersion = "v21.0";
        String containerUrl = String.format("https://graph.instagram.com/%s/%s/media", containerApiVersion, igUserId);


        BodyInserters.FormInserter<String> body = BodyInserters.fromFormData("image_url", imageUrl).with("caption", caption).with("media_type", "IMAGE").with("access_token", accessToken);

        log.debug("Sending request to create Instagram container at: {}", containerUrl);

        try {

            Map response = webClient.post().uri(containerUrl).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(body).retrieve().bodyToMono(Map.class).block();

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


        BodyInserters.FormInserter<String> body = BodyInserters.fromFormData("creation_id", containerId).with("access_token", accessToken);

        try {
            // Send POST request
            Map response = webClient.post().uri(publishUrl).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(body).retrieve().bodyToMono(Map.class).block(); // Blocking call

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
