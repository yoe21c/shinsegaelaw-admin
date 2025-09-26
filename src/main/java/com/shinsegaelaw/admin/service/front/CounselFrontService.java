package com.shinsegaelaw.admin.service.front;

import com.shinsegaelaw.admin.exception.TbmAdminRuntimeException;
import com.shinsegaelaw.admin.model.entity.Counsel;
import com.shinsegaelaw.admin.model.param.StringMultiValueMapAdapter;
import com.shinsegaelaw.admin.model.view.base.DataTableView;
import com.shinsegaelaw.admin.service.persist.CounselService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CounselFrontService {

    private final CounselService counselService;

    public DataTableView getCounsels(MultiValueMap<String, String> param, Long memberSeq) {

        int draw = Integer.parseInt(param.get("draw").get(0));
        int start = Integer.parseInt(param.get("start").get(0));
        int length = Integer.parseInt(param.get("length").get(0));

        String keyword = StringUtils.isNotBlank(param.get("keyword").get(0)) ? param.get("keyword").get(0) : "";
        String status = param.get("status").get(0);

        int pageNumber = (start / length);
        final PageRequest pageable = PageRequest.of(pageNumber, length, Sort.Direction.DESC, "seq");

        Specification<Counsel> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 상태 필터링 (status 필드 사용)
            if (StringUtils.isNotBlank(status) && !"all".equals(status)) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            // 키워드 검색 (고객명, 고객 전화번호, 상담사명에서 검색)
            if (StringUtils.isNotBlank(keyword)) {
                Predicate keywordPredicate = criteriaBuilder.or(
                    criteriaBuilder.like(root.get("customer"), "%" + keyword + "%"),
                    criteriaBuilder.like(root.get("customerPhoneNumber"), "%" + keyword + "%"),
                    criteriaBuilder.like(root.get("counselor"), "%" + keyword + "%"),
                    criteriaBuilder.like(root.get("counselorPhoneNumber"), "%" + keyword + "%")
                );
                predicates.add(keywordPredicate);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        final Page<Counsel> counselPage = counselService.getCounsels(spec, pageable);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("counsels", counselPage);

        return new DataTableView(draw, counselPage.getTotalElements(), counselPage.getTotalElements(), data);
    }

    public Counsel getCounsel(Long seq) {
        return counselService.getCounsel(seq);
    }

    public void updateCounsel(StringMultiValueMapAdapter param) {
        Counsel counsel;
        if(param.exist("seq")) {
            Long seq = param.longVal("seq");
            counsel = counselService.getCounsel(seq);
        }else {
            counsel = Counsel.builder().build();
        }

        counsel.setCustomer(param.stringVal("customer"));
        counsel.setCustomerPhoneNumber(param.stringVal("customerPhoneNumber"));
        counsel.setCounselor(param.stringVal("counselor"));
        counsel.setCounselorPhoneNumber(param.stringVal("counselorPhoneNumber"));
        counsel.setStatus(param.stringVal("status"));
        counsel.setDescription(param.stringVal("description"));
        counsel.setSummary(param.stringVal("summary"));
        
        if(param.exist("counselAt")) {
            counsel.setCounselAt(param.localDateTimeVal("counselAt"));
        }

        counselService.save(counsel);
    }

    public void deleteCounsel(Long seq) {
        log.info("deleteCounsel seq: {}", seq);
        try {
            final Counsel counsel = counselService.getCounsel(seq);
            counsel.setStatus("deleted");
            counsel.setDescription("삭제 처리됨");
            counselService.save(counsel);
        }catch (Exception e) {
            log.error("deleteCounsel error", e);
            throw new TbmAdminRuntimeException("deleteCounsel error");
        }
    }
}