package com.panha.markmekh.controller;

import com.panha.markmekh.dto.ApiResponse;
import com.panha.markmekh.dto.NotificationDto;
import com.panha.markmekh.security.SecurityUtils;
import com.panha.markmekh.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationDto>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(
                service.listForUser(SecurityUtils.currentUserId())));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount() {
        long count = service.unreadCount(SecurityUtils.currentUserId());
        Map<String, Long> body = new HashMap<>();
        body.put("count", count);
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id) {
        service.markRead(SecurityUtils.currentUserId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Marked read", null));
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        service.markAllRead(SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.ok("All read", null));
    }
}