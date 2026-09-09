package com.liang.medical.service.impl;

import com.liang.medical.appointment.entity.AppointmentStatus;
import com.liang.medical.audit.AuditService;
import com.liang.medical.auth.UserPrincipal;
import com.liang.medical.auth.UserRole;
import com.liang.medical.common.BusinessException;
import com.liang.medical.appointment.entity.Appointment;
import com.liang.medical.appointment.entity.Schedule;
import com.liang.medical.entity.TriageCase;
import com.liang.medical.entity.User;
import com.liang.medical.entity.WaitlistEntry;
import com.liang.medical.appointment.mapper.AppointmentMapper;
import com.liang.medical.appointment.mapper.ScheduleMapper;
import com.liang.medical.mapper.TriageCaseMapper;
import com.liang.medical.mapper.UserMapper;
import com.liang.medical.mapper.WaitlistEntryMapper;
import com.liang.medical.service.WaitlistService;
import com.liang.medical.waitlist.WaitlistStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WaitlistServiceImpl implements WaitlistService {
    private static final int OFFER_MINUTES = 15;

    private final ScheduleMapper scheduleMapper;
    private final WaitlistEntryMapper waitlistMapper;
    private final AppointmentMapper appointmentMapper;
    private final UserMapper userMapper;
    private final TriageCaseMapper triageCaseMapper;
    private final AuditService auditService;

    public WaitlistServiceImpl(ScheduleMapper scheduleMapper, WaitlistEntryMapper waitlistMapper,
                               AppointmentMapper appointmentMapper, UserMapper userMapper,
                               TriageCaseMapper triageCaseMapper) {
        this(scheduleMapper, waitlistMapper, appointmentMapper, userMapper, triageCaseMapper, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public WaitlistServiceImpl(ScheduleMapper scheduleMapper, WaitlistEntryMapper waitlistMapper,
                               AppointmentMapper appointmentMapper, UserMapper userMapper,
                               TriageCaseMapper triageCaseMapper, AuditService auditService) {
        this.scheduleMapper = scheduleMapper;
        this.waitlistMapper = waitlistMapper;
        this.appointmentMapper = appointmentMapper;
        this.userMapper = userMapper;
        this.triageCaseMapper = triageCaseMapper;
        this.auditService = auditService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WaitlistEntry join(Long patientId, Long scheduleId, Long triageCaseId) {
        Schedule schedule = requireSchedule(scheduleId);
        if (hasCapacity(schedule)) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "仍有可用号源，请直接预约");
        }
        if (waitlistMapper.existsActive(patientId, scheduleId)) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "该排班已有候补记录");
        }

        int priority = 20;
        if (triageCaseId != null) {
            TriageCase triageCase = triageCaseMapper.selectById(triageCaseId);
            if (triageCase == null) {
                throw new BusinessException(HttpStatus.NOT_FOUND, 404, "分诊记录不存在");
            }
            if (!patientId.equals(triageCase.getPatientId())) {
                throw new BusinessException(HttpStatus.FORBIDDEN, 403, "无权使用该分诊记录");
            }
            priority = WaitlistEntry.priorityFor(triageCase.getRiskLevel());
        }

        WaitlistEntry entry = new WaitlistEntry();
        entry.setScheduleId(scheduleId);
        entry.setPatientId(patientId);
        entry.setTriageCaseId(triageCaseId);
        entry.setPriority(priority);
        entry.setStatus(WaitlistStatus.WAITING);
        entry.setCreatedAt(LocalDateTime.now());
        waitlistMapper.insert(entry);
        audit(patientId, UserRole.PATIENT, "WAITLIST_JOIN", "WAITLIST", entry.getId(), "SUCCESS", null);
        return entry;
    }

    @Override
    public List<WaitlistEntry> listMine(Long patientId) {
        return waitlistMapper.findMine(patientId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long patientId, Long entryId) {
        WaitlistEntry entry = requireEntry(entryId);
        requireOwner(patientId, entry);
        if (entry.getStatus() != WaitlistStatus.WAITING && entry.getStatus() != WaitlistStatus.OFFERED) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "候补状态已结束");
        }
        if (waitlistMapper.cancel(entryId, entry.getStatus(), WaitlistStatus.CANCELLED) != 1) {
            throw stateChanged();
        }
        audit(patientId, UserRole.PATIENT, "WAITLIST_CANCEL", "WAITLIST", entryId, "SUCCESS", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Appointment accept(Long patientId, Long entryId) {
        WaitlistEntry entry = requireEntry(entryId);
        requireOwner(patientId, entry);
        if (entry.getStatus() != WaitlistStatus.OFFERED) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "候补尚未获得 offer");
        }
        if (entry.getOfferExpiresAt() == null || !entry.getOfferExpiresAt().isAfter(LocalDateTime.now())) {
            waitlistMapper.expireOne(entryId);
            throw new BusinessException(HttpStatus.CONFLICT, 409, "候补 offer 已过期");
        }

        Schedule schedule = requireSchedule(entry.getScheduleId());
        User user = userMapper.selectById(patientId);
        if (user == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, 401, "登录状态无效或已过期");
        }
        if (scheduleMapper.decrementIfAvailable(schedule.getId()) != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "该时段号源已被其他人占用");
        }

        Appointment appointment = new Appointment();
        appointment.setUserId(patientId);
        appointment.setScheduleId(schedule.getId());
        appointment.setDoctorId(schedule.getDoctorId());
        appointment.setTriageCaseId(entry.getTriageCaseId());
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setUsername(user.getUsername());
        appointment.setIdCard(user.getIdCard());
        appointment.setDoctorName(schedule.getDoctorName());
        appointment.setDepartment(schedule.getDepartment());
        appointment.setDate(schedule.getDate());
        appointment.setTime(schedule.getTime());
        appointmentMapper.insert(appointment);

        if (appointment.getId() == null
                || waitlistMapper.accept(entryId, WaitlistStatus.ACCEPTED, appointment.getId()) != 1) {
            throw stateChanged();
        }
        audit(patientId, UserRole.PATIENT, "WAITLIST_ACCEPT", "WAITLIST", entryId, "SUCCESS", null);
        return appointment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean offerNext(Long scheduleId) {
        WaitlistEntry entry = waitlistMapper.findNextWaiting(scheduleId);
        if (entry == null) {
            return false;
        }
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OFFER_MINUTES);
        entry.setOfferExpiresAt(expiresAt);
        entry.setOfferedAt(LocalDateTime.now());
        return waitlistMapper.offer(entry.getId(), WaitlistStatus.WAITING, WaitlistStatus.OFFERED, expiresAt) == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int expireOffers() {
        return waitlistMapper.expireOffers();
    }

    private Schedule requireSchedule(Long scheduleId) {
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "排班不存在");
        }
        return schedule;
    }

    private WaitlistEntry requireEntry(Long entryId) {
        WaitlistEntry entry = waitlistMapper.selectById(entryId);
        if (entry == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, 404, "候补记录不存在");
        }
        return entry;
    }

    private void requireOwner(Long patientId, WaitlistEntry entry) {
        if (!patientId.equals(entry.getPatientId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, 403, "无权操作该候补记录");
        }
    }

    private boolean hasCapacity(Schedule schedule) {
        return schedule.getBookedSlots() != null && schedule.getTotalSlots() != null
                && schedule.getBookedSlots() < schedule.getTotalSlots();
    }

    private BusinessException stateChanged() {
        return new BusinessException(HttpStatus.CONFLICT, 409, "候补状态已变更，请刷新后重试");
    }

    private void audit(Long actorId, UserRole role, String action, String targetType, Long targetId,
                       String result, String detail) {
        if (auditService != null) {
            auditService.record(new UserPrincipal(actorId, null, role), action, targetType,
                    targetId == null ? null : targetId.toString(), result, detail);
        }
    }
}
