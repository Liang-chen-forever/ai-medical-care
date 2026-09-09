package com.liang.medical.mapper;

import com.liang.medical.appointment.AppointmentStatus;
import com.liang.medical.entity.Appointment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    @Update("UPDATE appointment SET status = #{to}, handled_by = #{handledBy}, " +
            "handled_at = CURRENT_TIMESTAMP, cancel_reason = #{reason} " +
            "WHERE id = #{id} AND status = #{from}")
    int transitionStatus(@Param("id") Long id, @Param("from") AppointmentStatus from,
                         @Param("to") AppointmentStatus to, @Param("handledBy") Long handledBy,
                         @Param("reason") String reason);

    List<Appointment> findForDoctor(@Param("doctorUserId") Long doctorUserId,
                                    @Param("status") AppointmentStatus status);

    @Select("SELECT COUNT(*) FROM appointment WHERE status = #{status}")
    long countByStatus(@Param("status") AppointmentStatus status);
}
