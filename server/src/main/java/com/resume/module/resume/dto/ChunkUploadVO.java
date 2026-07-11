package com.resume.module.resume.dto;

import lombok.Data;

@Data
public class ChunkUploadVO {
    private String uploadId;
    private int chunkIndex;
    private boolean received;
}
