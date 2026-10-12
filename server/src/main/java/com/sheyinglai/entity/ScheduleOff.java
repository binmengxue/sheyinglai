package com.sheyinglai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/** 摄影师手动设置的休息日 */
@Data
@TableName("schedule_off")
public class ScheduleOff {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long photographerId;
    private LocalDate offDate;
}
