package com.resume.module.resume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("match_dimension")
public class MatchDimension {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long analysisId;
    private String dimensionKey;
    private String dimensionName;
    private Integer score;
    private BigDecimal weight;
    private String description;
    private String details;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
