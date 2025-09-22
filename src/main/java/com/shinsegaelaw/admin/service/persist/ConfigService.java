package com.shinsegaelaw.admin.service.persist;

import com.shinsegaelaw.admin.model.entity.Config;
import com.shinsegaelaw.admin.repository.ConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigService {
    private final ConfigRepository configRepository;

    public Config getConfig(String id) {
        return configRepository.findById(id).orElseThrow(() -> new RuntimeException("Config not found"));
    }

    public Optional<Config> getConfigOptional(String id) {
        return configRepository.findById(id);
    }

    public void updateConfig(Config config) {
        configRepository.save(config);
    }

    @Transactional
    public void updateConfig(String key, String message) {
        getConfig(key).setVal(message);
    }
}