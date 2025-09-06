package com.tbm.admin.service.front;

import com.tbm.admin.model.entity.SearchKeyword;
import com.tbm.admin.model.entity.SearchKeywordTeam;
import com.tbm.admin.model.entity.SearchKeywordTeamSchedule;
import com.tbm.admin.model.view.base.DataTableView;

import com.tbm.admin.model.view.rest.RestResult;
import com.tbm.admin.service.persist.SearchKeywordService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;


import java.util.*;
import java.util.stream.Collectors;



@Slf4j
@Service
@RequiredArgsConstructor
public class SearchKeywordFrontService {

    private final SearchKeywordService searchKeywordService;

    public List<SearchKeywordTeam> getTeams() {
        return searchKeywordService.getTeams();
    }

    public SearchKeywordTeam getTeam(Long teamSeq) {
        return searchKeywordService.getTeam(teamSeq);
    }

    public SearchKeywordTeam getTeamByDepart(String depart) {
        return searchKeywordService.getTeamByDepart(depart);
    }

    public List<SearchKeywordTeam> getTeamsByDeparts(List<String> departs) {
        return searchKeywordService.getTeamsByDeparts(departs);
    }

    public RestResult getSearchKeywords(Long teamSeq) {

        List<SearchKeyword> searchKeywords = searchKeywordService.getSearchKeywords(teamSeq);
        RestResult restResult = new RestResult();
        restResult.setSuccess(true);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("result", searchKeywords);
        restResult.setData(map);

        return restResult;
    }

    public DataTableView getSearchKeywordsPaging(Long teamSeq, int draw, int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("seq").ascending());
        
        // 전체 레코드 수 (필터링 없는 상태)
        long totalCount = searchKeywordService.getSearchKeywordsCountByStatus(teamSeq, null);
        
        // 필터링된 레코드 수
        long filteredCount = searchKeywordService.getSearchKeywordsCountByStatus(teamSeq, status);
        
        // 페이징된 데이터 조회
        Page<SearchKeyword> searchKeywords = searchKeywordService.getSearchKeywordsPaging(teamSeq, pageable, status);
        
        // 전체 키워드 조회 (상태별 카운트 계산용)
        List<SearchKeyword> allKeywords = searchKeywordService.getSearchKeywords(teamSeq);
        
        // 상태별 카운트 계산
        Map<String, Long> statusCounts = allKeywords.stream()
            .collect(Collectors.groupingBy(
                keyword -> keyword.getStatus() != null ? keyword.getStatus() : "UNKNOWN",
                Collectors.counting()
            ));
        
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", searchKeywords.getContent());
        data.put("statusCounts", Map.of(
            "total", totalCount,
            "WAITING", statusCounts.getOrDefault("WAITING", 0L),
            "PROCESSING", statusCounts.getOrDefault("PROCESSING", 0L),
            "COMPLETE", statusCounts.getOrDefault("COMPLETE", 0L),
            "INVALID", statusCounts.getOrDefault("INVALID", 0L)
        ));
        
