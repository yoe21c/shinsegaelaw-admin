package com.shinsegaelaw.admin.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Counsels")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Counsel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    @Column(length = 200, columnDefinition = "varchar(200) DEFAULT NULL COMMENT '녹취 파일 URL'")
    private String url;

    @Column(length = 100, columnDefinition = "varchar(100) DEFAULT NULL COMMENT '녹취 파일명'")
    private String fileName;

    @Column(columnDefinition = "bigint DEFAULT NULL COMMENT '녹취 파일 크기 (byte)'")
    private Long fileSize;

    @Column(length = 50, columnDefinition = "varchar(50) DEFAULT NULL COMMENT '녹취 파일 타입 (mp3, wav, m4a)'")
    private String fileType;

    @Column(columnDefinition = "int DEFAULT NULL COMMENT '녹취 파일 길이 (ms)'")
    private Integer fileDurationMs;

    @Column(length = 30, columnDefinition = "varchar(30) DEFAULT NULL COMMENT '상담 상태 (ready, 진행중, 완료)'")
    @Builder.Default
    private String status = "created";

    @Column(name = "whisper_json", columnDefinition = "JSON COMMENT 'Whisper 결과 JSON'")
    private String whisperJson;

    @Column(length = 100, columnDefinition = "varchar(100) DEFAULT NULL COMMENT '상담사명'")
    private String counselor;

    @Column(length = 100, columnDefinition = "varchar(100) DEFAULT NULL COMMENT '상담사 전화번호'")
    private String counselorPhoneNumber;

    @Column(length = 100, columnDefinition = "varchar(100) DEFAULT NULL COMMENT '고객명'")
    private String customer;

    @Column(length = 100, columnDefinition = "varchar(100) DEFAULT NULL COMMENT '고객 전화번호'")
    private String customerPhoneNumber;

    @Column(length = 100)
    private String summary;

    @Column(length = 200, columnDefinition = "varchar(200) DEFAULT NULL COMMENT '설명'")
    private String description;

    @Column(columnDefinition = "datetime DEFAULT NULL COMMENT '상담완료시각'")
    private LocalDateTime counselAt;

    @Column(columnDefinition = "datetime DEFAULT NULL COMMENT 'AI를 활용해서 녹취파일로부터 JSON 추출을 완료한 시간'")
    private LocalDateTime whisperAt;

    @Column(columnDefinition = "datetime DEFAULT NULL COMMENT 'AI를 활용해서 요약완료한 시간'")
    private LocalDateTime summaryAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}