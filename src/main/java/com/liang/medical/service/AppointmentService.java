package com.liang.medical.service;

import com.liang.medical.entity.Appointment;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AppointmentService extends IService<Appointment> {

    Appointment getOne(Appointment appointment);

    /**
     * 检查用户在指定科室7天内是否已有预约，返回冲突的预约列表
     * @return 冲突的预约列表，为空表示无冲突
     */
    List<Appointment> checkDepartmentConflict(String idCard, String department);

    /**
     * 根据用户名+身份证号+科室查找预约（不依赖日期时间，用于取消预约等场景）
     * @return 匹配的预约，如果有多个返回最近的一个
     */
    Appointment findByUserAndDepartment(String username, String idCard, String department);
}
