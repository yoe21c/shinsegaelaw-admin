package com.tbm.admin.service.front;

import com.tbm.admin.controller.page.RepairAgentPage;
import com.tbm.admin.exception.TbmAdminRuntimeException;
import com.tbm.admin.model.entity.RepairAgent;
import com.tbm.admin.model.entity.RepairAgentQueue;
import com.tbm.admin.model.entity.RepairCode;
import com.tbm.admin.model.param.StringMultiValueMapAdapter;
import com.tbm.admin.model.view.base.DataTableView;
import com.tbm.admin.model.view.rest.RestResult;
import com.tbm.admin.service.persist.RepairAgentQueueService;
import com.tbm.admin.service.persist.RepairAgentService;
import com.tbm.admin.service.persist.RepairCodeService;
import com.tbm.admin.utils.ContentUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RepairAgentQueueFrontService {

    private final RepairCodeService repairCodeService;
    private final RepairAgentService repairAgentService;
    private final RepairAgentQueueService repairAgentQueueService;
    private final RepairAgentPage repairAgentPage;

    public DataTableView getRepairAgentQueues(MultiValueMap<String, String> param, Long memberSeq) {

        int draw = Integer.parseInt(param.get("draw").get(0));
        int start = Integer.parseInt(param.get("start").get(0));
        int length = Integer.parseInt(param.get("length").get(0));
        String search = param.get("search[value]").get(0);

        String keyword = StringUtils.isNotBlank(param.get("keyword").get(0)) ? param.get("keyword").get(0) : "";
        String displayStatus = param.get("status").get(0);

//        log.info("keyword : {}, displayStatus : {}", keyword, displayStatus);

        int pageNumber = (start / length);
        final PageRequest pageable = PageRequest.of(pageNumber, length, Sort.Direction.DESC, "seq");

        final Page<RepairAgentQueue> repairAgentQueuePage = repairAgentQueueService.getRepairAgentQueues(pageable, displayStatus, keyword);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("repairAgentQueues", repairAgentQueuePage);

        return new DataTableView(draw, repairAgentQueuePage.getTotalElements(), repairAgentQueuePage.getTotalElements(), data);
    }

    public RepairAgentQueue getRepairAgentQueue(Long seq) {
        return repairAgentQueueService.getRepairAgentQueue(seq);
    }

    public void updateRepairAgentQueue(StringMultiValueMapAdapter param) {
        RepairAgentQueue repairAgentQueue;
        if(param.exist("seq")) {
            Long seq = param.longVal("seq");
            repairAgentQueue = repairAgentQueueService.getRepairAgentQueue(seq);
        }else {
            repairAgentQueue = new RepairAgentQueue();
            Optional<RepairAgent> repairAgentOptional = repairAgentService.getRepairAgentByIpAddress(param.stringVal("ipAddress"));
            if(repairAgentOptional.isEmpty()) {
                throw new TbmAdminRuntimeException("리페어 에이전트 검색실패! IP주소 : " + param.stringVal("ipAddress"));
            }

            RepairAgent repairAgent = repairAgentOptional.get();
            repairAgentQueue.setAgentSeq(repairAgent.getSeq());
            repairAgentQueue.setIpAddress(repairAgent.getIpAddress());
            repairAgentQueue.setMacAddress(repairAgent.getMacAddress());
            repairAgentQueue.setBlogId(repairAgent.getBlogId());
            repairAgentQueue.setStatus(param.stringVal("status"));
            repairAgentQueue.setBlogUrl(param.stringVal("blogUrl"));

//            RepairCode repairCode = repairCodeService.getRepairCodeByName("repair_agent_20241029_windows");
//            repairAgentQueue.setCodeSeq(repairCode.getSeq());
        }

        repairAgentQueue.setBlogUrl(param.stringVal("blogUrl"));
        repairAgentQueue.setTitle(param.stringVal("title"));
        repairAgentQueue.setSearchText(param.stringVal("searchText"));
        repairAgentQueue.setContent(param.stringVal("content"));
        if(repairAgentQueue.getContent() != null && !repairAgentQueue.getContent().isEmpty()) {
            // 80 은 네이버 블로그 에디터에서 라인 브레이커기준의 폭 길이, 전체 값에 20를 더해준다. (혹시몰라서)
            final int lineNumber = ContentUtils.calculateTotalLines(repairAgentQueue.getContent(), 80) + 20;
            repairAgentQueue.setLineNumber(lineNumber);
        }
        repairAgentQueue.setReservedAt(param.localDateTimeValISO("reservedAt"));

        repairAgentQueueService.save(repairAgentQueue);
    }

    public void deleteRepairAgentQueue(Long seq) {
        final RepairAgentQueue repairAgentQueue = repairAgentQueueService.getRepairAgentQueue(seq);
        repairAgentQueue.setStatus("deleted");
        repairAgentQueue.setDeletedAt(LocalDateTime.now());
        repairAgentQueueService.save(repairAgentQueue);
    }

    public RestResult importRepairs(MultipartFile file) {

        Workbook workbook;
        try {
            workbook = new XSSFWorkbook(file.getInputStream());
        } catch (IOException e) {
            throw new TbmAdminRuntimeException("엑셀파일 오픈 실패!" + e.getMessage());
        }

        Sheet sheet = workbook.getSheetAt(0);

        List<RepairAgentQueue> newRepairAgentQueues = new ArrayList<>();

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; // Skip header row

            if (StringUtils.isBlank(getTrimmedCellValue(row.getCell(0))) ||
                    StringUtils.isBlank(getTrimmedCellValue(row.getCell(1)))) {
                continue;
            }

            RepairAgentQueue repairAgentQueue = new RepairAgentQueue();

            String blogId = getTrimmedCellValue(row.getCell(0));
            repairAgentQueue.setBlogId(blogId);

            String blogUrl = getTrimmedCellValue(row.getCell(1));
            repairAgentQueue.setBlogUrl(blogUrl);

            Date date = row.getCell(2).getDateCellValue();
            LocalDateTime reservedAt = date.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();
            repairAgentQueue.setReservedAt(reservedAt);

            String searchText = getTrimmedCellValue(row.getCell(3));
            repairAgentQueue.setSearchText(searchText);

            String title = getTrimmedCellValue(row.getCell(4));
            repairAgentQueue.setTitle(title);

            String content = getTrimmedCellValue(row.getCell(5));
            repairAgentQueue.setContent(content);

            if(repairAgentQueue.getContent() != null && !repairAgentQueue.getContent().isEmpty()) {
                // 80 은 네이버 블로그 에디터에서 라인 브레이커기준의 폭 길이, 전체 값에 20를 더해준다. (혹시몰라서)
                final int lineNumber = ContentUtils.calculateTotalLines(repairAgentQueue.getContent(), 80) + 20;
                repairAgentQueue.setLineNumber(lineNumber);
            }

            RepairAgent repairAgent = repairAgentService.getRepairAgent(blogId);

            repairAgentQueue.setAgentSeq(repairAgent.getSeq());
            repairAgentQueue.setIpAddress(repairAgent.getIpAddress());
            repairAgentQueue.setMacAddress(repairAgent.getMacAddress());
            repairAgentQueue.setStatus("reserved"); // default 값이고 배치에서 시간이 지나면 변경한다.

            newRepairAgentQueues.add(repairAgentQueue);

            log.info("to update 할 목록:  blogId : {}, blogUrl : {}, reservedAt : {}, searchText : {}, title : {}",
                    blogId, blogUrl, reservedAt, searchText, title);
        }

        try {
            workbook.close();
        } catch (IOException e) {
            throw new TbmAdminRuntimeException("엑셀파일 닫기 실패!" + e.getMessage());
        }

        List<RepairAgentQueue> allRepairAgentQueues = repairAgentQueueService.findAllToUpsert();
        Map<String, Long> dbBlogCountMap = allRepairAgentQueues.stream()
                .collect(Collectors.groupingBy(RepairAgentQueue::getBlogId, Collectors.counting()));

        Map<String, Long> newBlogCountMap = newRepairAgentQueues.stream()
                .collect(Collectors.groupingBy(RepairAgentQueue::getBlogId, Collectors.counting()));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dbBlogCountMap", dbBlogCountMap);
        data.put("newBlogCountMap", newBlogCountMap);
        data.put("newRepairAgentQueues", newRepairAgentQueues);

        return new RestResult(data);
    }

    private String getTrimmedCellValue(Cell cell) {
        return cell == null ? "" : cell.getStringCellValue().trim();
    }

    public RestResult finalizeImportReports(List<RepairAgentQueue> newRepairAgentQueues) {

        // 기존것을
        List<RepairAgentQueue> allRepairAgentQueues = repairAgentQueueService.findAllToUpsert();

        // 지우고
        repairAgentQueueService.deleteAll(allRepairAgentQueues);

        // 새것을 저장한다.
        repairAgentQueueService.saveAll(newRepairAgentQueues);

        return RestResult.success();
    }
}
