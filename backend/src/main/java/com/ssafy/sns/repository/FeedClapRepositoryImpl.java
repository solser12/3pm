package com.ssafy.sns.repository;

import com.ssafy.sns.domain.clap.FeedClap;
import com.ssafy.sns.domain.newsfeed.Feed;
import com.ssafy.sns.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class FeedClapRepositoryImpl implements ClapRepository {

    private final EntityManager em;

    @Override
    public FeedClap save(FeedClap feedClap) {
        em.persist(feedClap);
        return feedClap;
    }

    @Override
    public List<FeedClap> findClap(User user, Feed feed) {
        return em.createQuery("SELECT f FROM FeedClap f WHERE f.user.id = :userId AND f.feed.id = :feedId", FeedClap.class)
                .setParameter("userId", user.getId())
                .setParameter("feedId", feed.getId())
                .getResultList();
    }

    @Override
    public void delete(FeedClap feedClap) {
        em.remove(feedClap);
    }

    @Override
    public List<FeedClap> findClapAll(Feed feed) {
        return em.createQuery("SELECT f FROM FeedClap f WHERE f.feed.id = :feedId", FeedClap.class)
                .setParameter("feedId", feed.getId())
                .getResultList();
    }


}