package com.Liang.java.ai.langchain4j.mapper;

import com.Liang.java.ai.langchain4j.entity.Appointment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {

    /**
     * 查询用户在指定科室7天内的已有预约，用于冲突检测
     */
    List<Appointment> findRecentByDepartment(@Param("idCard") String idCard,
                                             @Param("department") String department);

    @Select("SELECT EXISTS(SELECT 1 FROM appointment WHERE user_id = #{userId} AND schedule_id = #{scheduleId})")
    boolean existsByUserIdAndScheduleId(@Param("userId") Long userId,
                                        @Param("scheduleId") Long scheduleId);
}
