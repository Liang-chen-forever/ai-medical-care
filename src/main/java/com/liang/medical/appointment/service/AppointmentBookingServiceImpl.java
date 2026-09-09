package com.liang.medical.appointment.service;

import com.liang.medical.appointment.entity.AppointmentStatus;
import com.liang.medical.audit.AuditService;
import com.liang.medical.auth.UserPrincipal;
import com.liang.medical.auth.UserRole;
import com.liang.medical.common.BusinessException;
import com.liang.medical.appointment.entity.Appointment;
import com.liang.medical.appointment.entity.Doctor;
import com.liang.medical.appointment.entity.Schedule;
import com.liang.medical.entity.User;
import com.liang.medical.appointment.dto.DoctorAppointmentResponse;
import com.liang.medical.appointment.mapper.AppointmentMapper;
import com.liang.medical.appointment.mapper.DoctorMapper;
import com.liang.medical.appointment.mapper.ScheduleMapper;
import com.liang.medical.mapper.TriageCaseMapper;
import com.liang.medical.mapper.UserMapper;
import com.liang.medical.appointment.service.AppointmentBookingService;
import com.liang.medical.service.WaitlistService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppointmentBookingServiceImpl implements AppointmentBookingService {

    private final ScheduleMapper scheduleMapper;
    private final AppointmentMapper appointmentMapper;
    private final DoctorMapper doctorMapper;
    private final UserMapper userMapper;
    private final WaitlistService waitlistService;
    private final AuditService auditService;
    private final TriageCaseMapper triageCaseMapper;

    public AppointmentBookingServiceImpl(ScheduleMapper scheduleMapper,
                                         AppointmentMapper appointmentMapper,
                                         DoctorMapper doctorMapper,
                                         UserMapper userMapper) {
        this(scheduleMapper, appointmentMapper, doctorMapper, userMapper, null, null);
    }

    public AppointmentBookingServiceImpl(ScheduleMapper scheduleMapper,
                                         AppointmentMapper appointmentMapper,
                                         DoctorMapper doctorMapper,
                                         UserMapper userMapper,
                                         WaitlistService waitlistService) {
        this(scheduleMapper, appointmentMapper, doctorMapper, userMapper, waitlistService, null);
    }

    public AppointmentBookingServiceImpl(ScheduleMapper scheduleMapper,
                                         AppointmentMapper appointmentMapper,
                                         DoctorMapper doctorMapper,
                                         UserMapper userMapper,
                                         WaitlistService waitlistService,
                                         AuditService auditService) {
        this(scheduleMapper, appointmentMapper, doctorMapper, userMapper, waitlistService, auditService, null);
    }

    @Autowired
    public AppointmentBookingServiceImpl(ScheduleMapper scheduleMapper,
                                         AppointmentMapper appointmentMapper,
                                         DoctorMapper doctorMapper,
                                         UserMapper userMapper,
                                         WaitlistService waitlistService,
                                         AuditService auditService,
                                         TriageCaseMapper triageCaseMapper) {
        this.scheduleMapper = scheduleMapper;
        this.appointmentMapper = appointmentMapper;
        this.doctorMapper = doctorMapper;
        this.userMapper = userMapper;
        this.waitlistService = waitlistService;
        this.auditService = auditService;
        this.triageCaseMapper = triageCaseMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Appointment book(Long userId, Long scheduleId) {
        return book(userId, scheduleId, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Appointment book(Long userId, Long scheduleId, Long triageCaseId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, 401, "登录状态无效或已过期");
        }
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "排班不存在");
        }
        validateTriageCaseOwnership(userId, triageCaseId);
        if (appointmentMapper.existsByUserIdAndScheduleId(userId, scheduleId)) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "请勿重复预约");
        }
        if (scheduleMapper.decrementIfAvailable(scheduleId) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "该时段号源已约满");
        }

        Appointment appointment = new Appointment();
        appointment.setUserId(userId);
        appointment.setScheduleId(scheduleId);
        appointment.setTriageCaseId(triageCaseId);
        appointment.setDoctorId(schedule.getDoctorId());
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setUsername(user.getUsername());
        appointment.setIdCard(user.getIdCard());
        appointment.setDoctorName(schedule.getDoctorName());
        appointment.setDepartment(schedule.getDepartment());
        appointment.setDate(schedule.getDate());
        appointment.setTime(schedule.getTime());
        appointmentMapper.insert(appointment);
        audit(userId, UserRole.PATIENT, "APPOINTMENT_BOOK", "APPOINTMENT", appointment.getId(), "SUCCESS", null);
        return appointment;
    }

    private void validateTriageCaseOwnership(Long userId, Long triageCaseId) {
        if (triageCaseId == null) {
            return;
        }
        if (triageCaseMapper == null) {
            throw new IllegalStateException("分诊记录校验未配置");
        }
        com.liang.medical.entity.TriageCase triageCase = triageCaseMapper.selectById(triageCaseId);
        if (triageCase == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "分诊记录不存在");
        }
        if (!userId.equals(triageCase.getPatientId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, 403, "无权使用该分诊记录");
        }
    }

    @Override
    public List<Appointment> listMine(Long userId) {
        return appointmentMapper.selectList(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getUserId, userId)
                .orderByDesc(Appointment::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long appointmentId) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "预约不存在");
        }
        if (!userId.equals(appointment.getUserId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, 403, "无权操作该预约");
        }
        if (!isCancellable(appointment.getStatus())) {
            throw stateChanged();
        }
        if (appointmentMapper.transitionStatus(appointmentId, appointment.getStatus(), AppointmentStatus.CANCELLED,
                userId, null) != 1) {
            throw stateChanged();
        }
        if (scheduleMapper.incrementIfBooked(appointment.getScheduleId()) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "预约状态异常，请稍后重试");
        }
        if (waitlistService != null) {
            waitlistService.offerNext(appointment.getScheduleId());
        }
        audit(userId, UserRole.PATIENT, "APPOINTMENT_CANCEL", "APPOINTMENT", appointmentId, "SUCCESS", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmByDoctor(Long doctorUserId, Long appointmentId) {
        Appointment appointment = findOwnedAppointment(doctorUserId, appointmentId);
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw stateChanged();
        }
        if (appointmentMapper.transitionStatus(appointmentId, AppointmentStatus.PENDING,
                AppointmentStatus.CONFIRMED, doctorUserId, null) != 1) {
            throw stateChanged();
        }
        audit(doctorUserId, UserRole.DOCTOR, "APPOINTMENT_CONFIRM", "APPOINTMENT", appointmentId, "SUCCESS", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectByDoctor(Long doctorUserId, Long appointmentId, String reason) {
        Appointment appointment = findOwnedAppointment(doctorUserId, appointmentId);
        if (!isRejectable(appointment.getStatus())) {
            throw stateChanged();
        }
        if (appointmentMapper.transitionStatus(appointmentId, appointment.getStatus(), AppointmentStatus.REJECTED,
                doctorUserId, reason) != 1) {
            throw stateChanged();
        }
        restoreCapacity(appointment.getScheduleId());
        if (waitlistService != null) {
            waitlistService.offerNext(appointment.getScheduleId());
        }
        audit(doctorUserId, UserRole.DOCTOR, "APPOINTMENT_REJECT", "APPOINTMENT", appointmentId, "SUCCESS", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeByDoctor(Long doctorUserId, Long appointmentId) {
        Appointment appointment = findOwnedAppointment(doctorUserId, appointmentId);
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw stateChanged();
        }
        if (appointmentMapper.transitionStatus(appointmentId, AppointmentStatus.CONFIRMED,
                AppointmentStatus.COMPLETED, doctorUserId, null) != 1) {
            throw stateChanged();
        }
        audit(doctorUserId, UserRole.DOCTOR, "APPOINTMENT_COMPLETE", "APPOINTMENT", appointmentId, "SUCCESS", null);
    }

    @Override
    public List<DoctorAppointmentResponse> listForDoctor(Long doctorUserId, AppointmentStatus status) {
        return appointmentMapper.findForDoctor(doctorUserId, status).stream()
                .map(a -> new DoctorAppointmentResponse(a.getId(), a.getScheduleId(), a.getDepartment(),
                        a.getDate(), a.getTime(), a.getDoctorName(), a.getStatus(), a.getCancelReason()))
                .toList();
    }

    private Appointment findOwnedAppointment(Long doctorUserId, Long appointmentId) {
        Doctor doctor = doctorMapper.findByUserId(doctorUserId);
        if (doctor == null) {
            throw new BusinessException(HttpStatus.FORBIDDEN, 403, "无权操作该预约");
        }
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "预约不存在");
        }
        Schedule schedule = scheduleMapper.selectById(appointment.getScheduleId());
        if (schedule == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "排班不存在");
        }
        if (!doctor.getId().equals(schedule.getDoctorId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, 403, "无权操作该预约");
        }
        return appointment;
    }

    private boolean isCancellable(AppointmentStatus status) {
        return status == AppointmentStatus.PENDING || status == AppointmentStatus.CONFIRMED;
    }

    private boolean isRejectable(AppointmentStatus status) {
        return status == AppointmentStatus.PENDING || status == AppointmentStatus.CONFIRMED;
    }

    private void restoreCapacity(Long scheduleId) {
        if (scheduleMapper.incrementIfBooked(scheduleId) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "预约状态异常，请稍后重试");
        }
    }

    private BusinessException stateChanged() {
        return new BusinessException(HttpStatus.CONFLICT, 409, "预约状态已变更，请刷新后重试");
    }

    private void audit(Long actorId, UserRole role, String action, String targetType, Long targetId,
                       String result, String detail) {
        if (auditService != null) {
            auditService.record(new UserPrincipal(actorId, null, role), action, targetType,
                    targetId == null ? null : targetId.toString(), result, detail);
        }
    }
}
