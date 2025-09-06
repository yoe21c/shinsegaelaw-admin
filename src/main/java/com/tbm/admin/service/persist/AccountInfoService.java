package com.tbm.admin.service.persist;

import com.tbm.admin.exception.TbmAdminRuntimeException;
import com.tbm.admin.model.entity.AccountInfo;
import com.tbm.admin.repository.AccountInfoRepository;
import com.tbm.admin.service.telegram.TelegramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountInfoService {

    private final TelegramService telegramService;
    private final AccountInfoRepository accountInfoRepository;

    // 에이전트 할당
    public Page<AccountInfo> getAllWithoutUnassignedIp(String keyword, Pageable pageable) {
        return accountInfoRepository.findAllWithoutUnassignedIp(keyword, pageable);
    }

    // 에이전트 미할당
    public Page<AccountInfo> getAllWithoutAssignedIp(String keyword, Pageable pageable) {
        return accountInfoRepository.findAllWithoutAssignedIP(keyword, pageable);
    }

    // 전체
    public Page<AccountInfo> getAllWith(String keyword, Pageable pageable) {
        return accountInfoRepository.findAllWith(keyword, pageable);
    }

    // valid 전체
    public List<AccountInfo> getAll() {
        return accountInfoRepository.findAll();
    }

    public AccountInfo findAccountInfo(Long seq) {
        return accountInfoRepository.findById(seq)
                .orElseThrow(() -> new TbmAdminRuntimeException("[Admin] Not Exist url seq : " + seq));
    }

    public AccountInfo getAccountInfo(Long seq) {
        if (seq == null) {
            AccountInfo accountInfo = new AccountInfo();
            accountInfo.setStatus("active");
            accountInfo.setDailyCount(0);
            return accountInfo;
        }
        return findAccountInfo(seq);
    }

    public void save(AccountInfo accountInfo) { accountInfoRepository.save(accountInfo); }

    /**
     * 할당 가능한 계정을 찾아서 ip주소를 할당한다.
     * @param ipAddress
     * @return
     */
    public AccountInfo mappingAccountInfo(String ipAddress) {

        // 할당하러 왔는데 이미 할당이 되어 있으므로 할당하지 않는다.
        final Optional<AccountInfo> accountInfoOptional = accountInfoRepository.findByIpAddress(ipAddress);
        if(accountInfoOptional.isPresent()) {
            final AccountInfo accountInfo = accountInfoOptional.get();
            telegramService.sendTelegram(ipAddress + " 가 " + accountInfo.getId() + "에게 이미 할당되어 있어서 무시합니다...");
            return accountInfoOptional.get();
        }

        final Optional<AccountInfo> assignableAccountInfoOptional = accountInfoRepository.findFirstByStatusAndIpAddressIsNullAndIpAddressIsNot("active", ipAddress);

        // 없어야 만든다. 있으면 이미 할당되어 있으므로 무시하지만 텔레그램은 보내준다.
        if(assignableAccountInfoOptional.isPresent()) {
            final AccountInfo accountInfo = assignableAccountInfoOptional.get();
            telegramService.sendTelegram(ipAddress + " 가 " + accountInfo.getId() + "에게 이미 할당되어 있어서 무시합니다.");
            return accountInfo;
        }

        // 해당 ip 로 할당된 계정이 없으므로 할당되어 있지 않는 아무 계정이나 가져와서 ip를 할당한다.

        final AccountInfo accountInfo = accountInfoRepository.findFirstByStatusAndIdIsNotNullAndIpAddressIsNull("active")
            .orElseThrow(() -> {
                telegramService.sendTelegram("할당 가능한 계정이 없어서 네이버 ipAddress 가 null 인 상태로 네이버 계정 하나를 추가해주세요 !");
                return new TbmAdminRuntimeException("할당 가능한 계정이 없습니다.");
            });

        accountInfo.setIpAddress(ipAddress);
        accountInfo.setDailyCount(0);

        telegramService.sendTelegram(ipAddress + " 가 " + accountInfo.getId() + "에게 할당되었습니다.");

        return accountInfoRepository.save(accountInfo);
    }

    // 계정이 할당된 에이전트 개수
    public int countAssignedAccountInfo() {
        return accountInfoRepository.countAccountInfoByIpAddressNotNull();
    }

    // 활용가능한 에이전트 개수
    public int countAvailableAccountInfo() {
        final List<AccountInfo> accountInfos = accountInfoRepository.findAccountInfos();
        return accountInfos.stream().reduce(0, (acc, view) -> acc + (view.getDailyCountLimit() - view.getDailyCount()), Integer::sum);
    }

    public List<AccountInfo> getAllActiveAccountInfos(int limitDailyCount) {
        return accountInfoRepository.findAllByStatusAndDailyCountLessThan("active", limitDailyCount);
    }

    public void saveAll(ArrayList<AccountInfo> accountInfos) {
        accountInfoRepository.saveAll(accountInfos);
    }

    public AccountInfo getAccountInfoByInstanceId(String instanceId) {
        return accountInfoRepository.findByInstanceId(instanceId).orElseThrow(() -> new TbmAdminRuntimeException("Instance not found"));
    }
}
