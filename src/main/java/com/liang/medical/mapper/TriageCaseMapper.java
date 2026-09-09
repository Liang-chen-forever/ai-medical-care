package com.liang.medical.mapper;

import com.liang.medical.entity.TriageCase;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TriageCaseMapper extends BaseMapper<TriageCase> {
    @Select("SELECT COUNT(*) FROM triage_case WHERE status = #{status}")
    long countByStatus(@Param("status") String status);
}
