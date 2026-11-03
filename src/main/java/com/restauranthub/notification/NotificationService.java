package com.restauranthub.notification;

import com.restauranthub.entity.Notification;
import com.restauranthub.entity.User;
import com.restauranthub.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Transactional
    public void createInAppNotification(User user, String title, String message, String type) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
        log.info("Dispatched in-app notification to user: {} (Title: {})", user.getEmail(), title);
    }

    @Async
    public void sendEmailNotification(String toEmail, String subject, String content) {
        if (mailSender == null) {
            log.info("[MOCK MAIL SERVICE] To: {}, Subject: {}, Content: {}", toEmail, subject, content);
            return;
        }

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(toEmail);
            mailMessage.setSubject(subject);
            mailMessage.setText(content);
            mailMessage.setFrom("no-reply@restauranthub.com");

            mailSender.send(mailMessage);
            log.info("Email successfully dispatched to {}", toEmail);
        } catch (Exception ex) {
            // Fault tolerance: Never compromise the primary transaction if the SMTP server is down
            log.warn("Non-fatal: Failed to deliver email to {}. Provider error: {}", toEmail, ex.getMessage());
        }
    }

    public void notifyReservationConfirmed(User user, String referenceNo, String details) {
        String title = "Reservation Confirmed: " + referenceNo;
        String message = "Your booking (" + referenceNo + ") is officially confirmed. " + details;
        createInAppNotification(user, title, message, "RESERVATION");
        sendEmailNotification(user.getEmail(), title, message);
    }

    public void notifyOrderPlaced(User user, String orderNumber, String details) {
        String title = "Order Placed: " + orderNumber;
        String message = "Your culinary order (" + orderNumber + ") has been received by the kitchen. " + details;
        createInAppNotification(user, title, message, "ORDER");
        sendEmailNotification(user.getEmail(), title, message);
    }
}
