package com.tbm.admin.service.consumer;

import com.tbm.admin.model.entity.ScrapQueue;
import com.tbm.admin.model.entity.ScrapUrl;
import com.tbm.admin.model.message.AgentMessage;
import com.tbm.admin.service.persist.ScrapQueueService;
import com.tbm.admin.service.persist.ScrapUrlService;
import com.tbm.admin.service.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.tbm.admin.config.RabbitMqConfig.AGENT_RESULT_ROUTING_KEY;
import static com.tbm.admin.utils.Utils.toJson;

@Profile("consumer")
@Slf4j
@Component
@RequiredArgsConstructor
public class ScrapResultConsumer extends MessageListenerAdapter {

    private final TelegramService telegramService;
    private final ScrapQueueService scrapQueueService;
    private final ScrapUrlService scrapUrlService;

    @RabbitListener(queues = AGENT_RESULT_ROUTING_KEY, concurrency = "1")
    public void handleMessage(AgentMessage agentMessage) {

        ScrapQueue scrapQueue = null;
        try {
            log.info("Received message: {}", toJson(agentMessage));

            final Optional<ScrapQueue> scrapQueueOptional = scrapQueueService.getScrapQueueBySeq(agentMessage.getScrapQueueSeq());

            if(scrapQueueOptional.isEmpty()) {
                log.error("[quit] ScrapQueue is not exist. seq: {}", agentMessage.getScrapQueueSeq());
                telegramService.sendTelegram("[quit] ScrapQueue is not exist. seq: " + agentMessage.getScrapQueueSeq());
                return;
            }

            scrapQueue = scrapQueueOptional.get();

            final ScrapUrl scrapUrl = scrapUrlService.getScrapUrl(scrapQueue.getUrlSeq());
            scrapUrl.setCount(scrapUrl.getCount() + 1);

            if(agentMessage.isSuccess()) {
                scrapQueue.setStatus("completed");
                scrapQueue.setCompletedAt(LocalDateTime.now());

                log.info("ScrapQueue is completed. seq: {}, count: {}, target: {}",
                    agentMessage.getScrapQueueSeq(), scrapUrl.getCount(), scrapUrl.getTargetCount());

                if(scrapUrl.getTargetCount() <= scrapUrl.getCount()) {

                    scrapUrl.setStatus("closed");
                    scrapUrl.setClosedAt(LocalDateTime.now());
                    scrapUrl.setDescription("최종성공");

                    log.info("[success] ScrapUrl is closed. seq: {}", scrapQueue.getUrlSeq());
                }
            }else {
                scrapQueue.setStatus("failed");
                scrapQueue.setFailedAt(LocalDateTime.now());
                log.error("ScrapQueue is failed. seq: {}", agentMessage.getScrapQueueSeq());

                if(scrapUrl.getTargetCount() <= scrapUrl.getCount()) {

                    scrapUrl.setStatus("closed");
                    scrapUrl.setClosedAt(LocalDateTime.now());
                    scrapUrl.setDescription("실패로 인한 종료");

                    log.info("[failure] ScrapUrl is closed. seq: {}", scrapQueue.getUrlSeq());
                }
            }
            scrapQueueService.save(scrapQueue);
            scrapUrlService.save(scrapUrl);

            log.info("[final] Complete ScrapQueue. seq: {}", agentMessage.getScrapQueueSeq());

        }catch (Exception e) {

            if(scrapQueue != null) {
                scrapQueue.setStatus("failed");
                scrapQueue.setFailedAt(LocalDateTime.now());
                scrapQueue.setReasons(e.getMessage());
                scrapQueueService.save(scrapQueue);

                telegramService.sendTelegram("!!! ScrapQueue is failed. seq: " + agentMessage.getScrapQueueSeq() + ", error: " + e.getMessage());
            }
        }

    }

}