package com.Liang.java.ai.langchain4j.entity;

import com.Liang.java.ai.langchain4j.appointment.AppointmentStatus;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Appointment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String idCard;

    private String department;

    private String date;

    private String time;

    private String doctorName;

    private Long userId;

    private Long scheduleId;

    private Long doctorId;

    private AppointmentStatus status;

    private String cancelReason;

    private Long handledBy;

    private LocalDateTime handledAt;
}
