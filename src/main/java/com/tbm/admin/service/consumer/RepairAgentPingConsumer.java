package com.tbm.admin.service.consumer;

import com.tbm.admin.model.entity.RepairAgent;
import com.tbm.admin.model.message.RepairAgentPingMessage;
import com.tbm.admin.service.persist.RepairAgentService;
import com.tbm.admin.service.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.tbm.admin.config.RabbitMqConfig.REPAIR_AGENT_PING_ROUTING_KEY;
import static com.tbm.admin.utils.Utils.toJson;

/**
 * repair agent 가 최초에 실행될 때 본인을 등록하기 위한 엔드포인트 용도의 큐 컨슈머
 */
@Profile("batch-repair")
@Slf4j
@Component
@RequiredArgsConstructor
public class RepairAgentPingConsumer extends MessageListenerAdapter {

    private final TelegramService telegramService;
    private final RepairAgentService repairAgentService;

    @RabbitListener(queues = REPAIR_AGENT_PING_ROUTING_KEY, concurrency = "1")
    public void handleMessage(RepairAgentPingMessage joinMessage) {

        try {
            log.debug("Received message: {}", toJson(joinMessage));

            final Optional<RepairAgent> repairAgentOptional = repairAgentService.getRepairAgentByIpAddress(joinMessage.getIp());
            if(repairAgentOptional.isPresent()) {
                final RepairAgent repairAgent = repairAgentOptional.get();
                repairAgent.setPingAt(LocalDateTime.now());
                repairAgentService.save(repairAgent);
                log.info("Success RepairAgent Ping : {}", joinMessage.getIp());
            }

        }catch (Exception e) {
            log.error("RepairAgent Ping is failed, error: {}", e.getMessage(), e);
            telegramService.sendTelegram("!!! RepairAgent Ping is failed " + ", error: " + e.getMessage());
        }

    }

}