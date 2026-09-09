package com.liang.medical.service.impl;

import com.liang.medical.appointment.AppointmentStatus;
import com.liang.medical.audit.AuditService;
import com.liang.medical.auth.UserRole;
import com.liang.medical.common.BusinessException;
import com.liang.medical.entity.Appointment;
import com.liang.medical.entity.Doctor;
import com.liang.medical.entity.Encounter;
import com.liang.medical.mapper.AppointmentMapper;
import com.liang.medical.mapper.DoctorMapper;
import com.liang.medical.mapper.EncounterMapper;
import com.liang.medical.service.EncounterService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EncounterServiceImpl implements EncounterService {
    private final AppointmentMapper appointmentMapper;
    private final DoctorMapper doctorMapper;
    private final EncounterMapper encounterMapper;
    private final AuditService auditService;

    public EncounterServiceImpl(AppointmentMapper appointmentMapper, DoctorMapper doctorMapper,
                                EncounterMapper encounterMapper) {
        this(appointmentMapper, doctorMapper, encounterMapper, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public EncounterServiceImpl(AppointmentMapper appointmentMapper, DoctorMapper doctorMapper,
                                EncounterMapper encounterMapper, AuditService auditService) {
        this.appointmentMapper = appointmentMapper;
        this.doctorMapper = doctorMapper;
        this.encounterMapper = encounterMapper;
        this.auditService = auditService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Encounter complete(Long doctorUserId, Long appointmentId, String summary, String followUpAdvice) {
        Appointment appointment = requireAppointment(appointmentId);
        Doctor doctor = requireDoctor(doctorUserId);
        requireDoctorOwnsAppointment(doctor, appointment);
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "预约状态已变更，请先确认预约状态");
        }
        if (encounterMapper.findByAppointmentId(appointmentId) != null) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "该预约已有就诊摘要");
        }
        if (summary == null || summary.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "就诊摘要不能为空");
        }

        if (appointmentMapper.transitionStatus(appointmentId, AppointmentStatus.CONFIRMED,
                AppointmentStatus.COMPLETED, doctorUserId, null) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "预约状态已变更，请刷新后重试");
        }

        Encounter encounter = new Encounter();
        encounter.setAppointmentId(appointmentId);
        encounter.setDoctorId(doctor.getId());
        encounter.setPatientId(appointment.getUserId());
        encounter.setSummary(summary.trim());
        encounter.setFollowUpAdvice(normalize(followUpAdvice));
        encounter.setCompletedAt(LocalDateTime.now());
        encounterMapper.insert(encounter);
        if (auditService != null) {
            auditService.record(new com.liang.medical.auth.UserPrincipal(doctorUserId, null,
                            com.liang.medical.auth.UserRole.DOCTOR),
                    "ENCOUNTER_COMPLETE", "ENCOUNTER", appointmentId.toString(), "SUCCESS", null);
        }
        return encounter;
    }

    @Override
    public Encounter getForUser(Long userId, UserRole role, Long appointmentId) {
        Appointment appointment = requireAppointment(appointmentId);
        if (role == UserRole.PATIENT) {
            if (!userId.equals(appointment.getUserId())) {
                throw new BusinessException(HttpStatus.FORBIDDEN, 403, "无权访问该就诊摘要");
            }
        } else if (role == UserRole.DOCTOR) {
            requireDoctorOwnsAppointment(requireDoctor(userId), appointment);
        } else {
            throw new BusinessException(HttpStatus.FORBIDDEN, 403, "无权访问该就诊摘要");
        }
        Encounter encounter = encounterMapper.findByAppointmentId(appointmentId);
        if (encounter == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "就诊摘要不存在");
        }
        return encounter;
    }

    private Appointment requireAppointment(Long appointmentId) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "预约不存在");
        }
        return appointment;
    }

    private Doctor requireDoctor(Long doctorUserId) {
        Doctor doctor = doctorMapper.findByUserId(doctorUserId);
        if (doctor == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, 403, "无权操作该预约");
        }
        return doctor;
    }

    private void requireDoctorOwnsAppointment(Doctor doctor, Appointment appointment) {
        if (!doctor.getId().equals(appointment.getDoctorId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, 403, "无权操作该预约");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
