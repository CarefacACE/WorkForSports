package com.zhixun.erp.file.controller;

import com.zhixun.erp.common.response.Result;
import com.zhixun.erp.file.entity.File;
import com.zhixun.erp.file.service.FileService;
import com.zhixun.erp.file.vo.FileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@RequestMapping("/file")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public Result<FileResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId,
            @RequestParam("username") String username) throws IOException {
        File uploaded = fileService.uploadFile(file, userId, username);
        return Result.success("上传成功", new FileResponse(
                uploaded.getId(),
                uploaded.getOriginalName(),
                uploaded.getFileSize(),
                uploaded.getFileType(),
                uploaded.getUploadUsername(),
                uploaded.getCreateTime()
        ));
    }

    @GetMapping("/list")
    public Result<List<FileResponse>> getFileList(
            @RequestParam(value = "userId", required = false) Long userId) {
        return Result.success(fileService.getFileList(userId));
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) throws IOException {
        File file = fileService.getFileById(id);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file.getFilePath());
        String encodedFileName = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getFileType() != null ? file.getFileType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteFile(@PathVariable Long id) throws IOException {
        fileService.deleteFile(id);
        return Result.success("删除成功", null);
    }
}
