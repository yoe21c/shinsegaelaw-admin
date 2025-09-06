package com.tbm.admin.controller.api;

import com.tbm.admin.model.view.rest.RestResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/public-api/v1")
public class HealthCheckApi {

    @GetMapping("/health")
    public RestResult health() {
        log.debug("I'm debug log!");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("admin", "true");
        data.put("now", LocalDateTime.now());
        return new RestResult(data);
    }

    @GetMapping("/chrome-extensions")
    public RestResult chromeExtensions() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("admin", "true");
        data.put("now", LocalDateTime.now());
        return new RestResult(data);
    }
}
