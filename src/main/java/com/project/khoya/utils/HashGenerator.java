//package com.project.khoya.utils;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//@Component
//public class HashGenerator implements CommandLineRunner {
//    @Autowired
//    private PasswordEncoder passwordEncoder;
//
//    @Override
//    public void run(String... args) {
//        String hash = passwordEncoder.encode("admin007");
//        System.out.println("Hashed password: " + hash);
//    }
//}