package com.sheyinglai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sheyinglai.entity.StatDaily;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

public interface StatDailyMapper extends BaseMapper<StatDaily> {

    /** column 只能是 exposure / profile_view / consult，由 StatService 写死，不接收外部输入 */
    @Insert("INSERT INTO stat_daily (photographer_id, stat_date, ${column}) VALUES (#{pid}, #{date}, 1) "
            + "ON DUPLICATE KEY UPDATE ${column} = ${column} + 1")
    void incr(@Param("pid") Long photographerId, @Param("date") LocalDate date, @Param("column") String column);
}