        // DataTableView 생성자: draw, recordsTotal, recordsFiltered, data
        return new DataTableView(draw, totalCount, filteredCount, data);
    }

    public RestResult addTeam(String teamName, String depart) throws Exception {
        log.info("addTeam teamName: {}, depart: {}", teamName, depart);
        SearchKeywordTeam newSearchkeywordTeam = searchKeywordService.addTeam(teamName, depart);
        RestResult restResult = new RestResult();
        restResult.setSuccess(true);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("result", newSearchkeywordTeam);
        restResult.setData(map);
        return restResult;
    }

    public RestResult getSchedule(Long scheduleSeq) throws Exception {
        SearchKeywordTeamSchedule searchkeywordTeamSchedule = searchKeywordService.getSchedule(scheduleSeq);
        RestResult restResult = new RestResult();
        restResult.setSuccess(true);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("result", searchkeywordTeamSchedule);
        restResult.setData(map);
        return restResult;
    }

    public RestResult addSchedule(Long teamSeq, String scheduleTime) throws Exception {
        SearchKeywordTeamSchedule newSearchkeywordTeamSchedule = searchKeywordService.addSchedule(teamSeq, scheduleTime);
        RestResult restResult = new RestResult();
        restResult.setSuccess(true);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("result", newSearchkeywordTeamSchedule);
        restResult.setData(map);
        return restResult;
    }

    public RestResult modifySchedule(Long scheduleSeq, String scheduleTime) throws Exception {
        SearchKeywordTeamSchedule newSearchkeywordTeamSchedule = searchKeywordService.modifySchedule(scheduleSeq, scheduleTime);
        RestResult restResult = new RestResult();
        restResult.setSuccess(true);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("result", newSearchkeywordTeamSchedule);
        restResult.setData(map);
        return restResult;
    }

    public RestResult deleteSchedule(Long scheduleSeq) throws Exception {
        SearchKeywordTeamSchedule newSearchkeywordTeamSchedule = searchKeywordService.deleteSchedule(scheduleSeq);
        RestResult restResult = new RestResult();
        restResult.setSuccess(true);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("result", newSearchkeywordTeamSchedule);
        restResult.setData(map);
        return restResult;
    }

    public RestResult modifyTeamName(Long teamSeq, String teamName) throws Exception {
        searchKeywordService.modifyTeamName(teamSeq, teamName);
        RestResult restResult = new RestResult();
        restResult.setSuccess(true);
        return restResult;
    }

    public RestResult deleteTeam(Long teamSeq) throws Exception {
        searchKeywordService.deleteTeam(teamSeq);
        RestResult restResult = new RestResult();
        restResult.setSuccess(true);
        return restResult;
    }

    public RestResult changeTeamActive(Long teamSeq) throws Exception {
        searchKeywordService.changeTeamActive(teamSeq);
        RestResult restResult = new RestResult();
        restResult.setSuccess(true);
        return restResult;
    }

    public RestResult addKeyword(Long teamSeq, String keyword) {
        SearchKeyword newSearchkeyword = searchKeywordService.addKeyword(teamSeq, keyword);
        RestResult restResult = new RestResult();
        restResult.setSuccess(true);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("result", newSearchkeyword);
        restResult.setData(map);
        return restResult;
    }

    public RestResult modifyKeyword(Long keywordSeq, String keyword) throws Exception {
        searchKeywordService.modifyKeyword(keywordSeq, keyword);
        RestResult restResult = new RestResult();
        restResult.setSuccess(true);
        return restResult;
    }

    public RestResult deleteKeyword(Long keywordSeq) throws Exception {
        searchKeywordService.deleteKeyword(keywordSeq);
        RestResult restResult = new RestResult();
        restResult.setSuccess(true);
        return restResult;
    }

    public RestResult uploadSearchImage(Long teamSeq, MultipartFile imageFile) throws Exception {
        String fileName = searchKeywordService.uploadSearchImage(teamSeq, imageFile);
        RestResult restResult = new RestResult();
        restResult.setSuccess(true);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("result", fileName);
        restResult.setData(map);
        return restResult;
    }

    public RestResult getTeamSchedules(Long teamSeq) {
        List<SearchKeywordTeamSchedule> searchKeywordTeamSchedules = searchKeywordService.getTeamSchedules(teamSeq);

        RestResult restResult = new RestResult();
        restResult.setSuccess(true);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("result", searchKeywordTeamSchedules);
        restResult.setData(map);

        return restResult;
    }

    public RestResult uploadKeywordFile(MultipartFile file, Long teamSeq) throws Exception {
        this.searchKeywordService.uploadKeywordFile(file, teamSeq);
        RestResult restResult = new RestResult();
        restResult.setSuccess(true);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("result", file.getOriginalFilename());
        restResult.setData(map);
        return restResult;
    }

    public RestResult getScreenshotsList(Long teamSeq) throws Exception {
        List<String> results = this.searchKeywordService.getScreenshotsList(teamSeq);
        RestResult restResult = new RestResult();
        restResult.setSuccess(true);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("result", results);
        restResult.setData(map);
        return restResult;
    }

    public ResponseEntity<Resource> downloadScreenshots(Long teamSeq, String fileName) throws Exception {
        Resource downloadFile = this.searchKeywordService.downloadScreenshots(teamSeq, fileName);

        String encodedFileName = UriUtils.encode(fileName, "UTF-8");
        String contentDisposition = "attachment; filename*=UTF-8''" + encodedFileName;

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(downloadFile);
    }

    public List<String> getDeparts() {
        return searchKeywordService.getDeparts();
    }

    public RestResult setSearchKeywordTeamDepart(@RequestBody Map<String, Object> body) throws Exception {
        Long teamSeq = Long.parseLong(body.get("teamSeq").toString());
        String depart = body.get("depart").toString();

        this.searchKeywordService.setSearchKeywordTeamDepart(teamSeq, depart);
        return RestResult.success();
    }
}
