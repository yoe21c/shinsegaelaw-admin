package com.tbm.admin.service.queue;

import com.tbm.admin.model.entity.AccountInfo;
import com.tbm.admin.model.entity.ScrapQueue;
import com.tbm.admin.model.entity.ScrapUrl;
import com.tbm.admin.repository.AccountInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootTest
public class QueuingTests {

    @Autowired
    AccountInfoRepository accountInfoRepository;

    @Test
    public void test() {
        System.out.println("QueuingTests.test");


        List<AccountInfo> accountInfoList = accountInfoRepository.findAccountInfos();

        int targetCount = 100;

        List<AccountInfo> executeList = new ArrayList<>();
        for (int i = 0; i < targetCount; i++) {
            for (AccountInfo accountInfo : accountInfoList) {
                if(accountInfo.getDailyCountLimit() > accountInfo.getDailyCount()) {
                    executeList.add(accountInfo);
                    accountInfo.setDailyCount(accountInfo.getDailyCount() + 1);
                    break;
                }
            }
        }

        System.out.println("executeList.size() = " + executeList.size());
        for (AccountInfo accountInfo : executeList) {
            System.out.println("accountInfo = " + accountInfo);
        }
    }

    @Test
    public void test2() {

        int targetCount = 20;

        List<ScrapUrl> scrapUrlList = new ArrayList<>();
        scrapUrlList.add(new ScrapUrl("https://blog.naver.com/alduddlah/1111111", targetCount));
        scrapUrlList.add(new ScrapUrl("https://blog.naver.com/alduddlah/2222222", targetCount));
        scrapUrlList.add(new ScrapUrl("https://blog.naver.com/alduddlah/3333333", targetCount));

        List<ScrapUrl> flatTotalScrapUrlList = new ArrayList<>();
        for (ScrapUrl scrapUrl : scrapUrlList) {
            for (int i = 0; i < scrapUrl.getTargetCount(); i++) {
                flatTotalScrapUrlList.add(scrapUrl);
            }
        }

        int scrapTargetCount = flatTotalScrapUrlList.size();      // url 수 x targetCount = 3 x 5 = 15

        List<AccountInfo> accountInfoList = accountInfoRepository.findAccountInfos();

        List<AccountInfo> executeAccountList = new ArrayList<>();
        do {
            run(accountInfoList, executeAccountList, scrapTargetCount);
        } while (executeAccountList.size() < scrapTargetCount);

        // 최종 매핑하여 ScrapQueue 생성
        List<ScrapQueue> finalScrapQueueList = new ArrayList<>();
        for (int i = 0; i < executeAccountList.size(); i++) {
            final ScrapUrl scrapUrl = flatTotalScrapUrlList.get(i);
            ScrapQueue scrapQueue = new ScrapQueue();
            scrapQueue.setBlogUrl(scrapUrl.getBlogUrl());
            scrapQueue.setAccountId(executeAccountList.get(i).getId());
            scrapQueue.setAccountNickname(executeAccountList.get(i).getNickname());
            finalScrapQueueList.add(scrapQueue);
        }

        // final
        for (ScrapQueue scrapQueue : finalScrapQueueList) {
            System.out.println("scrapQueue = [" + scrapQueue + "]");
        }
    }

    private static void run(List<AccountInfo> accountInfoList, List<AccountInfo> executeList, int targetCount) {
        for (AccountInfo accountInfo : accountInfoList) {
            if(accountInfo.getDailyCountLimit() > accountInfo.getDailyCount()) {
                executeList.add(accountInfo);
                accountInfo.setDailyCount(accountInfo.getDailyCount() + 1);
            }
            if(executeList.size() >= targetCount) {
                break;
            }
        }
    }
}
