package com.shinsegaelaw.admin.service.front;

import com.shinsegaelaw.admin.model.entity.Config;
import com.shinsegaelaw.admin.model.param.MainMessage;
import com.shinsegaelaw.admin.model.view.rest.RestResult;
import com.shinsegaelaw.admin.service.persist.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MainHelloFrontService {

    private final ConfigService configService;

    public String mainHello() {
        final Config config = configService.getConfig("main_hello");
        return config.getVal();
    }

    public RestResult upsertMessage(MainMessage mainMessage) {
        configService.updateConfig("main_hello", mainMessage.getMessage());
        return RestResult.success();
    }
}
