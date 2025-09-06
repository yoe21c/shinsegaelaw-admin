package com.tbm.admin.model.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter @Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
public class FeedHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seq;

    private String logNo;

    private String title;

    private String url;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "feedUrlSeq")
    private FeedUrl feedUrl;

    /**
     * logNo 를 기준으로 객체의 동등성을 비교하기 위해 equals, hashCode 재정의
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FeedHistory)) return false;
        FeedHistory that = (FeedHistory) o;
        return logNo != null && logNo.equals(that.logNo);
    }

    @Override
    public int hashCode() {
        return logNo != null ? logNo.hashCode() : 0;
    }

    protected FeedHistory() {}

    @Builder
    private FeedHistory(String logNo, String title, String url, FeedUrl feedUrl) {
        this.logNo = logNo;
        this.title = title;
        this.url = url;
        this.feedUrl = feedUrl;
    }

    public static FeedHistory create(String logNo, String title, String url, FeedUrl feedUrl) {
        return FeedHistory.builder()
                .logNo(logNo)
                .title(title)
                .url(url)
                .feedUrl(feedUrl)
                .build();
    }
}