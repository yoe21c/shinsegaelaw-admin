
package com.tbm.admin.service.sender;

import com.tbm.admin.model.entity.AccountInfo;
import com.tbm.admin.model.message.AgentMessage;
import com.tbm.admin.service.persist.AccountInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.tbm.admin.config.RabbitMqConfig.AGENT_RESULT_ROUTING_KEY;

@SpringBootTest
public class SenderTests {
    @Autowired
    private AccountInfoService accountInfoService;

    @Autowired
    private MqMessageService mqMessageService;

    @Test
    public void sendAgentMessageTest() {
        final AccountInfo accountInfo = accountInfoService.getAccountInfo(42L);
        String blogUrl = "https://blog.naver.com/innauui/223435412872";

        AgentMessage agentMessage = new AgentMessage();
        agentMessage.setScrapQueueSeq(42L);
        agentMessage.setRoutingKey("agent-" + accountInfo.getIpAddress());
        agentMessage.setId(accountInfo.getId());
        agentMessage.setPassword(accountInfo.getPassword());
        agentMessage.setIpAddress(accountInfo.getIpAddress());
        agentMessage.setBlogUrl(blogUrl);
        agentMessage.setIpAddress(accountInfo.getIpAddress());
        agentMessage.setLimitRetryCount(3);
        agentMessage.setResultRoutingKey(AGENT_RESULT_ROUTING_KEY);

        mqMessageService.sendRequestMessage(agentMessage);
    }

}