package com.Liang.java.ai.langchain4j.service;

import com.Liang.java.ai.langchain4j.entity.Doctor;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface DoctorService extends IService<Doctor> {

    List<Doctor> getByDepartment(String department);
}