package com.autoconsultancy.service;

import com.autoconsultancy.entity.Notification;
import com.autoconsultancy.entity.User;
import com.autoconsultancy.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MongoTemplate mongoTemplate;

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
        // H6 fix: single bulk update instead of load-all + saveAll
        // db.notifications.updateMany({userId: userId, read: false}, {$set: {read: true}})
        Query query = new Query(Criteria.where("user.$id").is(userId).and("read").is(false));
        Update update = new Update().set("read", true);
        mongoTemplate.updateMulti(query, update, Notification.class);
    }
}
