package com.tbm.admin.service.sender;

import com.tbm.admin.exception.TbmAdminRuntimeException;
import com.tbm.admin.model.entity.RepairAgentQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.tbm.admin.utils.Utils.toObject;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqRepairAgentMessageService {

    @Autowired
    @Qualifier("repairRabbitTemplate")
    private RabbitTemplate repairRabbitTemplate;

    @Value("${repair.key}")
    private String secretKey;

    private String encryptCode(String code) {

        try {
            SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(code.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);

        }catch (Exception e) {
            log.error("encryptCode error ! message: {}", e.getMessage(), e);
            throw new TbmAdminRuntimeException("encryptCode error ! ");
        }
    }

    /**
     * Queue로 메시지를 발행
     *
     * @param sourceCode 발행할 메시지의 DTO 객체
     * @param repairAgentQueue Queue 정보
     * @param repairType repair type
     */
    public void sendRequestMessage(String sourceCode, RepairAgentQueue repairAgentQueue, String repairType) {
        final String encryptedCode = encryptCode(sourceCode);

        final String routingKey = "repair-request-" + repairAgentQueue.getIpAddress();

        log.info("routingKey: {}, message sent. size: {}", routingKey, encryptedCode.length());

        // MessagePostProcessor를 사용해서 MessageProperties를 조작
        MessagePostProcessor messagePostProcessor = message -> {
            message.getMessageProperties().setHeader("repairType", repairType);
            message.getMessageProperties().setHeader("repairAgentQueueSeq", repairAgentQueue.getSeq());
            return message;
        };

        repairRabbitTemplate.convertAndSend("repair-agent", routingKey, encryptedCode, messagePostProcessor);
    }

}