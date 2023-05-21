package com.fly.project.controller;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.fly.flyapicommon.model.entity.User;
import com.fly.project.common.BaseResponse;
import com.fly.project.common.DeleteRequest;
import com.fly.project.common.ErrorCode;
import com.fly.project.common.ResultUtils;
import com.fly.project.exception.BusinessException;
import com.fly.project.model.dto.user.*;
import com.fly.project.model.vo.UserVO;
import com.fly.project.service.UserService;
import com.fly.project.utils.RedisConstants;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 用户接口
 *
 * @author yupi
 */
@RestController
@RequestMapping( "/user" )
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    // region 登录相关

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @PostMapping( "/register" )
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();

        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            return null;
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping( "/login" )
    public BaseResponse<UserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserVO user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    /**
     * 用户手机号登录
     *
     * @param userphoneLoginRequest
     * @param request
     * @return
     */
    @PostMapping( "/phone/login" )
    public BaseResponse<UserVO> phoneLogin(@RequestBody UserPhoneLoginRequest userphoneLoginRequest, HttpServletRequest request) {
        if (userphoneLoginRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String phoneNum = userphoneLoginRequest.getPhoneNum();
        String code = userphoneLoginRequest.getCode();

        if (StringUtils.isAnyBlank(phoneNum, code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        UserVO user = userService.userPhoneLogin(phoneNum, code, request);
        return ResultUtils.success(user);
    }

    /**
     * 发送验证码
     *
     * @return
     */
    @GetMapping( "/sendCode" )
    public BaseResponse<String> sendCode(String phoneNum) {
        String code = userService.sendCode(phoneNum);
        return ResultUtils.success(code);
    }


    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @PostMapping( "/logout" )
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @GetMapping( "/get/login" )
    public BaseResponse<UserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return ResultUtils.success(userVO);
    }

    // endregion

    // region 增删改查

    /**
     * 创建用户
     *
     * @param userAddRequest
     * @param request
     * @return
     */
    @PostMapping( "/add" )
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest, HttpServletRequest request) {
        if (userAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        boolean result = userService.save(user);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(user.getId());
    }

    /**
     * 删除用户
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping( "/delete" )
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     *
     * @param userUpdateRequest
     * @param request
     * @return
     */
    @PostMapping( "/update" )
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.updateByUserId(userUpdateRequest);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取用户
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping( "/get" )
    public BaseResponse<UserVO> getUserById(int id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getById(id);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return ResultUtils.success(userVO);
    }

    /**
     * 获取用户列表
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @GetMapping( "/list" )
    public BaseResponse<List<UserVO>> listUser(UserQueryRequest userQueryRequest, HttpServletRequest request) {
        User userQuery = new User();
        if (userQueryRequest != null) {
            BeanUtils.copyProperties(userQueryRequest, userQuery);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>(userQuery);
        List<User> userList = userService.list(queryWrapper);
        List<UserVO> userVOList = userList.stream().map(user -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            return userVO;
        }).collect(Collectors.toList());
        return ResultUtils.success(userVOList);
    }

    /**
     * 分页获取用户列表
     *
     * @param userQueryRequest
     * @param request
     * @return
     */
    @GetMapping( "/list/page" )
    public BaseResponse<Page<UserVO>> listUserByPage(UserQueryRequest userQueryRequest, HttpServletRequest request) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Page<UserVO> listPageUsers = userService.listPageUsers(userQueryRequest, request);
        return ResultUtils.success(listPageUsers);
    }


    @PostMapping( "/forgetPassword" )
    public BaseResponse<Boolean> forgetPassword(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();

        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            return null;
        }

        boolean success = userService.canForgetPassword(userAccount, userPassword, checkPassword);
        if (!success) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改失败");
        }

        return ResultUtils.success(success);
    }

    @GetMapping( "/get/developer" )
    public BaseResponse<UserPersonInfo> developerInfo(Long userId, HttpServletRequest request) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        UserPersonInfo userAccessKey = userService.getUserPersonInfo(userId, request);
        return ResultUtils.success(userAccessKey);
    }

    // 下载sk，ak
    @GetMapping( "/get/sk" )
    public BaseResponse<String> getSk(Long userId, HttpServletRequest request) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        String key = userService.getSk(userId, request);
        return ResultUtils.success(key);
    }

    /**
     * 下载sdk
     *
     * @param response
     * @throws FileNotFoundException
     */
    @GetMapping( "/downLoad" )
    public BaseResponse<Boolean> downLoad(HttpServletRequest request, HttpServletResponse response) throws FileNotFoundException {
        // 下载本地文件
        String fileName = "flyapi-client-sdk-0.0.1.jar".toString(); // 文件的默认保存名
        // 读到流中
        InputStream inStream = new FileInputStream("D:\\fly\\project\\OpenAPI\\flyAPI-backend\\flyapi-backend\\flyapi-client-sdk\\target\\flyapi-client-sdk-0.0.1.jar");// 文件的存放路径
        // 设置输出的格式
        response.reset();
        response.addHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.addHeader("Access-Control-Allow-Origin", "http://localhost:8000");//允许所有来源访同
        response.addHeader("Access-Control-Allow-Method", "POST,GET");//允许访问的方式

        // 循环取出流中的数据
        byte[] b = new byte[100];
        int len = 0;
        try {
            while (true) {
                try {
                    if (!((len = inStream.read(b)) > 0)) break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                response.getOutputStream().write(b, 0, len);
            }
            inStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("success");

        return ResultUtils.success(true);
    }


    @GetMapping( "/develop/download" )
    public BaseResponse<ResponseEntity<byte[]>> downloadSk(HttpServletResponse response, HttpServletRequest request) {
        ResponseEntity<byte[]> responseEntity = userService.downloadSk(response, request);
        return ResultUtils.success(responseEntity);
    }
}
