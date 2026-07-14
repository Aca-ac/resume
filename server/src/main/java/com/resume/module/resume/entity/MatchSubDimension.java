package com.resume.module.resume.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("match_sub_dimension")
public class MatchSubDimension {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long dimensionId;
    private String name;
    private Integer score;
    private String description;
    private Integer sortOrder;
}
