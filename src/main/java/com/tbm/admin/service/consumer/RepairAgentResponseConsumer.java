package com.tbm.admin.service.consumer;

import com.tbm.admin.model.entity.RepairAgent;
import com.tbm.admin.model.entity.RepairAgentQueue;
import com.tbm.admin.model.message.RepairAgentPingMessage;
import com.tbm.admin.model.message.RepairResponse;
import com.tbm.admin.service.persist.RepairAgentQueueService;
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
import static com.tbm.admin.config.RabbitMqConfig.REPAIR_AGENT_RESPONSE_ROUTING_KEY;
import static com.tbm.admin.utils.Utils.toJson;

/**
 * repair agent 가 최초에 실행될 때 본인을 등록하기 위한 엔드포인트 용도의 큐 컨슈머
 * todo 여기에서 완전 비동기형태로 저장하고 처리하도록 한다.
 */
@Profile("batch-repair")
@Slf4j
@Component
@RequiredArgsConstructor
public class RepairAgentResponseConsumer extends MessageListenerAdapter {

    private final TelegramService telegramService;
    private final RepairAgentQueueService repairAgentQueueService;

    @RabbitListener(queues = REPAIR_AGENT_RESPONSE_ROUTING_KEY, concurrency = "1")
    public void handleMessage(RepairResponse repairResponse) {

        try {
            log.info("Received repairResponse message: {}", toJson(repairResponse));

            final RepairAgentQueue one = repairAgentQueueService.getRepairAgentQueue(repairResponse.getRepairAgentQueueSeq());

            // 성공
            if(repairResponse.getResult().equals("ok")) {

                one.setStatus("completed");
                one.setCompletedAt(LocalDateTime.now());
                repairAgentQueueService.save(one);

                if(repairResponse.getRepairType().equals("repair-title")) {
                } else if(repairResponse.getRepairType().equals("repair-content")) {
                }

                log.info("<--- 성공(repairType) : {}, 수정 Response {}, ipAddress: {}, blogUrl: {}, lineNumber: {}, searchText: {}, result: {}",
                    repairResponse.getRepairType(), one.getSeq(), one.getIpAddress(), one.getBlogUrl(), one.getLineNumber(), one.getSearchText(), toJson(repairResponse));
            }else {

                one.setStatus("failed");
                one.setFailReason(repairResponse.getMessage());
                one.setFailedAt(LocalDateTime.now());
                repairAgentQueueService.save(one);

                if(repairResponse.getRepairType().equals("repair-title")) {
                } else if(repairResponse.getRepairType().equals("repair-content")) {
                }

                log.error("<--- 실패(repairType) : {}, 수정 Response {}, ipAddress: {}, blogUrl: {}, lineNumber: {}, searchText: {}, result: {}",
                    repairResponse.getRepairType(), one.getSeq(), one.getIpAddress(), one.getBlogUrl(), one.getLineNumber(), one.getSearchText(), toJson(repairResponse));
            }

        }catch (Exception e) {
            log.error("RepairAgent Response is failed, error: {}", e.getMessage(), e);
            telegramService.sendTelegram("!!! RepairAgent Response is failed " + ", error: " + e.getMessage());
        }

    }

}