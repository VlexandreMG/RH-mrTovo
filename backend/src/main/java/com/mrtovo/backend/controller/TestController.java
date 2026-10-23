package com.mrtovo.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/hello")
    public Map<String, String> sayHello() {
        return Map.of("message", "Bonjour depuis le backend Spring Boot !");
    }
}