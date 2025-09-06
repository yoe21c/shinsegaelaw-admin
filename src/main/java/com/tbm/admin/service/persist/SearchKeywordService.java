package com.tbm.admin.service.persist;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.tbm.admin.model.entity.AdminMember;
import com.tbm.admin.model.entity.SearchKeyword;
import com.tbm.admin.model.entity.SearchKeywordTeam;
import com.tbm.admin.model.entity.SearchKeywordTeamSchedule;
import com.tbm.admin.model.props.SeleniumProps;
import com.tbm.admin.repository.AdminMemberRepository;
import com.tbm.admin.repository.SearchKeywordRepository;
import com.tbm.admin.repository.SearchKeywordTeamRepository;
import com.tbm.admin.repository.SearchKeywordTeamScheduleRepository;
import com.tbm.admin.service.thirdparty.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Size;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.io.FileHandler;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.openqa.selenium.JavascriptExecutor;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchKeywordService {
    private static final String IMAGE_UPLOAD_DIR = "uploads/teams/";

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    private final SearchKeywordTeamRepository searchKeywordTeamRepository;
    private final SearchKeywordRepository searchKeywordRepository;
    private final SearchKeywordTeamScheduleRepository searchKeywordTeamScheduleRepository;
    private final SeleniumProps seleniumProps;
    private final AdminMemberRepository adminMemberRepository;
    private final S3Service s3Service;

    public SearchKeywordTeam getTeam(Long teamSeq) {
        return searchKeywordTeamRepository.findBySeq(teamSeq);
    }

    public SearchKeywordTeam getTeamByDepart(String depart) {
        return searchKeywordTeamRepository.findByDepart(depart);
    }

    public List<SearchKeywordTeam> getTeamsByDeparts(List<String> departs) {
        return searchKeywordTeamRepository.findByDepartIn(departs);
    }

    public List<SearchKeywordTeam> getTeams() {
        return searchKeywordTeamRepository.findAllBy();
    }

    public List<SearchKeyword> getSearchKeywords(Long teamSeq) {
        return searchKeywordRepository.findByTeamSeq(teamSeq);
    }

    public Page<SearchKeyword> getSearchKeywordsPaging(Long teamSeq, Pageable pageable, String status) {
        if (status == null || status.equals("all")) {
            // 기존 모든 상태 조회
            return searchKeywordRepository.findByTeamSeq(teamSeq, pageable);
        } else {
            // 특정 상태만 조회
            return searchKeywordRepository.findByTeamSeqAndStatus(teamSeq, status, pageable);
        }
    }

    public SearchKeywordTeam addTeam(String teamName, String depart) throws Exception {
        SearchKeywordTeam isExist = searchKeywordTeamRepository.findByTeamName(teamName);
        if (isExist != null) {
            throw new Exception("잘못된 요청입니다.");
        }
        
        // depart가 null이거나 빈 문자열인 경우 처리
        if (depart == null || depart.trim().isEmpty()) {
            throw new Exception("부서 정보가 없습니다.");
        }
        
        SearchKeywordTeam searchKeywordTeam = new SearchKeywordTeam();
        searchKeywordTeam.setTeamName(teamName);
        searchKeywordTeam.setActive(true);
        searchKeywordTeam.setDepart(depart);
        searchKeywordTeam.setCreatedAt(LocalDateTime.now());    // 자동저장안되서 우선 코드에서
        searchKeywordTeam.setUpdatedAt(LocalDateTime.now());    // 자동저장안되서 우선 코드에서
        
        log.info("Creating new team: teamName={}, depart={}", teamName, depart);
        searchKeywordTeamRepository.save(searchKeywordTeam);
        log.info("Team created successfully: seq={}, teamName={}, depart={}", 
                searchKeywordTeam.getSeq(), searchKeywordTeam.getTeamName(), searchKeywordTeam.getDepart());
        
        return searchKeywordTeam;
    }

    public SearchKeywordTeamSchedule addSchedule(Long teamSeq, String searchTime) throws Exception {
        SearchKeywordTeam isExist = searchKeywordTeamRepository.findBySeq(teamSeq);
        if (isExist == null) {
            throw new Exception("잘못된 요청입니다.");
        }
        // 중복 검사
        Optional<SearchKeywordTeamSchedule> searchKeywordTeamScheduleOptional = searchKeywordTeamScheduleRepository
                .findByTeamSeqAndSearchTime(teamSeq, LocalTime.parse(searchTime).withSecond(0));
        if (searchKeywordTeamScheduleOptional.isPresent()) {
            throw new Exception("이미 존재하는 스케쥴입니다.");
        }

        SearchKeywordTeamSchedule newSearchKeywordTeamSchedule = new SearchKeywordTeamSchedule();
        newSearchKeywordTeamSchedule.setTeamSeq(teamSeq);
        newSearchKeywordTeamSchedule.setSearchTime(LocalTime.parse(searchTime).withSecond(0));
        searchKeywordTeamScheduleRepository.save(newSearchKeywordTeamSchedule);
        return newSearchKeywordTeamSchedule;
    }

    public SearchKeywordTeamSchedule getSchedule(Long scheduleSeq) throws Exception {
        SearchKeywordTeamSchedule searchKeywordTeamSchedule = searchKeywordTeamScheduleRepository
                .findBySeq(scheduleSeq);
        if (searchKeywordTeamSchedule == null) {
            throw new Exception("잘못된 요청입니다.");
        }
        return searchKeywordTeamSchedule;
    }

    public SearchKeywordTeamSchedule modifySchedule(Long scheduleSeq, String searchTime) throws Exception {
        SearchKeywordTeamSchedule searchKeywordTeamSchedule = searchKeywordTeamScheduleRepository
                .findBySeq(scheduleSeq);
        if (searchKeywordTeamSchedule == null) {
            throw new Exception("잘못된 요청입니다.");
        }
        searchKeywordTeamSchedule.setSearchTime(LocalTime.parse(searchTime).withSecond(0));
        searchKeywordTeamScheduleRepository.save(searchKeywordTeamSchedule);
        return searchKeywordTeamSchedule;
    }

    public SearchKeywordTeamSchedule deleteSchedule(Long scheduleSeq) throws Exception {
        SearchKeywordTeamSchedule searchKeywordTeamSchedule = searchKeywordTeamScheduleRepository
                .findBySeq(scheduleSeq);
        if (searchKeywordTeamSchedule == null) {
            throw new Exception("잘못된 요청입니다.");
        }
        searchKeywordTeamScheduleRepository.delete(searchKeywordTeamSchedule);
        return searchKeywordTeamSchedule;
    }

    public void modifyTeamName(Long teamSeq, String newTeamName) throws Exception {
        SearchKeywordTeam searchKeywordTeam = searchKeywordTeamRepository.findBySeq(teamSeq);
        if (searchKeywordTeam == null) {
            throw new Exception("잘못된 요청입니다.");
        }
        searchKeywordTeam.setTeamName(newTeamName);
        searchKeywordTeamRepository.save(searchKeywordTeam);
    }

    public void deleteTeam(Long teamSeq) throws Exception {
        SearchKeywordTeam searchKeywordTeam = searchKeywordTeamRepository.findBySeq(teamSeq);

        if (searchKeywordTeam == null) {
            throw new Exception("잘못된 요청입니다.");
        }
        searchKeywordTeamRepository.delete(searchKeywordTeam);
    }

    public SearchKeywordTeam changeTeamActive(Long teamSeq) throws Exception {
        SearchKeywordTeam searchKeywordTeam = searchKeywordTeamRepository.findBySeq(teamSeq);
        if (searchKeywordTeam == null) {
            throw new Exception("잘못된 요청입니다.");
        }
        searchKeywordTeam.setActive(!searchKeywordTeam.isActive());
        searchKeywordTeamRepository.save(searchKeywordTeam);
        return searchKeywordTeam;
    }

    public SearchKeyword addKeyword(Long teamSeq, String keyword) {
        SearchKeyword searchKeyword = new SearchKeyword();
        searchKeyword.setTeamSeq(teamSeq);
        searchKeyword.setKeyword(keyword);
        searchKeywordRepository.save(searchKeyword);
        return searchKeyword;
    }

    public void modifyKeyword(Long keywordSeq, String newKeyword) throws Exception {
        SearchKeyword searchKeyword = searchKeywordRepository.findBySeq(keywordSeq);
        if (searchKeyword == null) {
            throw new Exception("잘못된 요청입니다.");
        }
        searchKeyword.setKeyword(newKeyword);
        searchKeywordRepository.save(searchKeyword);
    }

    public void deleteKeyword(Long keywordSeq) throws Exception {
        SearchKeyword searchKeyword = searchKeywordRepository.findBySeq(keywordSeq);
        if (searchKeyword == null) {
            throw new Exception("잘못된 요청입니다.");
        }
        searchKeywordRepository.delete(searchKeyword);
    }

    public List<SearchKeywordTeamSchedule> getTeamSchedules(Long teamSeq) {
        return searchKeywordTeamScheduleRepository.findByTeamSeqOrderBySearchTimeAsc(teamSeq);
    }

    public String uploadSearchImage(Long teamSeq, MultipartFile imageFile) throws Exception {
        SearchKeywordTeam searchKeywordTeam = this.searchKeywordTeamRepository.findBySeq(teamSeq);
        if (searchKeywordTeam == null) {
            throw new Exception("잘못된 요청입니다.");
        }
        // 이미지 파일 체크
        if (imageFile.isEmpty()) {
            throw new Exception("잘못된 요청입니다.");
        }

        try {
            // 기존 S3 이미지 삭제 시도
            String existingImageUrl = searchKeywordTeam.getSearchThumbnailUrl();
            if (existingImageUrl != null && !existingImageUrl.isEmpty()) {
                try {
                    // URL에서 키 추출
                    String existingKey = existingImageUrl.substring(existingImageUrl.indexOf("/uploads/"));
                    s3Service.deleteFile(existingKey);
                } catch (Exception e) {
                    // 파일이 없거나 삭제 실패해도 계속 진행
                    log.warn("기존 이미지 삭제 실패: {}", existingImageUrl, e);
                }
            }

            // 파일명 생성
            String fileName = "uploads/teams/team_" + teamSeq + "_" + System.currentTimeMillis() + "_"
                    + imageFile.getOriginalFilename();

            // S3에 파일 업로드
            s3Service.uploadFile(
                    imageFile.getInputStream(),
                    imageFile.getSize(),
                    fileName,
                    imageFile.getContentType());

            // S3 전체 URL 생성
            String fullS3Url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName);

            // DB에 전체 URL 저장
            searchKeywordTeam.setSearchThumbnailUrl(fullS3Url);
            this.searchKeywordTeamRepository.save(searchKeywordTeam);

            return fullS3Url;
        } catch (IOException e) {
            log.error("이미지 업로드 실패", e);
            throw new Exception("업로드에 실패하였습니다.");
        }
    }

    public void searchKeywordWithImage() {
        System.setProperty("webdriver.chrome.driver", seleniumProps.getChromedriver());
        // DB에서 활성화 된 팀 목록 조회
        LocalTime nowWithoutSeconds = LocalTime.now().withSecond(0).withNano(0);
        List<SearchKeywordTeam> searchKeywordTeams = this.searchKeywordTeamRepository.findByActive(true);
        for (SearchKeywordTeam searchKeywordTeam : searchKeywordTeams) {
            // schedule 시간 조회
            Optional<SearchKeywordTeamSchedule> searchKeywordTeamScheduleOptional = this.searchKeywordTeamScheduleRepository
                    .findByTeamSeqAndSearchTime(searchKeywordTeam.getSeq(), nowWithoutSeconds);
            if (searchKeywordTeamScheduleOptional.isEmpty()) {
                continue;
            }

            final SearchKeywordTeamSchedule searchKeywordTeamSchedule = searchKeywordTeamScheduleOptional.get();

            log.info("-->> Searching ... teamName: {}, searchThumbnailUrl: {}, teamSeq: {}, searchTime: {}",
                    searchKeywordTeam.getTeamName(),
                    searchKeywordTeam.getSearchThumbnailUrl(),
                    searchKeywordTeamSchedule.getTeamSeq(),
                    searchKeywordTeamSchedule.getSearchTime());

            String searchThumbnailImage = searchKeywordTeam.getSearchThumbnailUrl();
            // keyword 조회
            List<SearchKeyword> searchKeywords = this.searchKeywordRepository.findByTeamSeq(searchKeywordTeam.getSeq());

            // 모든 키워드 상태를 한 번에 PROCESSING으로 변경
            searchKeywords.forEach(keyword -> keyword.setStatus("PROCESSING"));
            searchKeywords.forEach(keyword -> keyword.setRanking(null));
            searchKeywordRepository.saveAll(searchKeywords);

            for (SearchKeyword searchKeyword : searchKeywords) {
                try {
                    List<List<String>> allThumbnails = new ArrayList<>();
                    String url = "https://search.naver.com/search.naver?&query=" + searchKeyword.getKeyword();
                    RestTemplate restTemplate = new RestTemplate();
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("referer", "https://www.naver.com/");

                    addDefaultHeaders(headers);
                    String userAgent = this.getRandomPCUserAgent();
                    headers.add("user-agent", userAgent);

                    HttpEntity<String> entity = new HttpEntity<>(headers);

                    ResponseEntity<String> response = restTemplate.exchange(
                            url,
                            HttpMethod.GET, entity, String.class);
                    Document doc = Jsoup.parse(response.getBody());
                    Element sectionElement = doc.selectFirst("section.sc_new.sp_nreview._fe_view_root._prs_ugB_bsR");
                    if (sectionElement == null) {
                        throw new Exception("검색 결과 섹션을 찾을 수 없습니다.");
                    }

                    Elements lstViewElement = sectionElement.select("ul.lst_view li");
                    if (lstViewElement == null || lstViewElement.isEmpty()) {
                        throw new Exception("검색 결과 항목을 찾을 수 없습니다.");
                    }
                    int a = 1;
                    for (Element liElement : lstViewElement) {
                        // 광고 항목 체크
                        Element userBox = liElement.selectFirst("div.user_box");
                        Element linkAd = userBox != null ? userBox.selectFirst("a.link_ad") : null;

                        // link_ad 클래스가 있으면 건너뛰기
                        if (linkAd != null) {
                            continue;
                        }

                        List<String> thumbnails = new ArrayList<>();

                        // 썸네일이 여러 개인 경우: .mod_ugc_thumb_area .thumb_item img
                        Elements imgElements = liElement.select(".mod_ugc_thumb_area .thumb_item img");
                        if (!imgElements.isEmpty()) {
                            for (Element imgElement : imgElements) {
                                String src = imgElement.attr("src");
                                if (!src.isEmpty()) {
                                    thumbnails.add(src);
                                }
                            }
                        } else {
                            // 썸네일이 하나인 경우: .mod_ugc_thumb_area 바로 아래의 a 태그의 img 요소
                            Element singleImgElement = liElement.selectFirst(".mod_ugc_thumb_area > a > img");
                            if (singleImgElement != null) {
                                String src = singleImgElement.attr("src");
                                if (!src.isEmpty()) {
                                    thumbnails.add(src);
                                }
                            }
                        }

                        if (!thumbnails.isEmpty()) {
                            allThumbnails.add(thumbnails);
                        }
                    }
                    int ranking = 0;

                    // loggin allThumbnails
                    for (int index = 0; index < allThumbnails.size(); index++) {
                        List<String> thumbnails = allThumbnails.get(index);
                        for (String thumbnail : thumbnails) {
                            boolean result = this.compareThumbnailImages(searchThumbnailImage, thumbnail);
                            if (result) {
                                ranking = index + 1;
                                break;
                            }
                        }
                        if (ranking != 0)
                            break;
                    }

                    // 결과 저장
                    searchKeyword.setRanking(ranking);
                    ZoneId zoneId = ZoneId.of("Asia/Seoul");
                    LocalDateTime seoulTime = ZonedDateTime.now(zoneId).toLocalDateTime();
                    searchKeyword.setLastSearchedAt(seoulTime);
                    searchKeyword.setStatus("COMPLETE");
                    this.searchKeywordRepository.save(searchKeyword);

                    // 스크린샷 캡처
                    captureSearchScreenshot(searchKeyword.getKeyword(), searchKeywordTeam.getTeamName(), ranking);

                } catch (Exception e) {
                    log.error("Error processing keyword: {}, error: {}", searchKeyword.getKeyword(), e.getMessage(), e);

                    ZoneId zoneId = ZoneId.of("Asia/Seoul");
                    LocalDateTime seoulTime = ZonedDateTime.now(zoneId).toLocalDateTime();
                    searchKeyword.setLastSearchedAt(seoulTime);
                    searchKeyword.setStatus("INVALID");
                    searchKeyword.setRanking(null);
                    this.searchKeywordRepository.save(searchKeyword);

                    log.info("오류가 났지만 계속 다음을 진행한다. {}, error: {}", searchKeyword.getKeyword(), e.getMessage(), e);
                    continue;
                }
            }

            // 압축 및 정리
            String teamFilePath = "uploads/teams/" + searchKeywordTeam.getTeamName();
            log.info("압축을 시작합니다: teamName={}, teamSeq={}", searchKeywordTeam.getTeamName(), searchKeywordTeam.getSeq());
            compressScreenshots(teamFilePath, searchKeywordTeam.getSeq(), searchKeywordTeam.getTeamName());
            log.info("압축이 완료되었습니다: teamName={}, teamSeq={}", searchKeywordTeam.getTeamName(),
                    searchKeywordTeam.getSeq());
            deleteFolder(teamFilePath);
        }
    }

    private void deleteFolder(String folderPath) {
        try {
            log.info("임시 폴더 삭제를 시작합니다: folderPath={}", folderPath);
            Path folder = Paths.get(folderPath);
            Files.walk(folder)
                    .sorted(Comparator.reverseOrder()) // 파일 -> 하위 디렉토리 -> 상위 디렉토리 순으로 정렬
                    .forEach(path -> {
                        try {
                            Files.delete(path); // 파일/디렉토리 삭제
                        } catch (IOException e) {
                            System.err.println("Failed to delete: " + path + " - " + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.error("임시 폴더 삭제 중 오류가 발생했습니다: {}", e.getMessage());
            // 우선 skip 한다. (프로세스 중단 방지)
        }

    }

    private void captureSearchScreenshot(String keyword, String teamName, int ranking) {

        log.info("스크린샷 캡처를 시작합니다: keyword={}, teamName={}, ranking={}", keyword, teamName, ranking);

        System.setProperty("webdriver.chrome.driver", seleniumProps.getChromedriver());

        // Chrome 옵션 설정 (헤드리스 모드)
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // GUI 없이 실행
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--lang=ko-KR"); // 브라우저 기본 언어

        // WebDriver 객체 생성
        WebDriver driver = new ChromeDriver(options);

        try {
            // 네이버 메인 페이지로 이동
            driver.get("https://www.naver.com");

            // 검색창 요소 찾기
            WebElement searchInput = driver.findElement(By.id("query")); // 네이버 메인 페이지의 검색창 ID는 'query'입니다.

            // 검색어 입력
            searchInput.sendKeys(keyword);

            // 검색 버튼 클릭
            WebElement searchButton = driver.findElement(By.id("search-btn")); // 검색 버튼 ID는 'search_btn'입니다.
            searchButton.click();

            log.info("검색 결과 페이지로 이동했습니다: {}", driver.getCurrentUrl());

            // 검색 결과 페이지가 로드될 때까지 잠시 대기 (필요에 따라 조정)
            Thread.sleep(2000);

            log.info("검색 결과 페이지가 로드되었습니다. (2 초 대기 완료)");

            // 특정 요소 찾기 (예: 첫 번째 뉴스 기사)
            WebElement targetElement = driver
                    .findElement(By.cssSelector("section.sc_new.sp_nreview._fe_view_root._prs_ugB_bsR")); // 예시 CSS 선택자

            log.info("스크린샷 요소를 찾았습니다: {}", targetElement);

            // JavaScript 실행하여 link_ad를 포함한 li 전체 제거
            JavascriptExecutor js = (JavascriptExecutor) driver;

            js.executeScript("""
                        const element = arguments[0];
                        // 요소의 위치로 스크롤
                        element.scrollIntoView({behavior: 'instant', block: 'start'});
                        // 약간의 여유 공간을 위해 위로 조금 스크롤
                        window.scrollBy(0, -100);
                    """, targetElement);

            js.executeScript("""
                        const element = arguments[0];
                        const adLinks = element.querySelectorAll('.link_ad');
                        adLinks.forEach(ad => {
                            // link_ad의 조상 중 가장 가까운 li 태그를 찾아서 제거
                            const parentLi = ad.closest('li');
                            if (parentLi) {
                                parentLi.remove();
                            }
                        });
                    """, targetElement);

            js.executeScript("""
                        const element = arguments[0];
                        // 요소의 실제 높이를 구함
                        const originalHeight = element.scrollHeight;
                        // 요소의 스타일을 수정하여 모든 내용이 보이게 함
                        element.style.maxHeight = 'none';
                        element.style.overflow = 'visible';
                    """, targetElement);

            // 잠시 대기하여 스크롤과 스타일 변경이 적용되도록 함
            Thread.sleep(1000);

            // 요소 스크린샷 찍기
            File screenshot = targetElement.getScreenshotAs(OutputType.FILE);

            log.info("스크린샷을 찍었습니다: {}", screenshot);

            // 파일 저장 경로 설정
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));

            String teamFilePath = "uploads/teams/" + teamName;
            File uploadDir = new File(teamFilePath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            String rankedFilePath = "uploads/teams/" + teamName + "/ranked";
            uploadDir = new File(rankedFilePath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            String unrankedFilePath = "uploads/teams/" + teamName + "/unranked";
            uploadDir = new File(unrankedFilePath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            String filePath = null;
            if (ranking == 0) {
                filePath = unrankedFilePath + "/" + timestamp + "_" + keyword + ".png";
            } else {
                filePath = rankedFilePath + "/" + timestamp + "_" + ranking + "위_" + keyword + ".png";
            }

            // 파일 저장
            FileHandler.copy(screenshot, new File(filePath));

            log.info("스크린샷이 저장되었습니다: {} -> {}", screenshot, filePath);

        } catch (Exception e) {
            log.error("스크린샷 캡처 중 오류가 발생했습니다: {}", e.getMessage());
            throw new RuntimeException(e);

        } finally {

            // 작업 완료 후 드라이버 종료
            driver.quit();
        }
    }

    public void compressScreenshots(String screenshotPath, Long teamSeq, String teamName) {
        Path sourceDir = Paths.get(screenshotPath);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss"));
        String zipFileName = timestamp + "_" + teamName + ".zip";

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(byteArrayOutputStream)) {

            // 엑셀 파일 생성
            String excelFileName = timestamp + "_순위.xlsx";
            File excelFile = createExcelFile(teamSeq, excelFileName);

            Files.walk(sourceDir)
                    .filter(path -> !Files.isDirectory(path)) // 디렉토리는 제외하고 파일만 처리
                    .forEach(path -> {
                        ZipEntry zipEntry = new ZipEntry(sourceDir.relativize(path).toString());
                        try {
                            zos.putNextEntry(zipEntry);
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            System.err.println("Error while zipping file: " + path + " - " + e.getMessage());
                        }
                    });

            // 엑셀 파일 압축
            addFileToZip(zos, excelFile.toPath(), excelFile.getParentFile().toPath());

            zos.finish(); // 압축 마무리

            // 압축된 데이터를 S3에 업로드
            byte[] zipBytes = byteArrayOutputStream.toByteArray();
            InputStream zipInputStream = new ByteArrayInputStream(zipBytes);

            s3Service.uploadFile(zipInputStream, zipBytes.length,
                    "uploads/teams/" + teamSeq + "/" + zipFileName,
                    "application/zip");

            // 엑셀 파일 삭제
            excelFile.delete();
        } catch (IOException e) {
            throw new RuntimeException("Error compressing and uploading screenshots", e);
        }
    }

    private File createExcelFile(Long teamSeq, String fileName) throws IOException {
        // 키워드 데이터 조회
        List<SearchKeyword> keywords = searchKeywordRepository.findByTeamSeq(teamSeq);

        // 엑셀 워크북 생성
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("순위 결과");

            // 헤더 스타일
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // 헤더 생성
            Row headerRow = sheet.createRow(0);
            Cell keywordHeader = headerRow.createCell(0);
            Cell rankHeader = headerRow.createCell(1);
            keywordHeader.setCellValue("키워드");
            rankHeader.setCellValue("RANK");
            keywordHeader.setCellStyle(headerStyle);
            rankHeader.setCellStyle(headerStyle);

            // 데이터 입력
            int rowNum = 1;
            for (SearchKeyword keyword : keywords) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(keyword.getKeyword());
                if (keyword.getRanking() != null) {
                    if (keyword.getRanking() == 0) {
                        row.createCell(1).setCellValue(" ");
                    } else {
                        row.createCell(1).setCellValue(keyword.getRanking());
                    }
                } else {
                    row.createCell(1).setCellValue("-");
                }
            }

            // 컬럼 너비 자동 조정
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            // 파일 저장
            File excelFile = new File(System.getProperty("java.io.tmpdir"), fileName);
            try (FileOutputStream fileOut = new FileOutputStream(excelFile)) {
                workbook.write(fileOut);
            }

            return excelFile;
        }
    }

    private void addFileToZip(ZipOutputStream zos, Path file, Path sourceDir) {
        try {
            ZipEntry zipEntry = new ZipEntry(sourceDir.relativize(file).toString());
            zos.putNextEntry(zipEntry);
            Files.copy(file, zos);
            zos.closeEntry();
        } catch (IOException e) {
            log.error("Error while adding file to zip: {} - {}", file, e.getMessage());
        }
    }

    private void addDefaultHeaders(HttpHeaders headers) {
        headers.add("accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        headers.add("accept-language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.add("cache-control", "no-cache");
        headers.add("pragma", "no-cache");
        headers.add("sec-ch-ua", "\"Google Chrome\";v=\"123\", \"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"123\"");
        headers.add("sec-ch-ua-arch", "\"arm\"");
        headers.add("sec-ch-ua-bitness", "\"64\"");
        headers.add("sec-ch-ua-full-version-list",
                "\"Google Chrome\";v=\"123.0.6312.123\", \"Not:A-Brand\";v=\"8.0.0.0\", \"Chromium\";v=\"123.0.6312.123\"");
        headers.add("sec-ch-ua-mobile", "?0");
        headers.add("sec-ch-ua-model", "\"\"");
        headers.add("sec-ch-ua-platform", "macOS");
        headers.add("sec-ch-ua-platform-version", "14.4.1");
        headers.add("sec-ch-ua-wow64", "?0");
        headers.add("sec-fetch-dest", "document");
        headers.add("sec-fetch-mode", "navigate");
        headers.add("sec-fetch-site", "same-site");
        headers.add("sec-fetch-user", "?1");
        headers.add("upgrade-insecure-requests", "1");
        headers.add("user-agent",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
    }

    private boolean compareThumbnailImages(String s3ImageUrl, String webImagePath) {
        try {
            // S3 이미지 로드
            Mat localImage;
            try {
                // https://gpgpadad.s3.ap-northeast-2.amazonaws.com/uploads/teams/... 형식에서
                // uploads/teams/... 부분만 추출
                String s3Key = s3ImageUrl.substring(s3ImageUrl.indexOf(".com/") + 5);

                Resource resource = s3Service.getFile(s3Key);
                byte[] imageBytes = resource.getInputStream().readAllBytes();
                Mat matrix = imdecode(new Mat(imageBytes), IMREAD_COLOR);
                localImage = matrix;
            } catch (Exception e) {
                log.error("S3 이미지 로드 실패: {}, error: {}", s3ImageUrl, e.getMessage(), e);
                return false;
            }

            // 온라인 이미지 로드 (기존 방식 유지)
            Mat onlineImage = readImageFromUrl(webImagePath);

            if (localImage.empty() || onlineImage.empty()) {
                System.out.println("이미지를 로드할 수 없습니다.");
                return false;
            }

            // 1. 이미지 크기 통일
            Size size = new Size(400, 400);
            resize(localImage, localImage, size);
            resize(onlineImage, onlineImage, size);

            // 2. BGR 히스토그램 비교
            int[] bgrChannels = { 0, 1, 2 };
            int[] bgrHistSize = { 64, 64, 64 };
            float[] bgrRanges = { 0f, 256f, 0f, 256f, 0f, 256f };

            Mat bgrHist1 = new Mat();
            Mat bgrHist2 = new Mat();

            calcHist(new MatVector(localImage), new IntPointer(bgrChannels), new Mat(),
                    bgrHist1, new IntPointer(bgrHistSize), new FloatPointer(bgrRanges));
            calcHist(new MatVector(onlineImage), new IntPointer(bgrChannels), new Mat(),
                    bgrHist2, new IntPointer(bgrHistSize), new FloatPointer(bgrRanges));

            normalize(bgrHist1, bgrHist1, 0, 1, NORM_MINMAX, -1, new Mat());
            normalize(bgrHist2, bgrHist2, 0, 1, NORM_MINMAX, -1, new Mat());

            double bgrSimilarity = compareHist(bgrHist1, bgrHist2, HISTCMP_CORREL);

            // 3. 템플릿 매칭
            Mat result = new Mat();
            matchTemplate(onlineImage, localImage, result, TM_CCOEFF_NORMED);
            DoublePointer minVal = new DoublePointer(1);
            DoublePointer maxVal = new DoublePointer(1);
            Point minLoc = new Point();
            Point maxLoc = new Point();
            minMaxLoc(result, minVal, maxVal, minLoc, maxLoc, null);

            double templateSimilarity = maxVal.get();

            log.info("onlineImagePath: {}, bgrSimilarity: {}, templateSimilarity: {}",
                    webImagePath, bgrSimilarity, templateSimilarity);

            // BGR 히스토그램과 템플릿 매칭 점수가 모두 높아야 함
            return bgrSimilarity > 0.75 && templateSimilarity > 0.7;
        } catch (Exception e) {
            log.error("이미지 비교 중 오류가 발생했습니다.", e);
        }
        return false;
    }

    // URL에서 이미지를 읽어와 Mat 객체로 반환하는 메서드
    public static Mat readImageFromUrl(String imageUrl) throws Exception {
        URL url = new URL(imageUrl);
        InputStream in = url.openStream();
        File tempFile = File.createTempFile("temp_image", ".png");
        Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Mat image = imread(tempFile.getAbsolutePath(), IMREAD_COLOR);
        tempFile.delete();
        return image;
    }

    public void uploadKeywordFile(MultipartFile file, Long teamSeq) throws Exception {
        SearchKeywordTeam searchKeywordTeam = getTeam(teamSeq);
        if (searchKeywordTeam == null) {
            throw new Exception("잘못된 요청입니다.");
        }
        List<String> keywords = parseExcelFile(file);
        for (String keyword : keywords) {
            // 중복 체크
            SearchKeyword exists = this.searchKeywordRepository.findByTeamSeqAndKeyword(teamSeq, keyword);
            if (exists == null) {
                SearchKeyword searchKeyword = new SearchKeyword();
                searchKeyword.setTeamSeq(teamSeq);
                searchKeyword.setKeyword(keyword);
                this.searchKeywordRepository.save(searchKeyword);
            }
        }
        searchKeywordTeam.setLastUploadFileName(file.getOriginalFilename());
        this.searchKeywordTeamRepository.save(searchKeywordTeam);
    }

    public List<String> getScreenshotsList(Long teamSeq) throws Exception {
        List<String> zipFiles = s3Service.listObjects("uploads/teams/" + teamSeq + "/").stream()
                .filter(key -> key.toLowerCase().endsWith(".zip")) // ZIP 파일 필터링
                .map(key -> key.replace("uploads/teams/" + teamSeq + "/", "")).collect(Collectors.toList());
        return zipFiles;
    }

    public Resource downloadScreenshots(Long teamSeq, String fileName) throws Exception {
        return s3Service.getFile("uploads/teams/" + teamSeq + "/" + fileName);
    }

    private List<String> parseExcelFile(MultipartFile file) throws IOException {
        List<String> keywords = new ArrayList<>();

        try (InputStream is = file.getInputStream();
                Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0); // 첫 번째 시트 사용

            for (Row row : sheet) {
                Cell cell = row.getCell(0); // 첫 번째 열 사용
                if (cell != null) {
                    cell.setCellType(CellType.STRING);
                    String keyword = cell.getStringCellValue().trim();
                    if (!keyword.isEmpty()) {
                        keywords.add(keyword);
                    }
                }
            }
        } catch (InvalidFormatException e) {
            e.printStackTrace();
            throw new IOException("엑셀 파일 형식이 올바르지 않습니다.");
        }

        return keywords;
    }

    private String getRandomPCUserAgent() {
        List<String> pcUserAgents = Arrays.asList(
                // Chrome (Windows)
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.82 Safari/537.36",

                // Chrome (Mac)
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.77 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.85 Safari/537.36",

                // Firefox (Windows)
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:88.0) Gecko/20100101 Firefox/88.0",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:87.0) Gecko/20100101 Firefox/87.0",

                // Firefox (Mac)
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:89.0) Gecko/20100101 Firefox/89.0",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.14; rv:88.0) Gecko/20100101 Firefox/88.0",

                // Edge (Windows)
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36 Edg/91.0.864.64",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36 Edg/90.0.818.56");
        Random random = new Random();
        return pcUserAgents.get(random.nextInt(pcUserAgents.size()));
    }

    public List<String> getDeparts() {
        List<AdminMember> adminMembers = adminMemberRepository.findAll();
        return adminMembers.stream()
                .map(AdminMember::getDepart) // depart 필드 추출
                .filter(Objects::nonNull) // null 값 필터링
                .distinct() // 중복 제거
                .collect(Collectors.toList()); // 리스트로 변환
    }

    public void setSearchKeywordTeamDepart(Long teamSeq, String depart) {
        SearchKeywordTeam searchKeywordTeam = searchKeywordTeamRepository.findBySeq(teamSeq);
        searchKeywordTeam.setDepart(depart);
        searchKeywordTeamRepository.save(searchKeywordTeam);

    }

    public long getSearchKeywordsCountByStatus(Long teamSeq, String status) {
        if (status == null || status.equals("all")) {
            // 전체 개수 반환
            return searchKeywordRepository.countByTeamSeq(teamSeq);
        } else {
            // 특정 상태의 개수만 반환
            return searchKeywordRepository.countByTeamSeqAndStatus(teamSeq, status);
        }
    }

}
