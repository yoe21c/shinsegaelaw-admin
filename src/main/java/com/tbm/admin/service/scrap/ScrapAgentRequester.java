package com.tbm.admin.service.scrap;

import com.tbm.admin.model.entity.AccountInfo;
import com.tbm.admin.model.entity.ScrapQueue;
import com.tbm.admin.model.message.AgentMessage;
import com.tbm.admin.service.persist.AccountInfoService;
import com.tbm.admin.service.persist.ConfigService;
import com.tbm.admin.service.persist.ScrapQueueService;
import com.tbm.admin.service.sender.MqMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.tbm.admin.config.RabbitMqConfig.AGENT_RESULT_ROUTING_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapAgentRequester {

    private final AccountInfoService accountInfoService;
    private final ConfigService configService;
    private final ScrapQueueService scrapQueueService;
    private final MqMessageService mqMessageService;

    public void requestScrap() {

        int defaultScrapCount = Integer.parseInt(configService.getConfig("agent.daily.scrap.count.default").getVal());
        int tryCountLimit = Integer.parseInt(configService.getConfig("agent.daily.scrap.count.try-limit").getVal());

        List<AccountInfo> accountInfos = accountInfoService.getAllActiveAccountInfos(defaultScrapCount);

        log.info("scrap request start ! accountInfos size: {}", accountInfos.size());

        // 계정별로 순회하면서 스크랩을 진행한다.
        for (AccountInfo accountInfo : accountInfos) {

            log.debug("account seq: {}, account id: {}, ip: {}, defaultScrapCount: {}, now: {}",
                accountInfo.getSeq(), accountInfo.getId(), accountInfo.getIpAddress(), defaultScrapCount, LocalDateTime.now());

            // 각 계정에 속해 있는 스크랩 큐를 가져와서 하나씩 큐로 던진다.
            final List<ScrapQueue> scrapQueues = scrapQueueService.getAllReadyByAccountSeq(accountInfo.getSeq());
            for (ScrapQueue scrapQueue : scrapQueues) {

                if(StringUtils.isBlank(scrapQueue.getBlogUrl())) {
                    log.error("scrapQueue's blogUrl is empty ! scrapQueueSeq: {}", scrapQueue.getSeq());
                    scrapQueue.setStatus("failed");
                    scrapQueue.setFailedAt(LocalDateTime.now());
                    scrapQueue.setReasons("blogUrl is empty ! Not requested to scrap.");
                    scrapQueueService.save(scrapQueue);
                }else {
                    AgentMessage agentMessage = new AgentMessage();
                    agentMessage.setScrapQueueSeq(scrapQueue.getSeq());
                    agentMessage.setRoutingKey("agent-" + accountInfo.getIpAddress());
                    agentMessage.setId(accountInfo.getId());
                    agentMessage.setPassword(accountInfo.getPassword());
                    agentMessage.setIpAddress(accountInfo.getIpAddress());
                    agentMessage.setBlogUrl(scrapQueue.getBlogUrl());
                    agentMessage.setIpAddress(accountInfo.getIpAddress());
                    agentMessage.setLimitRetryCount(tryCountLimit);
                    agentMessage.setResultRoutingKey(AGENT_RESULT_ROUTING_KEY);

                    scrapQueue.setStatus("processing");
                    scrapQueueService.save(scrapQueue);

                    // fire and forget
                    mqMessageService.sendRequestMessage(agentMessage);
                }
            }
        }
    }
}
