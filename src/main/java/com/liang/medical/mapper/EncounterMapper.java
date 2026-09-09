package com.liang.medical.mapper;

import com.liang.medical.entity.Encounter;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EncounterMapper extends BaseMapper<Encounter> {
    @Select("SELECT * FROM encounter WHERE appointment_id = #{appointmentId}")
    Encounter findByAppointmentId(Long appointmentId);
}
