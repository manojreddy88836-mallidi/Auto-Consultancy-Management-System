package com.autoconsultancy.service;

import com.autoconsultancy.entity.Notification;
import com.autoconsultancy.entity.User;
import com.autoconsultancy.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void createNotification(User user, String title, String message, String type, Long applicationId) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .relatedApplicationId(applicationId)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
    }

    public List<Notification> getMyNotifications(Long userId) {
        return notificationRepository.findByUserId(userId);
    }

    public void markAsRead(Long notificationId, Long userId) {
        Notification notif = notificationRepository.findById(notificationId).orElseThrow();
        if(notif.getUser().getId().equals(userId)) {
            notif.setRead(true);
            notificationRepository.save(notif);
        }
    }

    public void markAllAsRead(Long userId) {
        List<Notification> notifs = notificationRepository.findByUserId(userId);
        notifs.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifs);
    }
}
