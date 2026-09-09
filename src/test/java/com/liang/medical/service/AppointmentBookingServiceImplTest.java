package com.liang.medical.service;

import com.liang.medical.common.BusinessException;
import com.liang.medical.appointment.AppointmentStatus;
import com.liang.medical.entity.Appointment;
import com.liang.medical.entity.Doctor;
import com.liang.medical.entity.Schedule;
import com.liang.medical.entity.TriageCase;
import com.liang.medical.entity.User;
import com.liang.medical.mapper.AppointmentMapper;
import com.liang.medical.mapper.DoctorMapper;
import com.liang.medical.mapper.ScheduleMapper;
import com.liang.medical.mapper.TriageCaseMapper;
import com.liang.medical.mapper.UserMapper;
import com.liang.medical.service.impl.AppointmentBookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

class AppointmentBookingServiceImplTest {

    private ScheduleMapper scheduleMapper;
    private AppointmentMapper appointmentMapper;
    private DoctorMapper doctorMapper;
    private TriageCaseMapper triageCaseMapper;
    private UserMapper userMapper;
    private AppointmentBookingService bookingService;

    @BeforeEach
    void setUp() {
        scheduleMapper = mock(ScheduleMapper.class);
        appointmentMapper = mock(AppointmentMapper.class);
        doctorMapper = mock(DoctorMapper.class);
        triageCaseMapper = mock(TriageCaseMapper.class);
        userMapper = mock(UserMapper.class);
        bookingService = new AppointmentBookingServiceImpl(scheduleMapper, appointmentMapper, doctorMapper, userMapper,
                null, null, triageCaseMapper);
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
        assertThat(appointment.getDoctorId()).isEqualTo(42L);
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        verify(appointmentMapper).insert(appointment);
    }

