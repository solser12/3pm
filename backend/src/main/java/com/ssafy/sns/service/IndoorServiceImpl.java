package com.ssafy.sns.service;

import com.ssafy.sns.domain.follow.Follow;
import com.ssafy.sns.domain.hashtag.FeedHashtag;
import com.ssafy.sns.domain.hashtag.Hashtag;
import com.ssafy.sns.domain.newsfeed.Feed;
import com.ssafy.sns.domain.newsfeed.Indoor;
import com.ssafy.sns.domain.user.User;
import com.ssafy.sns.dto.newsfeed.*;
import com.ssafy.sns.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@Transactional
@RequiredArgsConstructor
public class IndoorServiceImpl implements FeedService {

    private final FeedRepositoryImpl feedRepository;
    private final HashtagRepositoryImpl hashtagRepository;
    private final FeedClapRepositoryImpl feedClapRepository;
    private final CommentRepositoryImpl commentRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;
    private final FileServiceImpl fileService;
    private final FollowServiceImpl followService;

    @Override
    public FeedListResponseDto findMyList(Long userId, Long targetId, int num) {
        User user = userRepository.findById(userId).orElseThrow(NoSuchElementException::new);
        List<Feed> indoorList = feedRepository.findMyList(targetId, num, Indoor.class);
        List<IndoorResponseDto> indoorResponseDtoList = new ArrayList<>();
        for (Feed feed : indoorList) {
            indoorResponseDtoList.add(new IndoorResponseDto((Indoor) feed,
                    (int) commentRepository.findListById(feed).count(),
                    feedClapRepository.findClapAll(feed).size(),
                    feedClapRepository.findClap(user, feed).isPresent(),
                    followService.isFollow(userId, feed)
                    ));
        }
        return new FeedListResponseDto<>(indoorResponseDtoList, num + indoorList.size());
    }

    @Override
    public FeedListResponseDto readList(Long userId, int num) {
        User user = userRepository.findById(userId).orElseThrow(NoSuchElementException::new);
        List<Feed> indoorList = feedRepository.findList(num, Indoor.class);
        List<IndoorResponseDto> indoorResponseDtoList = new ArrayList<>();
        for (Feed feed : indoorList) {
            indoorResponseDtoList.add(new IndoorResponseDto((Indoor) feed,
                    (int) commentRepository.findListById(feed).count(),
                    feedClapRepository.findClapAll(feed).size(),
                    feedClapRepository.findClap(user, feed).isPresent(),
                    followService.isFollow(userId, feed)));
        }
        return new FeedListResponseDto<>(indoorResponseDtoList, num + indoorList.size());
    }

    @Override
    public FeedResponseDto read(Long userId, Long feedId) {
        User user = userRepository.findById(userId).orElseThrow(NoSuchElementException::new);
        Feed feed = feedRepository.findById(feedId);
        if (!(feed instanceof Indoor)) throw new NoSuchElementException();
        return new IndoorResponseDto((Indoor) feed,
                (int) commentRepository.findListById(feed).count(),
                feedClapRepository.findClapAll(feed).size(),
                feedClapRepository.findClap(user, feed).isPresent(),
                followService.isFollow(userId, feed));
    }

    @Override
    public Long write(Long userId, FeedRequestDto feedRequestDto) {
        // ?????? ??????
        User user = userRepository.findById(userId).orElseThrow(NoSuchElementException::new);

        // ??? ??????
        Indoor indoor = ((Indoor) feedRepository.save(new Indoor(feedRequestDto, user)));

        // ?????? ????????????
        for (String fileName : feedRequestDto.getFilePaths()) {
            fileService.addFile(fileName, indoor);
        }

        user.addFeed(indoor);

        // ?????? ??????
        List<Hashtag> hashtags = new ArrayList<>();

        for (String tag : feedRequestDto.getTags()) {
            hashtagRepository.findByTag(tag).ifPresentOrElse(
                    hashtags::add,
                    () -> hashtags.add(hashtagRepository.save(new Hashtag(tag))));
        }

        for (Hashtag hashtag : hashtags) {
            FeedHashtag feedHashtag = new FeedHashtag();
            indoor.addFeedHashtag(feedHashtag);
            hashtag.addFeedHashtag(feedHashtag);
        }

        return indoor.getId();
    }

