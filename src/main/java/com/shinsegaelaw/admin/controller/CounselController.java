package com.shinsegaelaw.admin.controller;

import com.shinsegaelaw.admin.model.dto.CounselRequestDto;
import com.shinsegaelaw.admin.model.dto.CounselResponseDto;
import com.shinsegaelaw.admin.service.persist.CounselService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/counsel")
@RequiredArgsConstructor
public class CounselController {

    private final CounselService counselService;

    @PostMapping("/add")
    public ResponseEntity<CounselResponseDto> addCounsel(@RequestBody CounselRequestDto request) {
        log.info("Received counsel add request - id: {}, url: {}", request.getId(), request.getUrl());

        try {
            CounselResponseDto response = counselService.processCounsel(request);
            log.info("Successfully processed counsel for id: {}", request.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error processing counsel for id: {}", request.getId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CounselResponseDto.builder()
                            .success(false)
                            .message("Failed to process counsel: " + e.getMessage())
                            .build());
        }
    }
}