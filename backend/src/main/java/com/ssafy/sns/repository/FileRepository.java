package com.ssafy.sns.repository;

import com.ssafy.sns.domain.file.File;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository<File, Long> {

    List<File> findAllByFeedId(Long id);


}
