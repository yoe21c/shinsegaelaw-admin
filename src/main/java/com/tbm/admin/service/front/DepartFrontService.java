package com.tbm.admin.service.front;

import com.tbm.admin.model.entity.Depart;
import com.tbm.admin.model.view.rest.RestResult;
import com.tbm.admin.service.persist.DepartService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DepartFrontService {
    
    private final DepartService departService;
    
    public Page<Depart> getDeparts(int page, int size) {
        return departService.getDeparts(page, size);
    }

    public RestResult getAllDeparts() {
        List<Depart> departs = departService.getAllDeparts();
        Map<String, Object> map = new HashMap<>();
        map.put("departs", departs);
        return new RestResult(map);
    }
    
    public RestResult getDepart(Long seq) {
        Depart depart = departService.getDepart(seq);
        Map<String, Object> map = new HashMap<>();
        map.put("depart", depart);
        return new RestResult(map);
    }
    
    public RestResult createDepart(String departName) {
        Depart depart = departService.createDepart(departName);
        Map<String, Object> map = new HashMap<>();
        map.put("depart", depart);
        return new RestResult(map);
    }
    
    public RestResult updateDepart(Long seq, String departName) {
        Depart depart = departService.updateDepart(seq, departName);
        Map<String, Object> map = new HashMap<>();
        map.put("depart", depart);
        return new RestResult(map);
    }
    
    public RestResult addDepart(Map<String, Object> body) {
        String departName = (String) body.get("departName");
        Depart depart = departService.createDepart(departName);
        Map<String, Object> map = new HashMap<>();
        map.put("depart", depart);
        return new RestResult(map);
    }
    
    public RestResult modifyDepart(Map<String, Object> body) {
        Long seq = Long.valueOf(body.get("seq").toString());
        String departName = (String) body.get("departName");
        Depart depart = departService.updateDepart(seq, departName);
        Map<String, Object> map = new HashMap<>();
        map.put("depart", depart);
        return new RestResult(map);
    }
    
    public RestResult deleteDepart(Map<String, Object> body) {
        Long seq = Long.valueOf(body.get("seq").toString());
        departService.deleteDepart(seq);
        return new RestResult();
    }
    
    public RestResult deleteDepart(Long seq) {
        departService.deleteDepart(seq);
        return new RestResult();
    }
} 