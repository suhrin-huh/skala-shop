package com.skala.fund.service;

import com.skala.fund.common.exception.CustomException;
import com.skala.fund.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * dev 전용. ./uploads 에 UUID 파일명으로 저장하고 /images/** 경로로 서빙한다.
 * 원본 파일명을 그대로 쓰면 경로 조작(../)과 파일명 충돌이 생기므로 확장자만 남기고 버린다.
 */
@Slf4j
@Profile("dev")
@Service
public class LocalFileStorageService implements FileStorageService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp");

    private final Path uploadRoot;

    public LocalFileStorageService(@Value("${app.storage.local-path:./uploads}") String localPath) {
        this.uploadRoot = Paths.get(localPath).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.EMPTY_FILE);
        }

        String extension = resolveExtension(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "." + extension;

        try {
            Files.createDirectories(uploadRoot);
            Path target = uploadRoot.resolve(storedName);
            try (var input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("이미지 저장 실패 - originalName={}", file.getOriginalFilename(), e);
            throw new CustomException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        return "/images/" + storedName;
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
