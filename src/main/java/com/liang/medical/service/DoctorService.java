package com.liang.medical.service;

import com.liang.medical.entity.Doctor;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface DoctorService extends IService<Doctor> {

    List<Doctor> getByDepartment(String department);
}