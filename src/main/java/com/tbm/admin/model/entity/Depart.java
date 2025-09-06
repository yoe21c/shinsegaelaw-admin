package com.tbm.admin.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Depart")
@Data
public class Depart {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seq")
    private Long seq;
    
    @Column(name = "departName", length = 20)
    private String departName;
    
    @CreationTimestamp
    @Column(name = "createdAt")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updatedAt")
    private LocalDateTime updatedAt;
} 