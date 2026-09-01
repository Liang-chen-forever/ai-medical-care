package com.Liang.java.ai.langchain4j.service;

import com.Liang.java.ai.langchain4j.entity.Appointment;

import java.util.List;

/**
 * 面向已登录用户的预约写入事务。
 */
public interface AppointmentBookingService {

    Appointment book(Long userId, Long scheduleId);

    List<Appointment> listMine(Long userId);

    void cancel(Long userId, Long appointmentId);
}