    @Test
    void bookingRejectsATriageCaseOwnedByAnotherPatientBeforeChangingCapacity() {
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(scheduleMapper.selectById(101L)).thenReturn(schedule(101L, 20, 3));
        TriageCase triageCase = new TriageCase();
        triageCase.setId(900L);
        triageCase.setPatientId(8L);
        when(triageCaseMapper.selectById(900L)).thenReturn(triageCase);

        assertThatThrownBy(() -> bookingService.book(7L, 101L, 900L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权使用该分诊记录");

        verify(scheduleMapper, never()).decrementIfAvailable(anyLong());
        verify(appointmentMapper, never()).insert(any(Appointment.class));
    }

    @Test
    void ownDoctorConfirmsPendingAppointmentWithoutChangingCapacity() {
        when(doctorMapper.findByUserId(17L)).thenReturn(doctor(42L, 17L));
        when(appointmentMapper.selectById(55L)).thenReturn(appointment(55L, 101L, AppointmentStatus.PENDING));
        when(scheduleMapper.selectById(101L)).thenReturn(scheduleForDoctor(101L, 42L));
        when(appointmentMapper.transitionStatus(55L, AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, 17L, null))
                .thenReturn(1);

        bookingService.confirmByDoctor(17L, 55L);

        verify(appointmentMapper).transitionStatus(55L, AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED, 17L, null);
        verify(scheduleMapper).selectById(101L);
        verify(scheduleMapper, never()).incrementIfBooked(anyLong());
        verifyNoMoreInteractions(scheduleMapper);
    }

    @Test
    void wrongDoctorCannotConfirmAnotherDoctorsAppointment() {
        when(doctorMapper.findByUserId(17L)).thenReturn(doctor(41L, 17L));
        when(appointmentMapper.selectById(55L)).thenReturn(appointment(55L, 101L, AppointmentStatus.PENDING));
        when(scheduleMapper.selectById(101L)).thenReturn(scheduleForDoctor(101L, 42L));

        assertThatThrownBy(() -> bookingService.confirmByDoctor(17L, 55L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权操作该预约");

        verify(appointmentMapper, never()).transitionStatus(anyLong(), any(), any(), anyLong(), any());
        verify(scheduleMapper, never()).incrementIfBooked(anyLong());
    }

    @Test
    void rejectedConfirmedAppointmentRestoresCapacityExactlyOnce() {
        when(doctorMapper.findByUserId(17L)).thenReturn(doctor(42L, 17L));
        when(appointmentMapper.selectById(55L)).thenReturn(appointment(55L, 101L, AppointmentStatus.CONFIRMED));
        when(scheduleMapper.selectById(101L)).thenReturn(scheduleForDoctor(101L, 42L));
        when(appointmentMapper.transitionStatus(55L, AppointmentStatus.CONFIRMED, AppointmentStatus.REJECTED, 17L, "医生拒绝"))
                .thenReturn(1);
        when(scheduleMapper.incrementIfBooked(101L)).thenReturn(1);

        bookingService.rejectByDoctor(17L, 55L, "医生拒绝");

        verify(scheduleMapper).incrementIfBooked(101L);
    }

    @Test
    void rejectedPendingAppointmentRestoresCapacity() {
        when(doctorMapper.findByUserId(17L)).thenReturn(doctor(42L, 17L));
        when(appointmentMapper.selectById(55L)).thenReturn(appointment(55L, 101L, AppointmentStatus.PENDING));
        when(scheduleMapper.selectById(101L)).thenReturn(scheduleForDoctor(101L, 42L));
        when(appointmentMapper.transitionStatus(55L, AppointmentStatus.PENDING, AppointmentStatus.REJECTED, 17L, "医生拒绝"))
                .thenReturn(1);
        when(scheduleMapper.incrementIfBooked(101L)).thenReturn(1);

        bookingService.rejectByDoctor(17L, 55L, "医生拒绝");

        verify(scheduleMapper).incrementIfBooked(101L);
    }

    @Test
    void rejectedAppointmentWithConcurrentStateChangeDoesNotRestoreCapacity() {
        when(doctorMapper.findByUserId(17L)).thenReturn(doctor(42L, 17L));
        when(appointmentMapper.selectById(55L)).thenReturn(appointment(55L, 101L, AppointmentStatus.CONFIRMED));
        when(scheduleMapper.selectById(101L)).thenReturn(scheduleForDoctor(101L, 42L));
        when(appointmentMapper.transitionStatus(55L, AppointmentStatus.CONFIRMED, AppointmentStatus.REJECTED, 17L, "医生拒绝"))
                .thenReturn(0);

        assertThatThrownBy(() -> bookingService.rejectByDoctor(17L, 55L, "医生拒绝"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("预约状态已变更，请刷新后重试");

        verify(scheduleMapper, never()).incrementIfBooked(anyLong());
    }

    @Test
    void patientCancelsPendingAppointmentAndRestoresCapacity() {
        when(appointmentMapper.selectById(55L)).thenReturn(appointment(55L, 101L, AppointmentStatus.PENDING, 7L));
        when(appointmentMapper.transitionStatus(55L, AppointmentStatus.PENDING, AppointmentStatus.CANCELLED, 7L, null))
                .thenReturn(1);
        when(scheduleMapper.incrementIfBooked(101L)).thenReturn(1);

        bookingService.cancel(7L, 55L);

        verify(scheduleMapper).incrementIfBooked(101L);
    }

    @Test
    void patientCancelsConfirmedAppointmentAndRestoresCapacity() {
        when(appointmentMapper.selectById(55L)).thenReturn(appointment(55L, 101L, AppointmentStatus.CONFIRMED, 7L));
        when(appointmentMapper.transitionStatus(55L, AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED, 7L, null))
                .thenReturn(1);
        when(scheduleMapper.incrementIfBooked(101L)).thenReturn(1);

        bookingService.cancel(7L, 55L);

        verify(scheduleMapper).incrementIfBooked(101L);
    }

    @Test
    void patientCancellationWithConcurrentStateChangeDoesNotRestoreCapacity() {
        when(appointmentMapper.selectById(55L)).thenReturn(appointment(55L, 101L, AppointmentStatus.PENDING, 7L));
        when(appointmentMapper.transitionStatus(55L, AppointmentStatus.PENDING, AppointmentStatus.CANCELLED, 7L, null))
                .thenReturn(0);

        assertThatThrownBy(() -> bookingService.cancel(7L, 55L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("预约状态已变更，请刷新后重试");

        verify(scheduleMapper, never()).incrementIfBooked(anyLong());
    }

    @Test
    void patientCannotCancelCompletedAppointment() {
        when(appointmentMapper.selectById(55L)).thenReturn(appointment(55L, 101L, AppointmentStatus.COMPLETED, 7L));

        assertThatThrownBy(() -> bookingService.cancel(7L, 55L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("预约状态已变更，请刷新后重试");

        verify(appointmentMapper, never()).transitionStatus(anyLong(), any(), any(), anyLong(), any());
        verify(scheduleMapper, never()).incrementIfBooked(anyLong());
    }

    @Test
    void ownDoctorCompletesConfirmedAppointmentWithoutRestoringCapacity() {
        when(doctorMapper.findByUserId(17L)).thenReturn(doctor(42L, 17L));
        when(appointmentMapper.selectById(55L)).thenReturn(appointment(55L, 101L, AppointmentStatus.CONFIRMED));
        when(scheduleMapper.selectById(101L)).thenReturn(scheduleForDoctor(101L, 42L));
        when(appointmentMapper.transitionStatus(55L, AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED, 17L, null))
                .thenReturn(1);

        bookingService.completeByDoctor(17L, 55L);

        verify(scheduleMapper, never()).incrementIfBooked(anyLong());
    }

    private Schedule schedule(Long id, int totalSlots, int bookedSlots) {
        Schedule schedule = new Schedule();
        schedule.setId(id);
        schedule.setDoctorId(42L);
        schedule.setTotalSlots(totalSlots);
        schedule.setBookedSlots(bookedSlots);
        schedule.setDoctorName("张医生");
        schedule.setDepartment("神经内科");
        schedule.setDate("2026-09-02");
        schedule.setTime("上午");
        return schedule;
    }

    private Appointment appointment(Long id, Long scheduleId, AppointmentStatus status) {
        return appointment(id, scheduleId, status, 7L);
    }

    private Appointment appointment(Long id, Long scheduleId, AppointmentStatus status, Long userId) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setUserId(userId);
        appointment.setScheduleId(scheduleId);
        appointment.setDoctorId(42L);
        appointment.setStatus(status);
        return appointment;
    }

    private Doctor doctor(Long id, Long userId) {
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setUserId(userId);
        return doctor;
    }

    private Schedule scheduleForDoctor(Long id, Long doctorId) {
        Schedule schedule = new Schedule();
        schedule.setId(id);
        schedule.setDoctorId(doctorId);
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
