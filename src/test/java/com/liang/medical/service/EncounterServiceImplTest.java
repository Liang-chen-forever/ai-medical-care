package com.liang.medical.service;

import com.liang.medical.appointment.AppointmentStatus;
import com.liang.medical.auth.UserRole;
import com.liang.medical.common.BusinessException;
import com.liang.medical.entity.Appointment;
import com.liang.medical.entity.Doctor;
import com.liang.medical.entity.Encounter;
import com.liang.medical.mapper.AppointmentMapper;
import com.liang.medical.mapper.DoctorMapper;
import com.liang.medical.mapper.EncounterMapper;
import com.liang.medical.service.impl.EncounterServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EncounterServiceImplTest {
    private AppointmentMapper appointmentMapper;
    private DoctorMapper doctorMapper;
    private EncounterMapper encounterMapper;
    private EncounterService service;

    @BeforeEach
    void setUp() {
        appointmentMapper = mock(AppointmentMapper.class);
        doctorMapper = mock(DoctorMapper.class);
        encounterMapper = mock(EncounterMapper.class);
        service = new EncounterServiceImpl(appointmentMapper, doctorMapper, encounterMapper);
    }

    @Test
    void doctorCompletesOwnConfirmedAppointmentAndPersistsSummary() {
        Appointment appointment = appointment(55L, 7L, 42L, AppointmentStatus.CONFIRMED);
        when(appointmentMapper.selectById(55L)).thenReturn(appointment);
        when(doctorMapper.findByUserId(17L)).thenReturn(doctor(42L, 17L));
        when(appointmentMapper.transitionStatus(55L, AppointmentStatus.CONFIRMED,
                AppointmentStatus.COMPLETED, 17L, null)).thenReturn(1);
        when(encounterMapper.insert(any(Encounter.class))).thenAnswer(invocation -> 1);

        Encounter encounter = service.complete(17L, 55L, "完成问诊摘要", "一周后复诊");

        assertThat(encounter.getAppointmentId()).isEqualTo(55L);
        assertThat(encounter.getDoctorId()).isEqualTo(42L);
        assertThat(encounter.getPatientId()).isEqualTo(7L);
        assertThat(encounter.getSummary()).isEqualTo("完成问诊摘要");
        verify(encounterMapper).insert(encounter);
    }

    @Test
    void doctorCannotCompleteAnotherDoctorsAppointment() {
        when(appointmentMapper.selectById(55L)).thenReturn(appointment(55L, 7L, 42L, AppointmentStatus.CONFIRMED));
        when(doctorMapper.findByUserId(17L)).thenReturn(doctor(41L, 17L));

        assertThatThrownBy(() -> service.complete(17L, 55L, "摘要", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权操作该预约");
        verify(appointmentMapper, never()).transitionStatus(any(), any(), any(), any(), any());
        verify(encounterMapper, never()).insert(any(Encounter.class));
    }

    @Test
    void completedAppointmentCannotCreateAnotherEncounter() {
        when(appointmentMapper.selectById(55L)).thenReturn(appointment(55L, 7L, 42L, AppointmentStatus.COMPLETED));
        when(doctorMapper.findByUserId(17L)).thenReturn(doctor(42L, 17L));

        assertThatThrownBy(() -> service.complete(17L, 55L, "摘要", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("预约状态已变更，请先确认预约状态");
    }

    @Test
    void patientCanReadOnlyOwnEncounter() {
        Encounter encounter = new Encounter();
        encounter.setAppointmentId(55L);
        encounter.setPatientId(7L);
        when(appointmentMapper.selectById(55L)).thenReturn(appointment(55L, 7L, 42L, AppointmentStatus.COMPLETED));
        when(encounterMapper.findByAppointmentId(55L)).thenReturn(encounter);

        assertThat(service.getForUser(7L, UserRole.PATIENT, 55L)).isSameAs(encounter);
        assertThatThrownBy(() -> service.getForUser(8L, UserRole.PATIENT, 55L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("无权访问该就诊摘要");
    }

    private Appointment appointment(Long id, Long patientId, Long doctorId, AppointmentStatus status) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setUserId(patientId);
        appointment.setDoctorId(doctorId);
        appointment.setScheduleId(101L);
        appointment.setStatus(status);
        return appointment;
    }

    private Doctor doctor(Long id, Long userId) {
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setUserId(userId);
        return doctor;
    }
}
