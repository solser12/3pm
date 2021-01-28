package com.ssafy.sns.domain.hashtag;

import com.ssafy.sns.domain.newsfeed.Indoor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class IndoorHashtag {

    // 뉴스피드 해시태그 id
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "indoor_hashtag_id")
    private Long id;

    // 뉴스피드
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "indoor_id")
    private Indoor indoor;

    // 해시태그
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hashtag_id")
    private Hashtag hashtag;
}
