package com.fly.project.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求体
 *
 * @author yupi
 */
@Data
public class UserPhoneLoginRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    private String phoneNum;

    private String code;
}
