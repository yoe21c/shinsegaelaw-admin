package com.shinsegaelaw.admin.service.front;

import com.shinsegaelaw.admin.exception.TbmAdminRuntimeException;
import com.shinsegaelaw.admin.model.entity.RepairAgent;
import com.shinsegaelaw.admin.model.param.StringMultiValueMapAdapter;
import com.shinsegaelaw.admin.model.view.base.DataTableView;
import com.shinsegaelaw.admin.service.persist.RepairAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepairAgentFrontService {

    private final RepairAgentService repairAgentService;

    public DataTableView getRepairAgents(MultiValueMap<String, String> param, Long memberSeq) {

        int draw = Integer.parseInt(param.get("draw").get(0));
        int start = Integer.parseInt(param.get("start").get(0));
        int length = Integer.parseInt(param.get("length").get(0));
        String search = param.get("search[value]").get(0);

        String keyword = StringUtils.isNotBlank(param.get("keyword").get(0)) ? param.get("keyword").get(0) : "";

        String displayStatus = param.get("status").get(0);

        int pageNumber = (start / length);
        final PageRequest pageable = PageRequest.of(pageNumber, length, Sort.Direction.DESC, "seq");

        final Page<RepairAgent> repairAgentPage = repairAgentService.getRepairAgents(pageable);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("repairAgents", repairAgentPage);

        return new DataTableView(draw, repairAgentPage.getTotalElements(), repairAgentPage.getTotalElements(), data);
    }

    public RepairAgent getRepairAgent(Long seq) {
        return repairAgentService.getRepairAgent(seq);
    }

    public void updateRepairAgent(StringMultiValueMapAdapter param) {
        RepairAgent repairAgent;
        if(param.exist("seq")) {
            Long seq = param.longVal("seq");
            repairAgent = repairAgentService.getRepairAgent(seq);
        }else {
            repairAgent = new RepairAgent();

            // 새로 생성하는데 ip주소가 중복되는 경우에 오류를 리턴한다.
            Optional<RepairAgent> repairAgentOptional = repairAgentService.getRepairAgentByIpAddress(param.stringVal("ipAddress"));
            if(repairAgentOptional.isPresent()) {
                throw new TbmAdminRuntimeException("IP주소가 이미 등록되어 있습니다 : " + param.stringVal("ipAddress"));
            }
        }

        repairAgent.setBlogId(param.stringVal("blogId"));
        repairAgent.setStatus(param.stringVal("status"));
        repairAgent.setIpAddress(param.stringVal("ipAddress"));
        repairAgent.setMacAddress(param.stringVal("macAddress"));
        repairAgent.setDescription(param.stringVal("description"));

        repairAgentService.save(repairAgent);
    }

    public void deleteRepairAgent(Long seq) {
        log.info("deleteRepairAgent seq: {}", seq);
        try {
            repairAgentService.delete(seq);
        }catch (Exception e) {
            log.error("deleteRepairAgent error", e);
            throw new TbmAdminRuntimeException("deleteRepairAgent error");
        }
    }
}
