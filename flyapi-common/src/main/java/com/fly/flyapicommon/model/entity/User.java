package com.fly.flyapicommon.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户
 *
 * @TableName user
 */
@TableName( value = "user" )
@Data
public class User implements Serializable {
    /**
     * id
     */
    @TableId( type = IdType.AUTO )
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
     * 密码
     */
    private String userPassword;

    /**
     * 手机号
     */
    private String phoneNum;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 标识
     */
    private String accessKey;

    /**
     * 密钥
     */
    private String secretKey;

    /**
     * 下载密钥次数
     */
    private Integer downloadCount;


    @TableField( exist = false )
    private static final long serialVersionUID = 1L;
}