package com.collaborative.task.platform.controller;

import com.collaborative.task.platform.service.SystemInfoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * System controller providing system information endpoints
 * Demonstrates clean REST API design with proper separation of concerns
 */
@RestController
@RequestMapping("/system")
public class SystemController {

    private final SystemInfoService systemInfoService;

    public SystemController(SystemInfoService systemInfoService) {
        this.systemInfoService = systemInfoService;
    }

    /**
     * Get comprehensive system information
     * Useful for monitoring and debugging
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        return ResponseEntity.ok(systemInfoService.getSystemInfo());
    }
}