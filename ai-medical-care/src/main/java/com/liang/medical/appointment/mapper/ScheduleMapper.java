package com.liang.medical.appointment.mapper;

import com.liang.medical.appointment.entity.Schedule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {

    @Update("UPDATE schedule SET booked_slots = booked_slots + 1 " +
            "WHERE id = #{scheduleId} AND booked_slots < total_slots")
    int decrementIfAvailable(@Param("scheduleId") Long scheduleId);

    @Update("UPDATE schedule SET booked_slots = booked_slots - 1 " +
            "WHERE id = #{scheduleId} AND booked_slots > 0")
    int incrementIfBooked(@Param("scheduleId") Long scheduleId);
}
