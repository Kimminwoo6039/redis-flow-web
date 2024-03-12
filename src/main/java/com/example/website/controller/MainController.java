package com.example.website.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;

@Controller
public class MainController {
    RestTemplate restTemplate = new RestTemplate();

    @RequestMapping("/")
    public String index(@RequestParam(name = "queue", defaultValue = "default") String queue,
                        @RequestParam(name = "user_id") Long userId,
                        HttpServletRequest request
                        ) {

        Cookie[] cookies = request.getCookies();
        String cookieName = "user-queue-%s-token".formatted(queue);

        // 쿠키 비교해서
        String token = "";
        if (cookies != null) {
            Optional<Cookie> cookie = Arrays.stream(cookies).filter(i -> i.getName().equalsIgnoreCase(cookieName)).findFirst();
            token = cookie.orElse(new Cookie(cookieName,"")).getValue();
        }

        URI uri = UriComponentsBuilder
                .fromHttpUrl("http://127.0.0.1:9010")
                .path("/api/v1/queue/allowed")
                .queryParam("queue", queue)
                .queryParam("user_id", userId)
                .queryParam("token",token)
                .encode()
                .build()
                .toUri();

        ResponseEntity<AllowedUserResponse> response = restTemplate.getForEntity(uri, AllowedUserResponse.class);

        // response.getBody().allowed() 쿠키가 등록되면 허용상태라고 본다
        if (response.getBody() == null || !response.getBody().allowed()) { // 허용된 상태가 아니다면
            // 허용 상태가 아니면 리다이렉트
            // 대기 웹페이지로 리다이렉트
            return "redirect:http://127.0.0.1:9010/waiting-room?user_id=%d&redirect_url=%s".formatted(
                    userId,"http://127.0.0.1:9000?user_id=%d".formatted(userId));
        }
        
        // 허용 상태라면 해당페이지 진입
        return "/index";
    }

    public record AllowedUserResponse(Boolean allowed) {

    }
}
