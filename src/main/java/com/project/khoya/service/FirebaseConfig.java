package com.project.khoya.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
@Slf4j
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            InputStream serviceAccount;
            String serviceAccountJson = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON");
            if (serviceAccountJson != null && !serviceAccountJson.isEmpty()) {
                log.info("🔍 Loading Firebase from environment variable (HTTP v1)");
                serviceAccount = new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8));
            } else {
                log.info("🔍 Falling back to local service account file (HTTP v1)");
                serviceAccount = new ClassPathResource("firebase-service-account.json").getInputStream();
            }
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount)
                            .createScoped("https://www.googleapis.com/auth/firebase.messaging"))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("✅ Firebase initialized with HTTP v1 for project: {}", options.getProjectId());
            }
            serviceAccount.close();
        } catch (IOException e) {
            log.error("❌ Failed to initialize Firebase (HTTP v1): {}", e.getMessage(), e);
            tryAlternativeInitialization();
        }
    }

    private void tryAlternativeInitialization() {
        try {
            log.info("🔍 Attempting ADC for HTTP v1 initialization");
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault()
                            .createScoped("https://www.googleapis.com/auth/firebase.messaging"))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("✅ Firebase initialized with ADC for HTTP v1");
            }
        } catch (IOException e) {
            log.error("❌ ADC initialization failed (HTTP v1): {}", e.getMessage(), e);
            log.error("💡 Ensure GOOGLE_APPLICATION_CREDENTIALS or service account is configured");
        }
    }
}