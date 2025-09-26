package com.project.khoya.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            InputStream serviceAccount =
                    new ClassPathResource("firebase/firebase-service-account.json").getInputStream();

            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("✅ Firebase application initialized successfully");
                log.info("✅ Project ID: {}", options.getProjectId());
            } else {
                log.info("✅ Firebase already initialized");
            }

            serviceAccount.close(); // Close the stream

        } catch (IOException e) {
            log.error("❌ Failed to initialize Firebase: {}", e.getMessage(), e);

            // Try alternative approach with environment variable
            log.info("🔄 Trying alternative Firebase initialization...");
            tryAlternativeInitialization();
        }
    }

    private void tryAlternativeInitialization() {
        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("✅ Firebase initialized using Application Default Credentials");
            }
        } catch (IOException e) {
            log.error("❌ Alternative Firebase initialization also failed: {}", e.getMessage());
            log.error("💡 Please check your Firebase service account configuration");
        }
    }
}
