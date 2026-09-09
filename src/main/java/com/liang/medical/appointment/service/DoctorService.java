package com.liang.medical.appointment.service;

import com.liang.medical.appointment.entity.Doctor;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface DoctorService extends IService<Doctor> {

    List<Doctor> getByDepartment(String department);
}