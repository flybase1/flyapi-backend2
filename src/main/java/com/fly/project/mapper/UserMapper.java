package com.fly.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fly.flyapicommon.model.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * @Entity com.yupi.project.model.domain.User
 */
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT MONTH(createTime) AS month, COUNT(*) AS count " +
            "FROM user " +
            "WHERE createTime >= #{startDate} AND createTime < #{endDate} " +
            "GROUP BY MONTH(createTime) " +
            "ORDER BY MONTH(createTime)")
    List<Map<String, Object>> countUsersByMonth(@Param("startDate") String startDate, @Param("endDate") String endDate);
}




