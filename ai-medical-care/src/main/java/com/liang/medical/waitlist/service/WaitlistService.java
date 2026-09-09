package com.liang.medical.waitlist.service;

import com.liang.medical.appointment.entity.Appointment;
import com.liang.medical.waitlist.entity.WaitlistEntry;

import java.util.List;

public interface WaitlistService {
    WaitlistEntry join(Long patientId, Long scheduleId, Long triageCaseId);

    List<WaitlistEntry> listMine(Long patientId);

    void cancel(Long patientId, Long entryId);

    Appointment accept(Long patientId, Long entryId);

    boolean offerNext(Long scheduleId);

    int expireOffers();
}
