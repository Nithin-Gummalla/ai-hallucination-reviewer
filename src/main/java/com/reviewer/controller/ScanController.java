package com.reviewer.controller;

import com.reviewer.dto.ScanRequest;
import com.reviewer.dto.ScanResponse;
import com.reviewer.service.ScanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ScanController {

    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    @PostMapping("/scan")
    public ResponseEntity<ScanResponse> scan(@RequestBody ScanRequest request) {
        ScanResponse response = scanService.scanRepository(request);
        return ResponseEntity.ok(response);
    }
}
