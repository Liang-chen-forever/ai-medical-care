package com.liang.medical.service;

import com.liang.medical.entity.Schedule;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ScheduleService extends IService<Schedule> {

    List<Schedule> getAvailableSlots(String department, String date, String time);

    Schedule getByDoctorAndDate(Long doctorId, String date, String time);
}