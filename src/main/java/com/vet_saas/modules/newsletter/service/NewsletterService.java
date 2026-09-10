package com.vet_saas.modules.newsletter.service;

import com.vet_saas.modules.newsletter.model.NewsletterSubscriber;
import com.vet_saas.modules.newsletter.repository.NewsletterSubscriberRepository;
import com.vet_saas.modules.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NewsletterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewsletterService.class);

    private final NewsletterSubscriberRepository repository;
    private final EmailService emailService;

    @Transactional
    public void subscribe(String email) {
        boolean isNewSubscription = repository.findByEmail(email).map(existing -> {
            if (!existing.isActivo()) {
                existing.setActivo(true);
                repository.save(existing);
                return true;
            }
            return false;
        }).orElseGet(() -> {
            repository.save(NewsletterSubscriber.builder().email(email).build());
            return true;
        });

        if (isNewSubscription) {
            try {
                emailService.sendNewsletterConfirmation(email);
                emailService.sendNewsletterAdminNotification(email);
            } catch (Exception ex) {
                LOGGER.error("Error sending newsletter emails for {}: {}", email, ex.getMessage(), ex);
            }
        }
    }
}
