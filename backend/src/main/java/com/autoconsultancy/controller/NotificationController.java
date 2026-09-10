package com.autoconsultancy.controller;

import com.autoconsultancy.dto.response.ApiResponse;
import com.autoconsultancy.entity.Notification;
import com.autoconsultancy.entity.User;
import com.autoconsultancy.repository.UserRepository;
import com.autoconsultancy.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getMyNotifications(Authentication authentication) {
        User user = getCurrentUser(authentication);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getMyNotifications(user.getId()), "Fetched notifications"));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id, Authentication authentication) {
        User user = getCurrentUser(authentication);
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Marked as read"));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        User user = getCurrentUser(authentication);
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "All marked as read"));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(Authentication authentication) {
        User user = getCurrentUser(authentication);
        long count = notificationService.getMyNotifications(user.getId()).stream().filter(n -> !n.isRead()).count();
        return ResponseEntity.ok(ApiResponse.success(count, "Fetched unread count"));
    }
}
