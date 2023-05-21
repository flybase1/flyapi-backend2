package com.fly.project.model.dto.user;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class UserPersonInfo implements Serializable {

    private static final long serialVersionUID = -7348070200081832963L;

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 用户角色: user, admin
     */
    private String userRole;


    /**
     * 创建时间
     */
    private Date createTime;


    /**
     * 手机号
     */
    private String phoneNum;

    /**
     * 邮箱
     */
    private String email;


}
