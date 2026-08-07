package com.skala.fund.controller;

import com.skala.fund.common.response.ApiResponse;
import com.skala.fund.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "File", description = "이미지 업로드")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "이미지 업로드", description = "jpg/png/webp 만 허용. 저장된 이미지의 접근 URL 을 반환한다.")
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImageUploadResponse> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(new ImageUploadResponse(fileStorageService.store(file)));
    }

    public record ImageUploadResponse(String url) {}
}
