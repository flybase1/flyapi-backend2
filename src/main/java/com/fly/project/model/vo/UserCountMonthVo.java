package com.fly.project.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserCountMonthVo implements Serializable {

    private Integer month;
    private Integer count;
}
