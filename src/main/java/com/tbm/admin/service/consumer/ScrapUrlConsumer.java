package com.tbm.admin.service.consumer;

import com.tbm.admin.exception.TbmAdminRuntimeException;
import com.tbm.admin.model.entity.AccountInfo;
import com.tbm.admin.model.entity.ScrapQueue;
import com.tbm.admin.model.entity.ScrapUrl;
import com.tbm.admin.model.message.ConsumerResult;
import com.tbm.admin.model.message.ScrapUrlMessage;
import com.tbm.admin.model.view.base.ScrapUrlView;
import com.tbm.admin.repository.AccountInfoRepository;
import com.tbm.admin.repository.ScrapQueueRepository;
import com.tbm.admin.service.persist.AccountInfoService;
import com.tbm.admin.service.persist.ScrapUrlService;
import com.tbm.admin.service.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.tbm.admin.config.RabbitMqConfig.SCRAP_URL_ROUTING_KEY;
import static com.tbm.admin.utils.Utils.toJson;

@Profile("consumer")
@Slf4j
@Component
@RequiredArgsConstructor
public class ScrapUrlConsumer extends MessageListenerAdapter {

    private final TelegramService telegramService;
    private final ScrapUrlService scrapUrlService;
    private final AccountInfoRepository accountInfoRepository;
    private final ScrapQueueRepository scrapQueueRepository;
    private final AccountInfoService accountInfoService;

    @RabbitListener(queues = SCRAP_URL_ROUTING_KEY, concurrency = "1")
    public ConsumerResult handleMessage(ScrapUrlMessage scrapUrlMessage) {

        try {
            log.info("Received message upsertUrlList: {}", toJson(scrapUrlMessage));

            List<ScrapUrlView> newVies = new ArrayList<>();
            for (ScrapUrlView view : scrapUrlMessage.getScrapUrlViews()) {
                if(StringUtils.isBlank(view.getBlogUrl())) {
                    log.warn("blogUrl is empty. skip : {}", toJson(view));
                    continue;
                }
                newVies.add(view);
            }

            final Integer sumOfTargetCount = newVies.stream().reduce(0, (acc, view) -> acc + view.getTargetCount(), Integer::sum);

            final int availableAccountInfoCount = accountInfoService.countAvailableAccountInfo();

            if(availableAccountInfoCount < sumOfTargetCount) {
                throw new TbmAdminRuntimeException("가능한 최대 목표 스크랩수를 초과하였습니다. 가능한 최대 목표 스크랩수 : "
                                                   + availableAccountInfoCount + ", 등록한 합계 목표 스크랩수 : " + sumOfTargetCount);
            }

            List<ScrapUrl> updateList = new ArrayList<>();
            List<ScrapUrl> createList = new ArrayList<>();
            List<ScrapUrl> deleteList = new ArrayList<>();

            // fixme ! deleteList !

            // 만약에 3개의 URL을 입력하였다 하면 3번을 순회한다.
            for (ScrapUrlView view : newVies) {
                ScrapUrl scrapUrl = scrapUrlService.getScrapUrl(view.getSeq());
                scrapUrl.setMemberSeq(scrapUrlMessage.getAdminSeq());
                scrapUrl.setStartAt(view.getStartAt());
                scrapUrl.setEndAt(view.getStartAt().plusDays(1));
                scrapUrl.setBlogUrl(view.getBlogUrl());
                scrapUrl.setActivate("Y");
                scrapUrl.setTargetCount(view.getTargetCount());
                scrapUrl.setCreatedAt(LocalDateTime.now());
                scrapUrl.setUpdatedAt(LocalDateTime.now());
                final ScrapUrl saved = scrapUrlService.save(scrapUrl);

                if(scrapUrl.isCreatedUrl()) {
                    createList.add(saved);
                }else {
                    updateList.add(saved);
                }
            }

            if( ! createList.isEmpty()) {
                insertScrapQueue(createList, scrapUrlMessage.getAdminSeq());
            }

            if( ! updateList.isEmpty()) {
                updateScrapQueue(updateList);
            }

            final ConsumerResult consumerResult = new ConsumerResult();
            consumerResult.setResult("success");
            return consumerResult;

        }catch (Exception e) {
            final ConsumerResult consumerResult = new ConsumerResult();
            consumerResult.setResult("failure");
            consumerResult.setMessage(e.getMessage());

            log.error("Failure handleMessage ! error: {}", e.getMessage());

            telegramService.sendTelegram("ScrapUrlConsumer ! Failure handleMessage ! error: " + e.getMessage());

            return consumerResult;
        }

    }

