package com.tbm.admin.service.consumer;

import com.tbm.admin.model.entity.RepairAgent;
import com.tbm.admin.model.message.RepairAgentJoinMessage;
import com.tbm.admin.service.persist.RepairAgentService;
import com.tbm.admin.service.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.tbm.admin.config.RabbitMqConfig.REPAIR_AGENT_JOIN_ROUTING_KEY;
import static com.tbm.admin.utils.Utils.toJson;

/**
 * repair agent 가 최초에 실행될 때 본인을 등록하기 위한 엔드포인트 용도의 큐 컨슈머
 */
@Profile("batch-repair")
@Slf4j
@Component
@RequiredArgsConstructor
public class RepairAgentJoinConsumer extends MessageListenerAdapter {

    private final TelegramService telegramService;
    private final RepairAgentService repairAgentService;

    @RabbitListener(queues = REPAIR_AGENT_JOIN_ROUTING_KEY, concurrency = "1")
    public void handleMessage(RepairAgentJoinMessage joinMessage) {

        try {
            log.info("Received message: {}", toJson(joinMessage));

            final Optional<RepairAgent> repairAgentOptional = repairAgentService.getRepairAgentByIpAddress(joinMessage.getIp());
            if(repairAgentOptional.isPresent()) {
                telegramService.sendTelegram("SKIP RepairAgent Join : " + joinMessage.getIp());
            }else {

                RepairAgent repairAgent = new RepairAgent();
                repairAgent.setStatus("inactive");
                repairAgent.setIpAddress(joinMessage.getIp());
                repairAgent.setMacAddress(joinMessage.getMac());
                repairAgent.setDescription("최초 등록하여 블로그 ID를 등록해야합니다.");
                repairAgentService.save(repairAgent);
                telegramService.sendTelegram("Success RepairAgent Join : " + joinMessage.getIp());

                log.info("Success RepairAgent Join : {}", joinMessage.getIp());
            }

            log.info("[final] Complete ScrapQueue. seq: {}", joinMessage.getIp());

        }catch (Exception e) {
            log.error("RepairAgent Join is failed, error: {}", e.getMessage(), e);
            telegramService.sendTelegram("!!! RepairAgent Join is failed " + ", error: " + e.getMessage());
        }

    }

}