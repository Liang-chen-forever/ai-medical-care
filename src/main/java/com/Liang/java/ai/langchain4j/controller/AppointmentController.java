package com.Liang.java.ai.langchain4j.controller;

import com.Liang.java.ai.langchain4j.bean.Result;
import com.Liang.java.ai.langchain4j.entity.Appointment;
import com.Liang.java.ai.langchain4j.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "预约管理")
@RestController
@RequestMapping("/api/appointment")
public class AppointmentController {

    private static final Logger log = LoggerFactory.getLogger(AppointmentController.class);

    @Autowired
    private AppointmentService appointmentService;

    @Operation(summary = "查询用户的所有预约")
    @GetMapping("/list")
    public Result<List<Appointment>> list(@RequestParam String idCard) {
        List<Appointment> list = appointmentService.lambdaQuery()
                .eq(Appointment::getIdCard, idCard)
                .orderByDesc(Appointment::getDate)
                .list();
        return Result.success(list);
    }

    @Operation(summary = "直接预约挂号")
    @PostMapping("/book")
    public Result<String> book(@RequestBody Appointment appointment) {
        // 1. 精确重复检查：同一人+同一科室+同一日期+同一时间段
        Appointment existing = appointmentService.getOne(appointment);
        if (existing != null) {
            log.warn("[预约冲突-精确] 用户:{}, 科室:{}, 日期:{}, 时间段:{} — 已存在完全相同预约(id={})",
                    appointment.getUsername(), appointment.getDepartment(),
                    appointment.getDate(), appointment.getTime(), existing.getId());
            return Result.error("您已在 " + appointment.getDepartment() + " " +
                    appointment.getDate() + " " + appointment.getTime() + " 有预约，请勿重复预约");
        }

        // 2. 7天内科室冲突检查：同一人+同一科室+7天内
        List<Appointment> conflicts = appointmentService.checkDepartmentConflict(
                appointment.getIdCard(), appointment.getDepartment());
        if (!conflicts.isEmpty()) {
            Appointment conflict = conflicts.get(0);
            log.warn("[预约冲突-7天限制] 用户:{}, 科室:{}, 尝试日期:{}, 已有预约日期:{}",
                    appointment.getUsername(), appointment.getDepartment(),
                    appointment.getDate(), conflict.getDate());
            return Result.error("您已在 " + conflict.getDate() + " 预约过【" +
                    appointment.getDepartment() + "】科室，" +
                    "同一科室7天内仅允许预约1次，请选择其他科室或等待"
                    + conflict.getDate() + " 7天后重新预约");
        }

        appointment.setId(null);
        if (appointmentService.save(appointment)) {
            log.info("[预约成功] 用户:{}, 科室:{}, 医生:{}, 日期:{}, 时间段:{}",
                    appointment.getUsername(), appointment.getDepartment(),
                    appointment.getDoctorName(), appointment.getDate(), appointment.getTime());
            return Result.success("预约成功");
        }
        return Result.error("预约失败");
    }

    @Operation(summary = "取消预约")
    @DeleteMapping("/cancel/{id}")
    public Result<String> cancel(@PathVariable Long id) {
        if (appointmentService.removeById(id)) {
            log.info("[取消预约] 预约ID:{} 已取消", id);
            return Result.success("取消预约成功");
        }
        return Result.error("取消预约失败");
    }
}