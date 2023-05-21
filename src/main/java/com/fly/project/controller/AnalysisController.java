package com.fly.project.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.fly.flyapicommon.model.entity.InterfaceInfo;
import com.fly.flyapicommon.model.entity.UserInterfaceInfo;
import com.fly.project.annotation.AuthCheck;
import com.fly.project.common.BaseResponse;
import com.fly.project.common.ErrorCode;
import com.fly.project.common.ResultUtils;
import com.fly.project.exception.BusinessException;
import com.fly.project.mapper.UserInterfaceInfoMapper;
import com.fly.project.model.vo.InterfaceInfoVo;
import com.fly.project.model.vo.UserCountMonthVo;
import com.fly.project.service.InterfaceInfoService;
import com.fly.project.service.UserService;
import com.fly.project.utils.RedisConstants;
import io.swagger.models.auth.In;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 分析控制器
 */
@RestController
@RequestMapping( "/analysis" )
public class AnalysisController {

    @Resource
    private UserInterfaceInfoMapper userInterfaceInfoMapper;
    @Resource
    private InterfaceInfoService interfaceInfoService;
    @Resource
    private RedisTemplate<String,Object> redisTemplate;
    @Resource
    private UserService userService;

    @GetMapping( "/top/interface/invoke" )
    @AuthCheck( anyRole = "admin" )
    public BaseResponse<List<InterfaceInfoVo>> listTopInvokeInterfaceInfo() {
        String key = RedisConstants.COUNT_LIST_INTERFACE;
        List<InterfaceInfoVo> infoVoList = (List<InterfaceInfoVo>)redisTemplate.opsForValue().get(key);
        if (infoVoList!=null){
            return ResultUtils.success(infoVoList);
        }

        List<UserInterfaceInfo> userInterfaceInfoList = userInterfaceInfoMapper.listTopInvokeInterfaceInfo(3);
        Map<Long, List<UserInterfaceInfo>> interfaceInfoObjMap = userInterfaceInfoList.stream().collect(Collectors.groupingBy(UserInterfaceInfo::getInterfaceInfoId));

        QueryWrapper<InterfaceInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id", interfaceInfoObjMap.keySet());
        List<InterfaceInfo> interfaceInfoList = interfaceInfoService.list(queryWrapper);
        if (CollectionUtils.isEmpty(interfaceInfoList)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

         infoVoList = interfaceInfoList.stream().map(interfaceInfo -> {
            InterfaceInfoVo interfaceInfoVo = new InterfaceInfoVo();
            BeanUtils.copyProperties(interfaceInfo, interfaceInfoVo);
            int totalNum = interfaceInfoObjMap.get(interfaceInfo.getId()).get(0).getTotalNum();
            interfaceInfoVo.setTotalNum(totalNum);
            return interfaceInfoVo;
        }).collect(Collectors.toList());

        redisTemplate.opsForValue().set(key,infoVoList,RedisConstants.COUNT_LIST_INTERFACE_TIME, TimeUnit.MINUTES);

        return ResultUtils.success(infoVoList);
    }


    @GetMapping( "/count/orderbymonth" )
    @AuthCheck( anyRole = "admin" )
    public BaseResponse<List<Map<String, Object>>> getUserRegisterOrderByMonth(){
       return  ResultUtils.success(userService.getUserRegisterOrderByMonth());
    }

}
