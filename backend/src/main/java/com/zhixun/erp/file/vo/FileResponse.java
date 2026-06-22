package com.zhixun.erp.file.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {

    private Long id;

    private String originalName;

    private Long fileSize;

    private String fileType;

    private String uploadUsername;

    private LocalDateTime createTime;
}
