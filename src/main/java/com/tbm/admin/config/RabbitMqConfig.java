package com.tbm.admin.config;

import com.tbm.admin.config.properties.RabbitServerProperties;
import com.tbm.admin.service.consumer.RepairAgentJoinConsumer;
import com.tbm.admin.service.consumer.RepairAgentResponseConsumer;
import com.tbm.admin.service.consumer.ScrapResultConsumer;
import com.tbm.admin.service.consumer.ScrapUrlConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import static com.tbm.admin.utils.Utils.toJson;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RabbitMqConfig {

    public static final String AGENT_RESULT_ROUTING_KEY = "agent-scrap-result";
    public static final String SCRAP_URL_ROUTING_KEY = "scrap-url-routing";
    public static final String REPAIR_AGENT_JOIN_ROUTING_KEY = "repair-agent-join";
    public static final String REPAIR_AGENT_PING_ROUTING_KEY = "repair-agent-ping";
    public static final String REPAIR_AGENT_RESPONSE_ROUTING_KEY = "repair-response";

    private final RabbitServerProperties rabbitServerProperties;

    @Bean
    public DirectExchange growExchange() {
        return new DirectExchange("grow");
    }

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(rabbitServerProperties.getHost());
        connectionFactory.setPort(rabbitServerProperties.getPort());
        connectionFactory.setUsername(rabbitServerProperties.getUsername());
        connectionFactory.setPassword(rabbitServerProperties.getPassword());
        return connectionFactory;
    }

    /**
     * RabbitTemplate을 생성하여 반환
     *
     * @param connectionFactory RabbitMQ와의 연결을 위한 ConnectionFactory 객체
     * @return RabbitTemplate 객체
     */
    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        // JSON 형식의 메시지를 직렬화하고 역직렬할 수 있도록 설정
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

    /**
     * Jackson 라이브러리를 사용하여 메시지를 JSON 형식으로 변환하는 MessageConverter 빈을 생성
     *
     * @return MessageConverter 객체
     */
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 메시지 컨버터 공통 메소드
     * @param object
     * @return
     */
    public Message buildSimpleConverterMessage(Object object)
    {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setContentEncoding("utf-8");
        messageProperties.setMessageId(java.util.UUID.randomUUID().toString());
        return new Message(toJson(object).getBytes(), messageProperties);
    }

    // part-1 : AGENT_RESULT_ROUTING_KEY

    @Bean
    public Queue agentQueue() {
        return new Queue(AGENT_RESULT_ROUTING_KEY);
    }

    @Bean
    public Binding agentBinding(@Qualifier("growExchange") DirectExchange exchange) {
        return BindingBuilder.bind(agentQueue()).to(exchange).with(AGENT_RESULT_ROUTING_KEY);
    }

    @Profile("consumer")
    @Bean
    public MessageListenerAdapter agentConsumerAdapter(ScrapResultConsumer consumer) {
        return new MessageListenerAdapter(consumer, "handleMessage");
    }

    // part-2 : SCRAP_URL_ROUTING_KEY
    @Bean
    public Queue scrapUrlQueue() {
        return new Queue(SCRAP_URL_ROUTING_KEY);
    }

    @Bean
    public Binding scrapUrlBinding(@Qualifier("growExchange") DirectExchange exchange) {
        return BindingBuilder.bind(scrapUrlQueue()).to(exchange).with(SCRAP_URL_ROUTING_KEY);
    }

    @Profile("consumer")
    @Bean
    public MessageListenerAdapter scrapUrlConsumerAdapter(ScrapUrlConsumer consumer) {
        return new MessageListenerAdapter(consumer, "handleMessage");
    }

    /**********************************************************
     * Repair Agent 관련 큐 설정
     **********************************************************/

    // part-3 : REPAIR_AGENT_JOIN_ROUTING_KEY
    @Bean
    public DirectExchange repairAgentExchange() {
        return new DirectExchange("repair-agent");
    }

    @Bean
    public Queue repairAgentJoinQueue() {
        return new Queue(REPAIR_AGENT_JOIN_ROUTING_KEY);
    }

    @Bean
    public Binding repairAgentJoinBinding(@Qualifier("repairAgentExchange") DirectExchange exchange) {
        return BindingBuilder.bind(repairAgentJoinQueue()).to(exchange).with(REPAIR_AGENT_JOIN_ROUTING_KEY);
    }

    @Profile("batch-repair")
    @Bean
    public MessageListenerAdapter repairAgentJoinConsumerAdapter(RepairAgentJoinConsumer consumer) {
        return new MessageListenerAdapter(consumer, "handleMessage");
    }

    // part-4 : REPAIR_AGENT_PING_ROUTING_KEY
    @Bean
    public Queue repairAgentPingQueue() {
        return new Queue(REPAIR_AGENT_PING_ROUTING_KEY);
    }

    @Bean
    public Binding repairAgentPingBinding(@Qualifier("repairAgentExchange") DirectExchange exchange) {
        return BindingBuilder.bind(repairAgentPingQueue()).to(exchange).with(REPAIR_AGENT_PING_ROUTING_KEY);
    }

    @Profile("batch-repair")
    @Bean
    public MessageListenerAdapter repairAgentPingConsumerAdapter(RepairAgentJoinConsumer consumer) {
        return new MessageListenerAdapter(consumer, "handleMessage");
    }

    // part-5 : REPAIR_AGENT_RESPONSE_ROUTING_KEY
    @Bean
    public Queue repairAgentResponseQueue() {
        return new Queue(REPAIR_AGENT_RESPONSE_ROUTING_KEY);
    }

    @Bean
    public Binding repairAgentResponseBinding(@Qualifier("repairAgentExchange") DirectExchange exchange) {
        return BindingBuilder.bind(repairAgentResponseQueue()).to(exchange).with(REPAIR_AGENT_RESPONSE_ROUTING_KEY);
    }

    @Profile("batch-repair")
    @Bean
    public MessageListenerAdapter repairAgentResponseConsumerAdapter(RepairAgentResponseConsumer consumer) {
        return new MessageListenerAdapter(consumer, "handleMessage");
    }
}