    @Override
    public void uploadFiles(Long feedId, MultipartFile file) throws IOException {
        Feed feed = feedRepository.findById(feedId);

        // ?????? ?????????
        String fileName = s3Service.uploadFile(file);
        // ?????? ??????
        fileService.addFile(fileName, feed);
    }

    @Override
    public void delete(Long userId, Long feedId) throws IOException {

        User user = userRepository.findById(userId).orElseThrow(NoSuchElementException::new);
        Indoor indoor = (Indoor) feedRepository.findById(feedId);
        if (!indoor.getUser().getId().equals(user.getId())) {
            throw new NoSuchElementException();
        }

        // ????????? ????????? ????????? ?????? ??????
        List<String> fileNames = fileService.findFileNameList(feedId);
        for (String fileName : fileNames) {
            s3Service.deleteFile(fileName);
        }

        user.deleteFeed(indoor);
        feedRepository.remove(indoor);
    }

    @Override
    public void modify(Long userId, Long feedId, FeedRequestDto feedRequestDto) throws IOException {
        // ?????? ??????
        User user = userRepository.findById(userId).orElseThrow(NoSuchElementException::new);
        Indoor indoor = (Indoor) feedRepository.findById(feedId);

        if (!indoor.getUser().getId().equals(user.getId())) {
            throw new NoSuchElementException();
        }

        // ??? ??????
        indoor.update((IndoorRequestDto) feedRequestDto);

        // ?????? ?????? ??????
        List<FeedHashtag> feedHashtags = hashtagRepository.findFeedHashTag(indoor);
        for (FeedHashtag feedHashtag : feedHashtags) {
            indoor.deleteFeedHashtag(feedHashtag);
            feedHashtag.getHashtag().deleteFeedHashtag(feedHashtag);
            hashtagRepository.remove(feedHashtag);
        }

        List<Hashtag> hashtags = new ArrayList<>();

        for (String tag : feedRequestDto.getTags()) {
            hashtagRepository.findByTag(tag).ifPresentOrElse(
                    hashtags::add,
                    () -> hashtags.add(hashtagRepository.save(new Hashtag(tag))));
        }

        for (Hashtag hashtag : hashtags) {
            FeedHashtag feedHashtag = new FeedHashtag();
            indoor.addFeedHashtag(feedHashtag);
            hashtag.addFeedHashtag(feedHashtag);
        }

        // ?????? ?????? ?????? ??????
        List<String> prevFileNames =  indoor.getFileList().stream()
                .map(file -> file.getFileName())
                .collect(Collectors.toList());
        List<String> curFileNames = feedRequestDto.getFilePaths();
        fileService.modifyFiles(prevFileNames, curFileNames);
    }

    public FeedListResponseDto feedRecommend(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(NoSuchElementException::new);
        List<Feed> indoorRecommend = feedRepository.findIndoorRecommend();
        List<IndoorResponseDto> indoorResponseDtoList = new ArrayList<>();
        for (Feed feed : indoorRecommend) {
            if (feed instanceof Indoor)
                indoorResponseDtoList.add(new IndoorResponseDto((Indoor) feed,
                        (int) commentRepository.findListById(feed).count(),
                        feedClapRepository.findClapAll(feed).size(),
                        feedClapRepository.findClap(user, feed).isPresent(),
                        followService.isFollow(userId, feed)));
            if (indoorResponseDtoList.size() > 5) break;
        }
        return new FeedListResponseDto(indoorResponseDtoList, 0);
    }
}
