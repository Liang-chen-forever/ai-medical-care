package com.liang.medical.service.impl;

import com.liang.medical.entity.Appointment;
import com.liang.medical.mapper.AppointmentMapper;
import com.liang.medical.service.AppointmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentServiceImpl.class);

    @Override
    public Appointment getOne(Appointment appointment) {
        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Appointment::getUsername, appointment.getUsername());
        queryWrapper.eq(Appointment::getIdCard, appointment.getIdCard());
        queryWrapper.eq(Appointment::getDepartment, appointment.getDepartment());
        queryWrapper.eq(Appointment::getDate, appointment.getDate());
        queryWrapper.eq(Appointment::getTime, appointment.getTime());

        Appointment appointmentDB = baseMapper.selectOne(queryWrapper);
        return appointmentDB;
    }

    @Override
    public List<Appointment> checkDepartmentConflict(String idCard, String department) {
        return baseMapper.findRecentByDepartment(idCard, department);
    }

    @Override
    public Appointment findByUserAndDepartment(String username, String idCard, String department) {
        LambdaQueryWrapper<Appointment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Appointment::getUsername, username);
        queryWrapper.eq(Appointment::getIdCard, idCard);
        queryWrapper.eq(Appointment::getDepartment, department);
        queryWrapper.orderByDesc(Appointment::getDate);
        queryWrapper.last("LIMIT 1");
        return baseMapper.selectOne(queryWrapper);
    }
}