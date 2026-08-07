package com.skala.fund.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 저장을 추상화한다.
 * dev 는 로컬 파일시스템, prod 는 S3 구현체가 각각 @Profile 로 바인딩된다.
 */
public interface FileStorageService {

    /** 저장 후 외부에서 접근 가능한 URL 을 반환한다. */
    String store(MultipartFile file);
}
