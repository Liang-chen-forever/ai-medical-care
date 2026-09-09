package com.liang.medical.service.impl;

import com.liang.medical.entity.Doctor;
import com.liang.medical.mapper.DoctorMapper;
import com.liang.medical.service.DoctorService;
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