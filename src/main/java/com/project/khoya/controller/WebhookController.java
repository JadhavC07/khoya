package com.project.khoya.controller;

import org.springframework.web.bind.annotation.*;

@RestController
public class WebhookController {
    private static final String VERIFY_TOKEN = "IGAAQ7Cogd14VBZAFFZAWFpKd3FhX0o0UDd0ZAGZA2NkFMdENzZATRxSllxVC1lcVZAWb0ppclNsNjhKTUkwckdacnhBeXdCb21nUGhGd0NiSVl2NTJIZA1N5WUxEc0h3TUd5a2F3NXpiVzZArQVJCQWxQSkVjbnpjU2s1OU9paUFkQ0pCawZDZD";


    @GetMapping("/webhook")
    public String verifyWebhook(@RequestParam(value = "hub.mode") String mode,
                                @RequestParam(value = "hub.challenge") String challenge,
                                @RequestParam(value = "hub.verify_token") String verifyToken) {
        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(verifyToken)) {
            return challenge;
        } else {
            return "Verification failed";
        }
    }

    @PostMapping("/webhook")
    public String handleWebhookEvent(@RequestBody String payload) {
        System.out.println("Received webhook event: " + payload);
        return "EVENT_RECEIVED";
    }
}
