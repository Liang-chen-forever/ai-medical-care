package com.Liang.java.ai.langchain4j.service.impl;

import com.Liang.java.ai.langchain4j.entity.Doctor;
import com.Liang.java.ai.langchain4j.mapper.DoctorMapper;
import com.Liang.java.ai.langchain4j.service.DoctorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoctorServiceImpl extends ServiceImpl<DoctorMapper, Doctor> implements DoctorService {

    @Override
    public List<Doctor> getByDepartment(String department) {
        LambdaQueryWrapper<Doctor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Doctor::getDepartment, department);
        return baseMapper.selectList(queryWrapper);
    }
}