package com.tbm.admin.service.telegram;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TelegramServiceTest {

    @Autowired
    TelegramService telegramService;

    @Test
    void sendTelegramMessage() {
        // given
        String message = "테스트 메시지! ";

        // when
        telegramService.sendTelegram(message);

        // then
    }
}