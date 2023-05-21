package com.fly.project.utils;

import cn.hutool.core.util.StrUtil;
import com.fly.flyapicommon.model.entity.User;
import com.fly.project.constant.UserConstant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;


/**
 * 刷新token
 *
 */

public class RefreshTokenInterceptor implements HandlerInterceptor {

    public static final String SALT ="fly";
    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request == null){
            return true;
        }

        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj == null){
            return true;
        }

        // 1.获取请求头中的token
        String token = request.getHeader("x-auth-token");
        if (StrUtil.isBlank(token)) {
            return true;
        }

        User user = (User) userObj;
        String userToken = SALT + token + user.getId();
        String tokenKey = RedisConstants.LOGIN_TOKEN_KEY + userToken;

        String userId = stringRedisTemplate.opsForValue().get(tokenKey);


        if ( StringUtils.isEmpty(userId)){
            return true;
        }


        UserHolder.saveUser(userId);
        //刷新token
        stringRedisTemplate.expire(tokenKey, RedisConstants.LIST_USER_TIME, TimeUnit.MINUTES);


        //放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户
        UserHolder.removeUser();
    }
}
