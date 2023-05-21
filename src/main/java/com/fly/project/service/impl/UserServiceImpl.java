package com.fly.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fly.flyapicommon.model.entity.InterfaceInfo;
import com.fly.flyapicommon.model.entity.User;
import com.fly.flyapicommon.model.entity.UserInterfaceInfo;
import com.fly.project.common.ErrorCode;
import com.fly.project.common.ResultUtils;
import com.fly.project.exception.BusinessException;
import com.fly.project.mapper.UserMapper;
import com.fly.project.model.dto.user.UserPersonInfo;
import com.fly.project.model.dto.user.UserQueryRequest;
import com.fly.project.model.dto.user.UserUpdateRequest;
import com.fly.project.model.vo.UserCountMonthVo;
import com.fly.project.model.vo.UserVO;
import com.fly.project.service.UserService;
import com.fly.project.utils.RedisConstants;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StreamUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.fly.project.constant.UserConstant.ADMIN_ROLE;
import static com.fly.project.constant.UserConstant.USER_LOGIN_STATE;


/**
 * 用户服务实现类
 *
 * @author yupi
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    UserInterfaceInfoServiceImpl userInterfaceInfoService;

    @Resource
    private InterfaceInfoServiceImpl interfaceInfoService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "fly";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = userMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

            // 分配ak, sk
            String accessKey = DigestUtil.md5Hex(SALT + userAccount + RandomUtil.randomNumbers(5));
            String secretKey = DigestUtil.md5Hex(SALT + userAccount + RandomUtil.randomNumbers(8));

            // 3. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);

            user.setAccessKey(accessKey);
            user.setSecretKey(secretKey);

            user.setUserAvatar("https://picsum.photos/200/300");

            String randomNumbers = RandomUtil.randomNumbers(5);
            StringBuilder stringBuffer = new StringBuilder();
            StringBuilder username = stringBuffer.append("user_").append(user.getUserAccount()).append("_").append(randomNumbers);
            user.setUserName(String.valueOf(username));

            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }

            return user.getId();
        }
    }

    @Override
    public UserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // todo 如何解决重复插入
        // 查询接口以及用户剩余次数，如果用户记录为空，就自动分配次数
        createMappingIfNotExists(user.getId());
        createNonExistUserInterfaceInfo(user.getId());
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);


        // 3. 记录用户的登录态
        HttpSession session = request.getSession();
        session.setAttribute(USER_LOGIN_STATE, user);
        //  System.out.println(session.getId());
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);

        // 记录在redis里面
        String userToken = SALT + ":" + userVO.getId();

        String tokenKey = RedisConstants.LOGIN_TOKEN_KEY + userToken;
        //用户信息保存在redis
        redisTemplate.opsForValue().set(tokenKey, userVO);
        //设置过期时间
        //todo 这里采用opsForValue().getOperations()
        redisTemplate.opsForValue().getOperations().expire(tokenKey, RedisConstants.LOGIN_TIME, TimeUnit.MINUTES);

        return userVO;

        //  return user;
    }

    /**
     * 用户注册完，第一次登录的时候，给与分配次数，但是这样存在一个问题，如果之后开发新的接口，用户就没办法获取调用次数
     *
     * @param userId
     */
    public void createMappingIfNotExists(Long userId) {
        QueryWrapper<UserInterfaceInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        // todo
        String key = RedisConstants.LIST_INTERFACE_SIZE + SALT + ":" + userId;

        List<UserInterfaceInfo> uid = userInterfaceInfoService.list(queryWrapper);
        if (uid.size() == 0) {
            List<InterfaceInfo> interfaceInfos = interfaceInfoService.list();
            List<UserInterfaceInfo> userInterfaceInfoList = interfaceInfos.stream()
                    .map(info -> {
                        UserInterfaceInfo userInterfaceInfo = new UserInterfaceInfo();
                        userInterfaceInfo.setInterfaceInfoId(info.getId());
                        userInterfaceInfo.setUserId(userId);
                        userInterfaceInfo.setLeftNum(20);
                        userInterfaceInfo.setTotalNum(0);
                        return userInterfaceInfo;
                    })
                    .collect(Collectors.toList());
            userInterfaceInfoService.saveBatch(userInterfaceInfoList);
        }
    }

    /**
     * 解决上面开发新接口用户次数的问题
     * (1)先查询用户在user_interface的集合
     * (2)再根据查询来的userId的集合查询相应接口集合
     * (3)再查询所有的接口信息，查询用户里面不存在的接口的信息
     * (4)设置相应参数
     *
     * @param userId
     */
    public void createNonExistUserInterfaceInfo(Long userId) {
        // 查询 userInterfaceInfo 表中所有符合条件的记录
        List<UserInterfaceInfo> uid = userInterfaceInfoService
                .lambdaQuery()
                .eq(UserInterfaceInfo::getUserId, userId)
                .list();

        // 根据 interfaceInfo 的 id 列表查询 uid 中存在的记录
        List<Long> existIdList = uid
                .stream()
                .map(UserInterfaceInfo::getInterfaceInfoId)
                .collect(Collectors.toList());


        List<InterfaceInfo> interfaceInfoList = interfaceInfoService.list();
        // 将 interfaceInfo 的 id 列表转换成 Set 类型
        Set<Long> interfaceIdSet = interfaceInfoList
                .stream()
                .map(InterfaceInfo::getId)
                .collect(Collectors.toSet());

        // 将 existIdList 转换成 Set 类型
        Set<Long> existIdSet = new HashSet<>(existIdList);
        // 通过求差集的方式得到不存在在 userInterfaceInfo 表中的 interfaceInfo 的 id 列表
        List<Long> nonExistIdList = interfaceIdSet
                .stream()
                .filter(id -> !existIdSet.contains(id))
                .collect(Collectors.toList());

        // 遍历 nonExistIdList 列表，为每一个 id 创建一条 UserInterfaceInfo 记录
        List<UserInterfaceInfo> userInterfaceInfoList = nonExistIdList.stream().map(id -> {
            UserInterfaceInfo userInterfaceInfo = new UserInterfaceInfo();
            userInterfaceInfo.setInterfaceInfoId(id);
            userInterfaceInfo.setUserId(userId);
            userInterfaceInfo.setLeftNum(20);
            userInterfaceInfo.setTotalNum(0);
            return userInterfaceInfo;
        }).collect(Collectors.toList());
        // 将创建的所有 UserInterfaceInfo 记录批量保存到 userInterfaceInfo 表中
        userInterfaceInfoService.saveBatch(userInterfaceInfoList);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //todo redis
        String tokenKey = SALT + currentUser.getId();
        UserVO userVO = (UserVO) redisTemplate.opsForValue().get(RedisConstants.LOGIN_TOKEN_KEY + tokenKey);
        if (userVO != null) {
            BeanUtils.copyProperties(userVO, currentUser);
            return currentUser;
        }

        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR,"请重新登录");
        }

        //request.getSession().setAttribute(USER_LOGIN_STATE, currentUser);

        return currentUser;
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && ADMIN_ROLE.equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public boolean canForgetPassword(String userAccount, String newUserPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, newUserPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (newUserPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "还未注册");
        }
        if (user.getUserPassword().equals(newUserPassword)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "重置密码不能和原密码一样");
        }

        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + newUserPassword).getBytes());
        user.setUserPassword(encryptPassword);

        int success = userMapper.updateById(user);
        if (success <= 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改失败，请联系管理员");
        }

        return success > 0;
    }

    @Override
    public UserPersonInfo getUserPersonInfo(Long userId, HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (!loginUser.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        UserPersonInfo userPersonInfo = new UserPersonInfo();
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("id", userId);
        User user = this.getById(userQueryWrapper);
        BeanUtils.copyProperties(user, userPersonInfo);
        return userPersonInfo;
    }

    @Override
    public Page<UserVO> listPageUsers(UserQueryRequest userQueryRequest, HttpServletRequest request) {
        long current = 1;
        long size = 10;
        User queryUser = new User();
        if (userQueryRequest != null) {
            BeanUtils.copyProperties(userQueryRequest, queryUser);
            current = userQueryRequest.getCurrent();
            size = userQueryRequest.getPageSize();
        }
        User loginUser = getLoginUser(request);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        String key = RedisConstants.LIST_USERS_KEY + loginUser.getId();
        String data = (String) redisTemplate.opsForValue().get(key);
        if (data != null) {
            return deserialize(data);
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>(queryUser);
        Page<User> userPage = this.page(new Page<>(current, size), queryWrapper);


        Page<UserVO> voPage = new PageDTO<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<UserVO> userVoList = userPage.getRecords().stream().map(user -> {
            UserVO userVo = new UserVO();
            BeanUtils.copyProperties(user, userVo);
            return userVo;
        }).collect(Collectors.toList());

        voPage.setRecords(userVoList);
        redisTemplate.opsForValue().set(key, serialize(voPage));

        return voPage;
    }

    @Override
    public String getSk(Long userId, HttpServletRequest request) {
        User user = this.getById(userId);
        return user.getSecretKey();
    }

    @Override
    public boolean updateByUserId(UserUpdateRequest userUpdateRequest) {
        String phonePattern = "^(?:(?:\\+|00)86)?1[3-9]\\d{9}$";
        String emailPattern = "^([a-zA-Z\\d][\\w-]{2,})@(\\w{2,})\\.([a-z]{2,})(\\.[a-z]{2,})?$";
        String phoneNum = userUpdateRequest.getPhoneNum();
        String email = userUpdateRequest.getEmail();
        String userName = userUpdateRequest.getUserName();
        if (phoneNum != null) {
            boolean matches = phoneNum.matches(phonePattern);
            if (!matches) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式错误");
            }
        }
        if (email != null) {
            boolean matches = email.matches(emailPattern);
            if (!matches) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式错误");
            }
        }

        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);

        return this.updateById(user);
    }

    @Override
    public UserVO userPhoneLogin(String phoneNum, String code, HttpServletRequest request) {
        if (phoneNum == null || code == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号或者验证码不能为空");
        }
        //开头数字必须为1，第二位必须为3至9之间的数字，后九尾必须为0至9组织成的十一位电话号码
        String mobileRegEx = "^1[3,4,5,6,7,8,9][0-9]{9}$";
        if (!ReUtil.isMatch(mobileRegEx, phoneNum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式错误！");
        }

        // 3.从redis获取验证码并校验
        String cacheCode = (String) redisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phoneNum);
        if (cacheCode == null || !cacheCode.equals(code)) {
            // 不一致，报错
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误！");
        }

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phoneNum", phoneNum);
        User user = getOne(queryWrapper);
        UserVO userVO = new UserVO();
        // 未注册登陆过
        if (user == null) {
            synchronized (phoneNum.intern()) {
                User newUser = new User();
                String userAccount = "user_phone_" + RandomUtil.randomNumbers(5);
                newUser.setUserAccount(userAccount);
                newUser.setUserName("user_phone_" + RandomUtil.randomNumbers(5));
                newUser.setUserAvatar("https://picsum.photos/200/300");
                newUser.setUserPassword(RandomUtil.randomNumbers(8));
                // 分配ak, sk
                String accessKey = DigestUtil.md5Hex(SALT + userAccount + RandomUtil.randomNumbers(5));
                String secretKey = DigestUtil.md5Hex(SALT + userAccount + RandomUtil.randomNumbers(8));
                newUser.setAccessKey(accessKey);
                newUser.setSecretKey(secretKey);
                boolean success = this.save(newUser);
                if (!success) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "服务错误，请稍后再试");
                }
                userVO = new UserVO();
                BeanUtils.copyProperties(newUser, userVO);
            }
            request.setAttribute(USER_LOGIN_STATE, userVO);
            return userVO;
        }
        // 注册过，直接登录
        BeanUtils.copyProperties(userVO, userVO);
        request.setAttribute(USER_LOGIN_STATE, userVO);
        return userVO;
    }

    @Override
    public String sendCode(String phoneNum) {
        if (phoneNum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入手机号");
        }

        String code = RandomUtil.randomNumbers(4);
        String key = RedisConstants.LOGIN_CODE_KEY + phoneNum;
        redisTemplate.opsForValue().set(key, code, RedisConstants.LOGIN_CODE_TIME, TimeUnit.MINUTES);
        return code;
    }

    @Override
    public ResponseEntity<byte[]> downloadSk(HttpServletResponse response, HttpServletRequest request) {
        User loginUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", loginUser.getUserAccount());
        User user = this.getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        String userAccount = user.getUserAccount();
        String secretKey = user.getSecretKey();
        String accessKey = user.getAccessKey();
        Integer downloadCount = user.getDownloadCount();

        String csvData = "userAccount,accessKey,secretKey\n" +
                userAccount + "," +
                accessKey + "," +
                secretKey;
        if (downloadCount <=0){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"您的下载次数已到达上线，如需继续请联系管理员");
        }
        //response.setContentType("application/octet-stream")
        response.setContentType("text/csv; charset=UTF-8");
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"credentials.csv\"");

        try {
            byte[] csvBytes = csvData.getBytes();
            int count = user.getDownloadCount() - 1;
            if (count >= 0) {
                user.setDownloadCount(count);
                // 保存用户数据（根据具体情况调用对应的方法）
                this.updateById(user);
            }
            return ResponseEntity
                    .ok()
                    .contentLength(csvBytes.length)
                    .body(csvBytes);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }

    @Override
    public List<Map<String, Object>> getUserRegisterOrderByMonth() {
        //SELECT MONTH(createTime) AS month, COUNT(*) AS count
        //FROM user
        //WHERE createTime >= '2023-01-01' AND createTime < '2024-01-01'
        //GROUP BY MONTH(createTime)
        //ORDER BY MONTH(createTime);
        String key = RedisConstants.COUNT_USER_REGISTER;
        List<Map<String,Object>> result = (List<Map<String, Object>>) redisTemplate.opsForValue().get(key);
        if (result != null){
            return result;
        }
        result = userMapper.countUsersByMonth("2023-01-01", "2024-01-01");
        redisTemplate.opsForValue().set(key,result,RedisConstants.COUNT_USER_REGISTER_TIME,TimeUnit.MINUTES);
        return result;
    }


    private String serialize(Page<UserVO> page) {
        Gson gson = new Gson();
        return gson.toJson(page);
    }

    private Page<UserVO> deserialize(String serializedData) {
        Gson gson = new Gson();
        Type type = new TypeToken<PageDTO<UserVO>>() {}.getType();
        return gson.<PageDTO<UserVO>>fromJson(serializedData, type);
    }


}




