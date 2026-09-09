package com.liang.medical.mapper;

import com.liang.medical.entity.Doctor;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DoctorMapper extends BaseMapper<Doctor> {

    @Select("SELECT * FROM doctor WHERE user_id = #{userId}")
    Doctor findByUserId(@Param("userId") Long userId);
}
