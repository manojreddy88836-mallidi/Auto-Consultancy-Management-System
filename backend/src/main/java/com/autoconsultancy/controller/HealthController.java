package com.autoconsultancy.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health check endpoints for Render's uptime monitoring.
 *
 * Render pings the root URL ("/") every 30 seconds to verify the service
 * is alive. This controller answers that ping immediately without any DB call
 * so it responds in <1ms regardless of MongoDB state.
 *
 * Both "/" and "/api/health" return {"status":"UP"}.
 * No internal details, version info, or credentials are exposed.
 */
@RestController
public class HealthController {

    /** Root path — answers Render's health checker pings at "/" */
    @GetMapping("/")
    public ResponseEntity<Map<String, String>> rootHealth() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    /** API health path — also permitted in SecurityConfig */
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