    public void insertScrapQueue (List<ScrapUrl> createList, Long adminSeq) {

        List<ScrapUrl> flatTotalScrapUrlList = new ArrayList<>();
        for (ScrapUrl scrapUrl : createList) {
            for (int i = 0; i < scrapUrl.getTargetCount(); i++) {
                scrapUrl.setBlogUrl(org.springframework.util.StringUtils.trimAllWhitespace(scrapUrl.getBlogUrl()));
                flatTotalScrapUrlList.add(scrapUrl);
            }
        }

        log.info("flatTotalScrapUrlList createList : {}", toJson(flatTotalScrapUrlList));

        int scrapTargetCount = flatTotalScrapUrlList.size();      // url 수 x targetCount = 3 x 5 = 15

        List<AccountInfo> accountInfoList = accountInfoRepository.findAccountInfos();

        List<AccountInfo> executeAccountList = new ArrayList<>();
        int count = 1;
        do {
            count = run(count, accountInfoList, executeAccountList, scrapTargetCount);
        } while (executeAccountList.size() < scrapTargetCount);

        // 최종 매핑하여 ScrapQueue 생성
        for (int i = 0; i < executeAccountList.size(); i++) {
            final AccountInfo accountInfo = executeAccountList.get(i);
            final ScrapUrl scrapUrl = flatTotalScrapUrlList.get(i);
            ScrapQueue scrapQueue = new ScrapQueue();
            scrapQueue.setMemberSeq(adminSeq);
            scrapQueue.setUrlSeq(scrapUrl.getSeq());
            scrapQueue.setBlogUrl(scrapUrl.getBlogUrl());
            scrapQueue.setStatus("ready");
            scrapQueue.setAccountSeq(accountInfo.getSeq());
            scrapQueue.setAccountId(accountInfo.getId());
            scrapQueue.setAccountNickname(accountInfo.getNickname());
            scrapQueue.setIpAddress(accountInfo.getIpAddress());
            scrapQueue.setStartAt(scrapUrl.getStartAt());
            scrapQueue.setEndAt(scrapUrl.getEndAt());
            scrapQueue.setTryCount(0);
            scrapQueue.setCreatedAt(LocalDateTime.now());
            scrapQueue.setUpdatedAt(LocalDateTime.now());
            scrapQueueRepository.save(scrapQueue);

            log.info("ScrapQueue Created ! count: {}, scrapQueue: {}", i+1, toJson(scrapQueue));
        }

    }

    private int run(int count, List<AccountInfo> accountInfoList, List<AccountInfo> executeList, int targetCount) {

        for (AccountInfo accountInfo : accountInfoList) {
            if(accountInfo.getDailyCountLimit() > accountInfo.getDailyCount()) {

                executeList.add(accountInfo);

                int beforeDailyCount = accountInfo.getDailyCount();

                accountInfo.setDailyCount(accountInfo.getDailyCount() + 1);

                log.info("Daily Count increase ! count: {}, accountSeq: {}, accountId: {}, targetCount: {}, dailyCount: {} -> {}",
                    count, accountInfo.getSeq(), accountInfo.getId(), targetCount, beforeDailyCount, accountInfo.getDailyCount());

                count++;

                // dailyCount 를 증가시켰으면 저장해야한다. @2024-08-03
                accountInfoService.save(accountInfo);
            }
            if(executeList.size() >= targetCount) {
                break;
            }
        }

        return count;
    }

    public void updateScrapQueue (List<ScrapUrl> updateList) {

        for (ScrapUrl scrapUrl : updateList) {
            final List<ScrapQueue> scrapQueues = scrapQueueRepository.findByUrlSeq(scrapUrl.getSeq());

            for (ScrapQueue scrapQueue : scrapQueues) {
                scrapQueue.setStartAt(scrapUrl.getStartAt());
                scrapQueue.setEndAt(scrapUrl.getEndAt());
                scrapQueueRepository.save(scrapQueue);
            }
        }

    }

}