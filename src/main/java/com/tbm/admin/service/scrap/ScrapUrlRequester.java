package com.tbm.admin.service.scrap;

import com.tbm.admin.exception.TbmAdminRuntimeException;
import com.tbm.admin.model.entity.AccountInfo;
import com.tbm.admin.model.entity.AdminMember;
import com.tbm.admin.model.entity.ScrapQueue;
import com.tbm.admin.model.entity.ScrapUrl;
import com.tbm.admin.model.message.AgentMessage;
import com.tbm.admin.model.message.ConsumerResult;
import com.tbm.admin.model.message.ScrapUrlMessage;
import com.tbm.admin.model.view.base.ScrapUrlView;
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
import static com.tbm.admin.utils.Utils.toJson;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapUrlRequester {

    private final MqMessageService mqMessageService;

    public void requestScrapUrl(List<ScrapUrlView> scrapUrlViews, AdminMember adminMember) {

        ScrapUrlMessage scrapUrlMessage = new ScrapUrlMessage();
        scrapUrlMessage.setScrapUrlViews(scrapUrlViews);
        scrapUrlMessage.setAdminSeq(adminMember.getSeq());

        // fire and forget
        final ConsumerResult consumerResult = mqMessageService.sendAndReceiveRequestMessage(scrapUrlMessage);
        if( consumerResult == null) {
            log.error("Failed to request scrap url ! scrapUrlMessage : {}", toJson(scrapUrlMessage));
            throw new TbmAdminRuntimeException("Failed to request scrap url");
        }else if( ! consumerResult.getResult().equalsIgnoreCase("success")) {
            throw new TbmAdminRuntimeException("Failed to request scrap url");
        }
    }
}
