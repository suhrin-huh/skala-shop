package com.skala.fund.service;

import com.skala.fund.common.exception.CustomException;
import com.skala.fund.common.exception.ErrorCode;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * S3 에 UUID 파일명으로 저장하고 정적 버킷 URL 을 반환한다.
 * 버킷은 퍼블릭 리드 정책(images/* prefix)이 걸려 있다고 가정한다 — 업로드 시 ACL 은 지정하지 않는다.
 * (2023년 이후 신규 버킷은 Object Ownership 이 기본적으로 "Bucket owner enforced" 라 ACL 지정 자체가 실패한다.)
 */
@Slf4j
@Service
public class S3FileStorageService implements FileStorageService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp");
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp"
    );

    private final S3Template s3Template;
    private final String bucket;
    private final String region;

    public S3FileStorageService(
            S3Template s3Template,
            @Value("${app.storage.s3-bucket}") String bucket,
            @Value("${spring.cloud.aws.region.static}") String region) {
        this.s3Template = s3Template;
        this.bucket = bucket;
        this.region = region;
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }

        String extension = resolveExtension(file.getOriginalFilename());
        String key = "images/" + UUID.randomUUID() + "." + extension;

        try (var input = file.getInputStream()) {
            ObjectMetadata metadata = ObjectMetadata.builder()
                    .contentType(CONTENT_TYPES.get(extension))
                    .build();
            s3Template.upload(bucket, key, input, metadata);
        } catch (IOException | SdkException e) {
            log.error("S3 이미지 업로드 실패 - key={}, originalName={}", key, file.getOriginalFilename(), e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        return "https://%s.s3.%s.amazonaws.com/%s".formatted(bucket, region, key);
    }

    private String resolveExtension(String originalFilename) {
        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension == null) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }
        String normalized = extension.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(normalized)) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
        }
        return normalized;
    }
}
