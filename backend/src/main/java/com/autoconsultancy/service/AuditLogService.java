package com.autoconsultancy.service;

import com.autoconsultancy.entity.AuditLog;
import com.autoconsultancy.entity.User;
import com.autoconsultancy.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(User user, String action, String entityType, Long entityId, String description, HttpServletRequest request) {
        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .ipAddress(request != null ? request.getRemoteAddr() : "SYSTEM")
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
    }
}
