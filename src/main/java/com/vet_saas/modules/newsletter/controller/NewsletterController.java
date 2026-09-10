package com.vet_saas.modules.newsletter.controller;

import com.vet_saas.core.response.ApiResponse;
import com.vet_saas.modules.newsletter.service.NewsletterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterService newsletterService;

    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<Map<String, String>>> subscribe(
            @Valid @RequestBody SubscribeRequest request) {
        newsletterService.subscribe(request.email());
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Suscrito correctamente")));
    }

    public record SubscribeRequest(@NotBlank @Email String email) {}
}
