package com.shinsegaelaw.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;

@NoRepositoryBean
public interface BaseRepository<M, I extends Serializable> extends JpaRepository<M, I>, JpaSpecificationExecutor<M> {

}