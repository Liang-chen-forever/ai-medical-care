package com.liang.medical.service;

import com.liang.medical.waitlist.service.WaitlistService;
import com.liang.medical.appointment.entity.AppointmentStatus;
import com.liang.medical.common.BusinessException;
import com.liang.medical.appointment.entity.Appointment;
import com.liang.medical.appointment.entity.Schedule;
import com.liang.medical.auth.entity.User;
import com.liang.medical.waitlist.entity.WaitlistEntry;
import com.liang.medical.appointment.mapper.AppointmentMapper;
import com.liang.medical.appointment.mapper.ScheduleMapper;
import com.liang.medical.triage.mapper.TriageCaseMapper;
import com.liang.medical.auth.mapper.UserMapper;
import com.liang.medical.waitlist.mapper.WaitlistEntryMapper;
import com.liang.medical.waitlist.service.WaitlistServiceImpl;
import com.liang.medical.triage.TriageRiskLevel;
import com.liang.medical.waitlist.entity.WaitlistStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WaitlistServiceImplTest {
    private ScheduleMapper scheduleMapper;
    private WaitlistEntryMapper waitlistMapper;
    private AppointmentMapper appointmentMapper;
    private UserMapper userMapper;
    private TriageCaseMapper triageCaseMapper;
    private WaitlistService service;

    @BeforeEach
    void setUp() {
        scheduleMapper = mock(ScheduleMapper.class);
        waitlistMapper = mock(WaitlistEntryMapper.class);
        appointmentMapper = mock(AppointmentMapper.class);
        userMapper = mock(UserMapper.class);
        triageCaseMapper = mock(TriageCaseMapper.class);
        service = new WaitlistServiceImpl(scheduleMapper, waitlistMapper, appointmentMapper, userMapper, triageCaseMapper);
    }

    @Test
    void joinRequiresAFullScheduleAndPersistsRoutinePriority() {
        when(scheduleMapper.selectById(101L)).thenReturn(schedule(101L, 10, 10));
        when(waitlistMapper.existsActive(7L, 101L)).thenReturn(false);
        when(waitlistMapper.insert(any(WaitlistEntry.class))).thenAnswer(invocation -> 1);

        WaitlistEntry result = service.join(7L, 101L, null);

        assertThat(result.getPatientId()).isEqualTo(7L);
        assertThat(result.getStatus()).isEqualTo(WaitlistStatus.WAITING);
        assertThat(result.getPriority()).isEqualTo(20);
        verify(waitlistMapper).insert(result);
    }

    @Test
    void joinRejectsScheduleWithAvailableCapacity() {
        when(scheduleMapper.selectById(101L)).thenReturn(schedule(101L, 10, 9));

        assertThatThrownBy(() -> service.join(7L, 101L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("仍有可用号源，请直接预约");
        verify(waitlistMapper, never()).insert(any(WaitlistEntry.class));
    }

    @Test
    void acceptingOfferCreatesPendingAppointmentAndConsumesCapacity() {
        WaitlistEntry entry = entry(55L, 101L, 7L);
        when(waitlistMapper.selectById(55L)).thenReturn(entry);
        when(scheduleMapper.selectById(101L)).thenReturn(schedule(101L, 10, 9));
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(scheduleMapper.decrementIfAvailable(101L)).thenReturn(1);
        when(appointmentMapper.insert(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment appointment = invocation.getArgument(0);
            appointment.setId(88L);
            return 1;
        });
        when(waitlistMapper.accept(55L, WaitlistStatus.ACCEPTED, 88L)).thenReturn(1);

        Appointment appointment = service.accept(7L, 55L);

        assertThat(appointment.getUserId()).isEqualTo(7L);
        assertThat(appointment.getScheduleId()).isEqualTo(101L);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        verify(scheduleMapper).decrementIfAvailable(101L);
        verify(waitlistMapper).accept(55L, WaitlistStatus.ACCEPTED, 88L);
    }

    @Test
    void acceptingExpiredOfferDoesNotConsumeCapacity() {
        WaitlistEntry entry = entry(55L, 101L, 7L);
        entry.setOfferExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(waitlistMapper.selectById(55L)).thenReturn(entry);

        assertThatThrownBy(() -> service.accept(7L, 55L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("候补 offer 已过期");
        verify(scheduleMapper, never()).decrementIfAvailable(anyLong());
    }

    @Test
    void offeringNextEntryMovesOnlyWaitingEntryToOffer() {
        WaitlistEntry entry = entry(55L, 101L, 7L);
        entry.setStatus(WaitlistStatus.WAITING);
        when(waitlistMapper.findNextWaiting(101L)).thenReturn(entry);
        when(waitlistMapper.offer(anyLong(), any(), any(), any())).thenReturn(1);

        assertThat(service.offerNext(101L)).isTrue();
        verify(waitlistMapper).offer(55L, WaitlistStatus.WAITING, WaitlistStatus.OFFERED,
                entry.getOfferExpiresAt());
    }

    private Schedule schedule(Long id, int total, int booked) {
        Schedule schedule = new Schedule();
        schedule.setId(id);
        schedule.setTotalSlots(total);
        schedule.setBookedSlots(booked);
        schedule.setDoctorId(42L);
        schedule.setDoctorName("张医生");
        schedule.setDepartment("神经内科");
        schedule.setDate("2026-09-08");
        schedule.setTime("上午");
        return schedule;
    }

    private WaitlistEntry entry(Long id, Long scheduleId, Long patientId) {
        WaitlistEntry entry = new WaitlistEntry();
        entry.setId(id);
        entry.setScheduleId(scheduleId);
        entry.setPatientId(patientId);
        entry.setStatus(WaitlistStatus.OFFERED);
        entry.setPriority(20);
        entry.setCreatedAt(LocalDateTime.now().minusMinutes(2));
        entry.setOfferExpiresAt(LocalDateTime.now().plusMinutes(10));
        return entry;
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("patient");
        user.setIdCard("11010519491231002X");
        return user;
    }
}
