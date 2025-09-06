package com.tbm.admin.repository;

import com.tbm.admin.model.entity.Depart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartRepository extends JpaRepository<Depart, Long> {
    
    List<Depart> findAllByOrderByDepartNameAsc();
    
    boolean existsByDepartName(String departName);
    
    boolean existsByDepartNameAndSeqNot(String departName, Long seq);
} 