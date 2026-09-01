package com.Liang.java.ai.langchain4j.service;

import com.Liang.java.ai.langchain4j.common.BusinessException;
import com.Liang.java.ai.langchain4j.entity.Appointment;
import com.Liang.java.ai.langchain4j.entity.Schedule;
import com.Liang.java.ai.langchain4j.entity.User;
import com.Liang.java.ai.langchain4j.mapper.AppointmentMapper;
import com.Liang.java.ai.langchain4j.mapper.ScheduleMapper;
import com.Liang.java.ai.langchain4j.mapper.UserMapper;
import com.Liang.java.ai.langchain4j.service.impl.AppointmentBookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

class AppointmentBookingServiceImplTest {

    private ScheduleMapper scheduleMapper;
    private AppointmentMapper appointmentMapper;
    private UserMapper userMapper;
    private AppointmentBookingService bookingService;

    @BeforeEach
    void setUp() {
        scheduleMapper = mock(ScheduleMapper.class);
        appointmentMapper = mock(AppointmentMapper.class);
        userMapper = mock(UserMapper.class);
        bookingService = new AppointmentBookingServiceImpl(scheduleMapper, appointmentMapper, userMapper);
    }

    @Test
    void bookingRejectsAFullScheduleWithoutCreatingAnAppointment() {
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(scheduleMapper.selectById(101L)).thenReturn(schedule(101L, 20, 20));
        when(appointmentMapper.existsByUserIdAndScheduleId(7L, 101L)).thenReturn(false);
        when(scheduleMapper.decrementIfAvailable(101L)).thenReturn(0);

        assertThatThrownBy(() -> bookingService.book(7L, 101L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该时段号源已约满");

        verify(appointmentMapper, never()).insert(any(Appointment.class));
    }

    @Test
    void cancellationRejectsAnotherUsersAppointmentWithoutRestoringCapacity() {
        Appointment appointment = new Appointment();
        appointment.setUserId(8L);
        appointment.setScheduleId(101L);
        when(appointmentMapper.selectById(55L)).thenReturn(appointment);

        assertThatThrownBy(() -> bookingService.cancel(7L, 55L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权操作该预约");

        verify(scheduleMapper, never()).incrementIfBooked(anyLong());
    }

    @Test
    void bookingPersistsUserAndScheduleSnapshotsAfterAtomicallyDecrementingCapacity() {
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(scheduleMapper.selectById(101L)).thenReturn(schedule(101L, 20, 3));
        when(appointmentMapper.existsByUserIdAndScheduleId(7L, 101L)).thenReturn(false);
        when(scheduleMapper.decrementIfAvailable(101L)).thenReturn(1);
        when(appointmentMapper.insert(any(Appointment.class))).thenAnswer(invocation -> 1);

        Appointment appointment = bookingService.book(7L, 101L);

        assertThat(appointment.getUserId()).isEqualTo(7L);
        assertThat(appointment.getScheduleId()).isEqualTo(101L);
        assertThat(appointment.getUsername()).isEqualTo("Alice");
        assertThat(appointment.getIdCard()).isEqualTo("11010519491231002X");
        assertThat(appointment.getDoctorName()).isEqualTo("张医生");
        verify(appointmentMapper).insert(appointment);
    }

    private Schedule schedule(Long id, int totalSlots, int bookedSlots) {
        Schedule schedule = new Schedule();
        schedule.setId(id);
        schedule.setTotalSlots(totalSlots);
        schedule.setBookedSlots(bookedSlots);
        schedule.setDoctorName("张医生");
        schedule.setDepartment("神经内科");
        schedule.setDate("2026-09-02");
        schedule.setTime("上午");
        return schedule;
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("Alice");
        user.setIdCard("11010519491231002X");
        return user;
    }
}
