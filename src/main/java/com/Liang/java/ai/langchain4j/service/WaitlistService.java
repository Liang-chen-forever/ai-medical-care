package com.Liang.java.ai.langchain4j.service;

import com.Liang.java.ai.langchain4j.entity.Appointment;
import com.Liang.java.ai.langchain4j.entity.WaitlistEntry;

import java.util.List;

public interface WaitlistService {
    WaitlistEntry join(Long patientId, Long scheduleId, Long triageCaseId);

    List<WaitlistEntry> listMine(Long patientId);

    void cancel(Long patientId, Long entryId);

    Appointment accept(Long patientId, Long entryId);

    boolean offerNext(Long scheduleId);

    int expireOffers();
}
