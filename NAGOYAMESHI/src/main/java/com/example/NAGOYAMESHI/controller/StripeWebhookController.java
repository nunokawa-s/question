package com.example.NAGOYAMESHI.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.example.NAGOYAMESHI.entity.User;
import com.example.NAGOYAMESHI.repository.UserRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

@RestController
public class StripeWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(StripeWebhookController.class);

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    private final UserRepository userRepository;

    public StripeWebhookController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/stripe/webhook")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            logger.info("Received Stripe event: {}", event.getType());
        } catch (SignatureVerificationException e) {
            logger.error("Error verifying webhook signature.", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook Error: Invalid signature");
        } catch (Exception e) {
            logger.error("Error parsing webhook event.", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook Error: Could not parse event");
        }

        try {
            switch (event.getType()) {
                case "checkout.session.completed":
                    Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                    if (session != null) {
                        logger.info("Checkout session completed: {}", session.getId());
                        String userIdString = session.getMetadata().get("userId");
                        if (userIdString != null) {
                            Integer userId = Integer.parseInt(userIdString);
                            User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found for ID: " + userId));
                            user.setPaidFlg(true);
                            user.setStripeCustomerId(session.getCustomer());
                            userRepository.save(user);
                            logger.info("User {} (ID: {}) paidFlg updated to true and Stripe Customer ID saved.", user.getName(), userId);
                        } else {
                            logger.warn("userId metadata not found in checkout.session.completed event for session ID: {}", session.getId());
                        }
                    }
                    break;
                case "customer.subscription.updated":
                    Subscription subscriptionUpdated = (Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
                    if (subscriptionUpdated != null) {
                        logger.info("Subscription updated: {}", subscriptionUpdated.getId());
                        String customerId = subscriptionUpdated.getCustomer();
                        User user = userRepository.findByStripeCustomerId(customerId)
                            .orElseThrow(() -> new RuntimeException("User with Stripe Customer ID " + customerId + " not found."));
                        user.setPaidFlg("active".equals(subscriptionUpdated.getStatus())); // active なら true, それ以外なら false
                        userRepository.save(user);
                        logger.info("User {} (Stripe Customer ID: {}) paidFlg updated due to subscription status: {}", user.getName(), customerId, subscriptionUpdated.getStatus());
                    }
                    break;
                case "customer.subscription.deleted":
                    Subscription subscriptionDeleted = (Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
                    if (subscriptionDeleted != null) {
                        logger.info("Subscription deleted: {}", subscriptionDeleted.getId());
                        String customerId = subscriptionDeleted.getCustomer();
                         User user = userRepository.findByStripeCustomerId(customerId)
                            .orElseThrow(() -> new RuntimeException("User with Stripe Customer ID " + customerId + " not found."));
                        user.setPaidFlg(false);
                        userRepository.save(user);
                        logger.info("User {} (Stripe Customer ID: {}) paidFlg set to false due to subscription deleted event.", user.getName(), customerId);
                    }
                    break;
                default:
                    logger.warn("Unhandled Stripe event type: {}", event.getType());
                    break;
            }
            return ResponseEntity.ok("Received");
        } catch (Exception e) {
            logger.error("Error processing Stripe event.", e); // 例外をキャッチしてログ出力
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing Stripe event"); // 500 Internal Server Error を返す
        }
    }
}