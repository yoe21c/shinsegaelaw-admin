package com.tbm.admin.batch;

import com.tbm.admin.model.entity.RepairAgentQueue;
import com.tbm.admin.model.entity.RepairCode;
import com.tbm.admin.model.message.RepairResponse;
import com.tbm.admin.service.persist.RepairAgentQueueService;
import com.tbm.admin.service.persist.RepairCodeService;
import com.tbm.admin.service.sender.MqRepairAgentMessageService;
import com.tbm.admin.service.thirdparty.BlogTitleExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static com.tbm.admin.utils.Utils.toJson;

@Profile("batch-repair")
@Slf4j
@Component
@RequiredArgsConstructor
public class RepairAgentBatch {

    private final RepairAgentQueueService repairAgentQueueService;
    private final RepairCodeService repairCodeService;
    private final MqRepairAgentMessageService mqRepairAgentMessageService;
    private final BlogTitleExtractor blogTitleExtractor;

    // url 예약 상태 변경 - 1초마다
    @Scheduled(cron = "0/1 * * * * *")
    public void repairAgentBatch() {

        final RepairCode repairCodeContent = repairCodeService.getRepairCodeByName("repair_agent_20250224_windows_edge");
        final String sourceCodeContent = repairCodeContent.getSourceCode();

        final RepairCode repairCodeTitle = repairCodeService.getRepairCodeByName("repair_agent_title_20250224_windows_edge");
        final String sourceCodeTitle = repairCodeTitle.getSourceCode();

        List<RepairAgentQueue> repairAgentQueues = repairAgentQueueService.candidateRepairAgents();
        for (RepairAgentQueue one : repairAgentQueues) {
            one.setStatus("processing");
            one.setProcessingAt(LocalDateTime.now());
            repairAgentQueueService.save(one);

            // 타이틀을 수정하는 경우를 처리한다.
            if(StringUtils.isNotBlank(one.getTitle())) {

                repairTitle(one, sourceCodeTitle, blogTitleExtractor.extractTitle(one.getBlogUrl()));
            }

            // 본문을 수정하는 경우를 처리한다.
            if(StringUtils.isNotBlank(one.getContent())) {

                repairContent(one, sourceCodeContent);
            }

            log.info("repairAgent 에 요청을 보냄: {}", toJson(one));

        }
    }

    private void repairContent(RepairAgentQueue one, String sourceCodeContent) {
        String formatedSourceCode = sourceCodeContent
            .replace("%%BLOG_URL%%", one.getBlogUrl())
            .replace("%%LINE_NUMBER%%", String.valueOf(one.getLineNumber()))
            .replace("%%REPLACE_CONTENT%%", one.getContent())
            .replace("%%SEARCH_TEXT%%", one.getSearchText())
            .replace("%%SEQ%%", String.valueOf(one.getSeq()));

        log.info("---> 본문 수정 Request {}, ipAddress: {}, blogUrl: {}, lineNumber: {}, searchText: {}",
            one.getSeq(), one.getIpAddress(), one.getBlogUrl(), one.getLineNumber(), one.getSearchText());

        mqRepairAgentMessageService.sendRequestMessage(formatedSourceCode, one, "repair-content");
    }

    private void repairTitle(RepairAgentQueue one, String sourceCodeTitle, String currentTitle) {
        String formatedSourceCode = sourceCodeTitle
            .replace("%%BLOG_URL%%", one.getBlogUrl())
            .replace("%%REPLACE_CONTENT%%", one.getTitle())
            .replace("%%SEARCH_TEXT%%", currentTitle)
            .replace("%%SEQ%%", String.valueOf(one.getSeq()));

        log.info("---> 타이틀 수정 Request {}, ipAddress: {}, blogUrl: {}, searchText(title): {}",
            one.getSeq(), one.getIpAddress(), one.getBlogUrl(), currentTitle);

        mqRepairAgentMessageService.sendRequestMessage(formatedSourceCode, one, "repair-title");
    }

}
