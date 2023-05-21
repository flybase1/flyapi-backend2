package com.fly.project.service;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fly.flyapicommon.model.entity.User;
import com.fly.project.model.dto.user.UserPersonInfo;
import com.fly.project.model.dto.user.UserQueryRequest;
import com.fly.project.model.dto.user.UserUpdateRequest;
import com.fly.project.model.vo.UserCountMonthVo;
import com.fly.project.model.vo.UserVO;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 用户服务
 *
 * @author yupi
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    UserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);


    /**
     * 忘记密码
     *
     * @param userAccount     用户账号
     * @param newUserPassword 用户新密码
     * @param checkPassword
     * @return
     */
    boolean canForgetPassword(String userAccount, String newUserPassword, String checkPassword);

    /**
     * @param userId
     */
    UserPersonInfo getUserPersonInfo(Long userId, HttpServletRequest request);

    /**
     * 展示用户列表
     * @param userQueryRequest
     * @param request
     * @return
     */

    Page<UserVO> listPageUsers(UserQueryRequest userQueryRequest, HttpServletRequest request);

    /**
     * 获取用户sk
     * @param userId
     * @param request
     * @return
     */
    String getSk(Long userId, HttpServletRequest request);

    /**
     * 修改用户信息
     * @param userUpdateRequest
     * @return
     */
    boolean updateByUserId(UserUpdateRequest userUpdateRequest);

    /**
     * 手机号验证码登录
     * @param phoneNum
     * @param code
     * @param request
     * @return
     */
    UserVO userPhoneLogin(String phoneNum, String code, HttpServletRequest request);

    /**
     * 共发送验证码
     * @param phoneNum
     * @return
     */
    String sendCode(String phoneNum);

    /**
     * 下载用户Sk和ak
     * @param request
     * @return
     */
    ResponseEntity<byte[]> downloadSk(HttpServletResponse response, HttpServletRequest request);

    /**
     * 统计每个月份用户注册人数
     * @return
     */
    List<Map<String, Object>> getUserRegisterOrderByMonth();
}
