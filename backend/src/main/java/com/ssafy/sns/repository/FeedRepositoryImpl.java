package com.ssafy.sns.repository;

import com.ssafy.sns.domain.newsfeed.Feed;
import com.ssafy.sns.domain.newsfeed.Indoor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class FeedRepositoryImpl implements FeedRepository {

    private final EntityManager em;

    @Override
    public List<Feed> findMyList(Long id, int num) {
        int readPageCnt = 10;

        return em.createQuery("SELECT f FROM Feed f WHERE f.user.id = :id ORDER BY f.createdDate DESC", Feed.class)
                .setParameter("id", id)
                .setFirstResult(num)
                .setMaxResults(readPageCnt)
                .getResultList();
    }

    @Override
    public List<Feed> findList(int num) {
        int readPageCnt = 10;

        return em.createQuery("SELECT f FROM Feed f ORDER BY f.createdDate DESC", Feed.class)
                .setFirstResult(num)
                .setMaxResults(readPageCnt)
                .getResultList();
    }

    @Override
    public Feed findById(Long id) {
        return em.find(Feed.class, id);
    }

    @Override
    public Feed save(Feed feed) {
        em.persist(feed);
        return feed;
    }

    @Override
    public void remove(Feed feed) {
        em.remove(feed);
    }
}