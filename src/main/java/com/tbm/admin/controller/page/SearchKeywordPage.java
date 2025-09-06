package com.tbm.admin.controller.page;

import com.tbm.admin.config.security.Authed;
import com.tbm.admin.model.entity.AdminMember;
import com.tbm.admin.model.entity.SearchKeywordTeam;
import com.tbm.admin.model.props.SeleniumProps;
import com.tbm.admin.model.view.base.DataTableView;
import com.tbm.admin.model.view.base.ScrapUrlView;
import com.tbm.admin.model.view.rest.RestResult;
import com.tbm.admin.service.front.*;
import com.tbm.admin.service.persist.AccountInfoService;
import com.tbm.admin.service.persist.ScrapUrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Core;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SearchKeywordPage {

    private final SeleniumProps seleniumProps;

    private final SearchKeywordFrontService searchKeywordFrontService;

    private static final String SUPER_ADMIN_ROLE = "superadmin";

    @GetMapping("/search_keyword_teams")
    public String searchKeywordTeams(Model model, @Authed AdminMember adminMember) {
        model.addAttribute("adminMember", adminMember);
        model.addAttribute("SUPER_ADMIN_ROLE", SUPER_ADMIN_ROLE);
        model.addAttribute("departs", searchKeywordFrontService.getDeparts());
        if(adminMember.getRole().equals(SUPER_ADMIN_ROLE)) {
            List<SearchKeywordTeam> searchKeywordTeams = searchKeywordFrontService.getTeams();
            model.addAttribute("teams", !searchKeywordTeams.isEmpty()? searchKeywordTeams : new ArrayList<>());
        } else {
            // 사용자의 depart가 있는지 확인
            if(adminMember.getDepart() != null && !adminMember.getDepart().isEmpty()) {
                List<SearchKeywordTeam> searchKeywordTeams = searchKeywordFrontService.getTeamsByDeparts(List.of(adminMember.getDepart()));
                model.addAttribute("teams", searchKeywordTeams);
            } else {
                // depart가 없으면 빈 리스트
                model.addAttribute("teams", new ArrayList<>());
            }
        }

        return "search_keyword_teams";
    }

    @GetMapping("/get_search_keywords")
    @ResponseBody
    public RestResult getSearchKeywords(Long teamSeq) {
        return searchKeywordFrontService.getSearchKeywords(teamSeq);
    }

    @GetMapping("/get_search_keywords_paging")
    @ResponseBody
    public DataTableView getSearchKeywordsPaging(@RequestParam Long teamSeq,
                                                 @RequestParam int draw,
                                                 @RequestParam int page,
                                                 @RequestParam int size,
                                                 @RequestParam(required = false, defaultValue = "all") String status) {
        return searchKeywordFrontService.getSearchKeywordsPaging(teamSeq, draw, page, size, status);
    }

    @PostMapping("/add_team")
    @ResponseBody
    public RestResult addTeam(@RequestBody Map<String, Object> body, @Authed AdminMember adminMember) throws Exception {
        String teamName = body.get("teamName").toString();
        String depart = adminMember.getDepart();
        
        // depart가 null이거나 빈 문자열인 경우 처리
        if (depart == null || depart.trim().isEmpty()) {
            throw new Exception("사용자의 부서가 설정되어있지 않습니다.");
        }
        
        return searchKeywordFrontService.addTeam(teamName, depart);
    }

    @PostMapping("/modify_team_name")
    @ResponseBody
    public RestResult modifyTeamName(@RequestBody Map<String, Object> body) throws Exception {
        return searchKeywordFrontService.modifyTeamName(Long.parseLong(body.get("teamSeq").toString()), body.get("newTeamName").toString());
    }

    @PostMapping("/delete_team")
    @ResponseBody
    public RestResult deleteTeam(@RequestBody Map<String, Object> body) throws Exception {
        return searchKeywordFrontService.deleteTeam(Long.parseLong(body.get("teamSeq").toString()));
    }

    @PostMapping("/change_team_active")
    @ResponseBody
    public RestResult changeTeamActive(@RequestBody Map<String, Object> body) throws Exception {
        return searchKeywordFrontService.changeTeamActive(Long.parseLong(body.get("teamSeq").toString()));
    }

    @PostMapping("/add_keyword")
    @ResponseBody
    public RestResult addKeyword(@RequestBody Map<String, Object> body) {
        return searchKeywordFrontService.addKeyword(Long.parseLong(body.get("teamSeq").toString()), body.get("keyword").toString());
    }

    @PostMapping("/modify_keyword")
    @ResponseBody
    public RestResult modifyKeyword(@RequestBody Map<String, Object> body) throws Exception {
        return searchKeywordFrontService.modifyKeyword(Long.parseLong(body.get("keywordSeq").toString()), body.get("newKeyword").toString());
    }

    @PostMapping("/delete_keyword")
    @ResponseBody
    public RestResult deleteKeyword(@RequestBody Map<String, Object> body) throws Exception {
        return searchKeywordFrontService.deleteKeyword(Long.parseLong(body.get("keywordSeq").toString()));
    }

    @PostMapping("/upload_search_image")
    @ResponseBody
    public RestResult uploadSearchImage(
            @RequestParam("teamSeq") Long teamSeq,
            @RequestParam("image") MultipartFile imageFile) throws Exception {
        return searchKeywordFrontService.uploadSearchImage(teamSeq, imageFile);
    }

    @GetMapping("/search_keyword")
    public String searchKeyword(Model model, @Authed AdminMember adminMember, @RequestParam(value = "teamSeq", required = false) Long teamSeq) {
        model.addAttribute("adminMember", adminMember);
        model.addAttribute("SUPER_ADMIN_ROLE", SUPER_ADMIN_ROLE);
        if(adminMember.getRole().equals(SUPER_ADMIN_ROLE)) {
            List<SearchKeywordTeam> searchKeywordTeams = searchKeywordFrontService.getTeams();
            model.addAttribute("teams", searchKeywordTeams);
            if(teamSeq != null) {
                SearchKeywordTeam searchKeywordTeam = searchKeywordFrontService.getTeam(teamSeq);
                model.addAttribute("team", searchKeywordTeam);
            } else {
                model.addAttribute("team", searchKeywordTeams.isEmpty()? null : searchKeywordTeams.get(0));
            }
        } else {
            // 사용자의 depart에 해당하는 키워드 그룹만 조회
            if(adminMember.getDepart() != null && !adminMember.getDepart().isEmpty()) {
                List<SearchKeywordTeam> searchKeywordTeams = searchKeywordFrontService.getTeamsByDeparts(List.of(adminMember.getDepart()));
                model.addAttribute("teams", searchKeywordTeams);
                
                // 선택된 팀이 있으면 해당 팀을, 없으면 첫 번째 팀을 선택
                if(teamSeq != null) {
                    SearchKeywordTeam searchKeywordTeam = searchKeywordFrontService.getTeam(teamSeq);
                    model.addAttribute("team", searchKeywordTeam);
                } else {
                    model.addAttribute("team", searchKeywordTeams.isEmpty()? null : searchKeywordTeams.get(0));
                }
            } else {
                // depart가 없으면 빈 리스트
                model.addAttribute("teams", new ArrayList<>());
                model.addAttribute("team", null);
            }
        }
        model.addAttribute("selectedTeamSeq", teamSeq);
        return "search_keyword";
    }

    @GetMapping("/get_search_team_schedules")
    @ResponseBody
    public RestResult getSearchTeamSchedules(@RequestParam Long teamSeq) {
        return searchKeywordFrontService.getTeamSchedules(teamSeq);
    }

    @PostMapping("/add_schedule")
    @ResponseBody
    public RestResult addSchedule(@RequestBody Map<String, Object> body) throws Exception {
        Long teamSeq = Long.parseLong(body.get("teamSeq").toString());
        String time = body.get("scheduleTime").toString();
        return searchKeywordFrontService.addSchedule(teamSeq, time);
    }

    @GetMapping("/get_schedule")
    @ResponseBody
    public RestResult getSchedule(@RequestParam("scheduleSeq") Long scheduleSeq) throws Exception {
        return searchKeywordFrontService.getSchedule(scheduleSeq);
    }

    @PostMapping("/modify_schedule")
    @ResponseBody
    public RestResult modifySchedule(@RequestBody Map<String, Object> body) throws Exception {
        Long scheduleSeq = Long.parseLong(body.get("scheduleSeq").toString());
        String time = body.get("scheduleTime").toString();
        return searchKeywordFrontService.modifySchedule(scheduleSeq, time);
    }

    @PostMapping("/delete_schedule")
    @ResponseBody
    public RestResult deleteSchedule(@RequestBody Map<String, Object> body) throws Exception {
        Long scheduleSeq = Long.parseLong(body.get("scheduleSeq").toString());
        return searchKeywordFrontService.deleteSchedule(scheduleSeq);
    }

    @PostMapping("/upload_keyword_file")
    @ResponseBody
    public RestResult uploadKeywordFile(@RequestParam("file") MultipartFile file, @RequestParam("teamSeq") Long teamSeq) throws Exception {
        return searchKeywordFrontService.uploadKeywordFile(file, teamSeq);
    }

    @GetMapping("/screenshots_list")
    @ResponseBody
    public RestResult getScreenshotsList(@RequestParam("teamSeq") Long teamSeq) throws Exception {
        return searchKeywordFrontService.getScreenshotsList(teamSeq);
    }

    @GetMapping("/download_screenshots")
    @ResponseBody
    public ResponseEntity<Resource> downloadScreenshots(@RequestParam("teamSeq") Long teamSeq, @RequestParam("fileName") String fileName) throws Exception {
        return searchKeywordFrontService.downloadScreenshots(teamSeq, fileName);
    }

    @GetMapping("/login_test")
    @ResponseBody
    public void loginTest() throws Exception {
        System.setProperty("webdriver.chrome.driver", seleniumProps.getChromedriver());
        try {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless"); // GUI 없이 실행

            // WebDriver 객체 생성
            WebDriver driver = new ChromeDriver(options);
            // 네이버 로그인 화면 이동
            driver.get("https://nid.naver.com/nidlogin.login?mode=form&url=https://www.naver.com/");

            Thread.sleep(3000);

            // 자바스크립트를 사용하여 input 요소에 값을 설정
            String script = "document.getElementById('id').value='" + "ahb910306" + "';"
                    + "document.getElementById('pw').value='" + "85291gudQo@" + "';";

            ((JavascriptExecutor) driver).executeScript(script);

            Thread.sleep(2500);

            // 로그인 버튼 클릭
            WebElement loginButton = driver.findElement(By.id("log.login"));
            loginButton.click();

            Thread.sleep(2500);

            System.out.println("currentUrl: " + driver.getCurrentUrl());

            if (driver.getCurrentUrl().contains("nidlogin.login")) throw new Exception();

        } catch (Exception e) {
            log.error("Failure auto login. id: {}", "aa", e);
        }
    }

    @PostMapping("/set_search_keyword_team_depart")
    @ResponseBody
    public RestResult setSearchKeywordTeamDepart(@RequestBody Map<String, Object> body) throws Exception {
        return searchKeywordFrontService.setSearchKeywordTeamDepart(body);
    }
}
