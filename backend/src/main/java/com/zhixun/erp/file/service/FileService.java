package com.zhixun.erp.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.erp.file.entity.File;
import com.zhixun.erp.file.mapper.FileMapper;
import com.zhixun.erp.file.vo.FileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileMapper fileMapper;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    public File uploadFile(MultipartFile multipartFile, Long userId, String username) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalName = multipartFile.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }

        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String storedName = datePrefix + "_" + UUID.randomUUID().toString().replace("-", "") + extension;
        String filePath = uploadPath.resolve(storedName).toString();

        multipartFile.transferTo(Paths.get(filePath));

        File file = new File();
        file.setOriginalName(originalName);
        file.setStoredName(storedName);
        file.setFilePath(filePath);
        file.setFileSize(multipartFile.getSize());
        file.setFileType(multipartFile.getContentType());
        file.setUploadUserId(userId);
        file.setUploadUsername(username);
        file.setCreateTime(LocalDateTime.now());
        fileMapper.insert(file);

        return file;
    }

    public File getFileById(Long id) {
        return fileMapper.selectById(id);
    }

    public List<FileResponse> getFileList(Long filterUserId) {
        LambdaQueryWrapper<File> wrapper = new LambdaQueryWrapper<File>()
                .orderByDesc(File::getCreateTime);
        if (filterUserId != null) {
            wrapper.eq(File::getUploadUserId, filterUserId);
        }
        List<File> files = fileMapper.selectList(wrapper);
        return files.stream()
                .map(this::toFileResponse)
                .collect(Collectors.toList());
    }

    public Page<FileResponse> getFilePage(int pageNum, int pageSize) {
        Page<File> page = new Page<>(pageNum, pageSize);
        fileMapper.selectPage(page,
                new LambdaQueryWrapper<File>().orderByDesc(File::getCreateTime)
        );

        Page<FileResponse> result = new Page<>(pageNum, pageSize, page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(this::toFileResponse)
                .collect(Collectors.toList()));
        return result;
    }

    public void deleteFile(Long id) throws IOException {
        File file = fileMapper.selectById(id);
        if (file != null) {
            Path filePath = Paths.get(file.getFilePath());
            Files.deleteIfExists(filePath);
            fileMapper.deleteById(id);
        }
    }

    private FileResponse toFileResponse(File file) {
        return new FileResponse(
                file.getId(),
                file.getOriginalName(),
                file.getFileSize(),
                file.getFileType(),
                file.getUploadUsername(),
                file.getCreateTime()
        );
    }
}
