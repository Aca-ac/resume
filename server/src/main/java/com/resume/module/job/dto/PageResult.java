package com.resume.module.job.dto;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> content;
    private long total;
    private long page;
    private long size;

    public PageResult(List<T> content, long total, long page, long size) {
        this.content = content;
        this.total = total;
        this.page = page;
        this.size = size;
    }
}
