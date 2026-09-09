package com.liang.medical.service.impl;

import com.liang.medical.entity.Schedule;
import com.liang.medical.mapper.ScheduleMapper;
import com.liang.medical.service.ScheduleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper, Schedule> implements ScheduleService {

    @Override
    public List<Schedule> getAvailableSlots(String department, String date, String time) {
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getDepartment, department);
        queryWrapper.eq(Schedule::getDate, date);
        queryWrapper.eq(Schedule::getTime, time);
        queryWrapper.gt(Schedule::getTotalSlots, 0);

        List<Schedule> schedules = baseMapper.selectList(queryWrapper);
        return schedules.stream()
                .filter(s -> s.getBookedSlots() == null || s.getBookedSlots() < s.getTotalSlots())
                .collect(Collectors.toList());
    }

    @Override
    public Schedule getByDoctorAndDate(Long doctorId, String date, String time) {
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getDoctorId, doctorId);
        queryWrapper.eq(Schedule::getDate, date);
        queryWrapper.eq(Schedule::getTime, time);
        return baseMapper.selectOne(queryWrapper);
    }
}