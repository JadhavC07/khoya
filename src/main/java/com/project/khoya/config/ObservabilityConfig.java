//package com.project.khoya.config;
//
//import io.micrometer.core.instrument.Counter;
//import io.micrometer.core.instrument.MeterRegistry;
//import io.micrometer.core.instrument.Timer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class ObservabilityConfig {
//
//    @Bean
//    public Counter alertCreatedCounter(MeterRegistry registry) {
//        return Counter.builder("khoya.alerts.created")
//                .description("Total number of alerts created")
//                .tag("type", "missing_person")
//                .register(registry);
//    }
//
//    @Bean
//    public Counter alertFoundCounter(MeterRegistry registry) {
//        return Counter.builder("khoya.alerts.found")
//                .description("Total number of alerts marked as found")
//                .register(registry);
//    }
//
//    @Bean
//    public Counter instagramPostCounter(MeterRegistry registry) {
//        return Counter.builder("khoya.social.instagram.posts")
//                .description("Total Instagram posts attempted")
//                .register(registry);
//    }
//
//    @Bean
//    public Counter instagramSuccessCounter(MeterRegistry registry) {
//        return Counter.builder("khoya.social.instagram.success")
//                .description("Successful Instagram posts")
//                .register(registry);
//    }
//
//    @Bean
//    public Counter instagramFailureCounter(MeterRegistry registry) {
//        return Counter.builder("khoya.social.instagram.failure")
//                .description("Failed Instagram posts")
//                .register(registry);
//    }
//
//    @Bean
//    public Counter facebookPostCounter(MeterRegistry registry) {
//        return Counter.builder("khoya.social.facebook.posts")
//                .description("Total Facebook posts attempted")
//                .register(registry);
//    }
//
//    @Bean
//    public Counter facebookSuccessCounter(MeterRegistry registry) {
//        return Counter.builder("khoya.social.facebook.success")
//                .description("Successful Facebook posts")
//                .register(registry);
//    }
//
//    @Bean
//    public Counter facebookFailureCounter(MeterRegistry registry) {
//        return Counter.builder("khoya.social.facebook.failure")
//                .description("Failed Facebook posts")
//                .register(registry);
//    }
//
//    @Bean
//    public Counter notificationCounter(MeterRegistry registry) {
//        return Counter.builder("khoya.notifications.sent")
//                .description("Total notifications sent")
//                .register(registry);
//    }
//
//    @Bean
//    public Timer alertProcessingTimer(MeterRegistry registry) {
//        return Timer.builder("khoya.alerts.processing.time")
//                .description("Time taken to process and create an alert")
//                .register(registry);
//    }
//
//    @Bean
//    public Timer socialMediaPostingTimer(MeterRegistry registry) {
//        return Timer.builder("khoya.social.posting.time")
//                .description("Time taken to post to social media")
//                .register(registry);
//    }
//}