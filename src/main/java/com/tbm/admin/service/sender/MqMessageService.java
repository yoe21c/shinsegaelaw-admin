package com.tbm.admin.service.sender;

import com.tbm.admin.config.RabbitMqConfig;
import com.tbm.admin.model.message.AgentMessage;
import com.tbm.admin.model.message.ConsumerResult;
import com.tbm.admin.model.message.ScrapUrlMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqMessageService {

    private final RabbitMqConfig rabbitMqConfig;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Queue로 메시지를 발행
     *
     * @param agentMessage 발행할 메시지의 DTO 객체
     */
    public void sendRequestMessage(AgentMessage agentMessage) {
        Message message = rabbitMqConfig.buildSimpleConverterMessage(agentMessage);
        log.info("message sent: {}", new String(message.getBody()));
        rabbitTemplate.convertAndSend("grow", agentMessage.getRoutingKey(), message);
    }

    /**
     * Queue로 메시지를 발행
     *
     * @param scrapUrlMessage 발행할 메시지의 DTO 객체
     */
    public ConsumerResult sendAndReceiveRequestMessage(ScrapUrlMessage scrapUrlMessage) {
        Message message = rabbitMqConfig.buildSimpleConverterMessage(scrapUrlMessage);
        log.info("send and receive message sent: {}", new String(message.getBody()));
        return (ConsumerResult) rabbitTemplate.convertSendAndReceive("grow", RabbitMqConfig.SCRAP_URL_ROUTING_KEY, message);
    }

}