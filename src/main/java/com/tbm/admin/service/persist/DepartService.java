package com.tbm.admin.service.persist;

import com.tbm.admin.exception.TbmAdminRuntimeException;
import com.tbm.admin.model.entity.Depart;
import com.tbm.admin.repository.DepartRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartService {
    
    private final DepartRepository departRepository;
    
    public List<Depart> getAllDeparts() {
        return departRepository.findAllByOrderByDepartNameAsc();
    }

    public Page<Depart> getDeparts(int page, int size) {
        return departRepository.findAll(PageRequest.of(page, size));
    }
    
    public Depart getDepart(Long seq) {
        return departRepository.findById(seq)
                .orElseThrow(() -> new TbmAdminRuntimeException("존재하지 않는 부서입니다."));
    }
    
    public Depart createDepart(String departName) {
        if (departRepository.existsByDepartName(departName)) {
            throw new TbmAdminRuntimeException("이미 존재하는 부서명입니다.");
        }
        
        Depart depart = new Depart();
        depart.setDepartName(departName);
        return departRepository.save(depart);
    }
    
    public Depart updateDepart(Long seq, String departName) {
        Depart depart = getDepart(seq);
        
        if (departRepository.existsByDepartNameAndSeqNot(departName, seq)) {
            throw new TbmAdminRuntimeException("이미 존재하는 부서명입니다.");
        }
        
        depart.setDepartName(departName);
        return departRepository.save(depart);
    }
    
    public void deleteDepart(Long seq) {
        Depart depart = getDepart(seq);
        departRepository.delete(depart);
    }
} 