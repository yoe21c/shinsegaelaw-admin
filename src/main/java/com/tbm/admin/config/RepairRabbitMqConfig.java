package com.tbm.admin.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RepairRabbitMqConfig {

    @Bean
    public RabbitTemplate repairRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setReplyTimeout(300000);  // 300초 , 기본값은 5000ms, 5초, 응답시간을 90초로 설정, 응답시간을 넘어가면 null을 리턴
        rabbitTemplate.setReceiveTimeout(300000);  // 300초

        // 메시지 컨버터를 Jackson2JsonMessageConverter로 설정
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        return rabbitTemplate;
    }

}