package com.Liang.java.ai.langchain4j.mapper;

import com.Liang.java.ai.langchain4j.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}