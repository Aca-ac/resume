package com.resume.module.resume.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DimensionScoreVO {
    private Long id;
    private String name;
    private Integer score;
    private BigDecimal weight;
    private String description;
    private String details;
    private List<SubDimensionVO> subDimensions;
}
