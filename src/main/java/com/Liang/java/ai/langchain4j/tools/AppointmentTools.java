package com.Liang.java.ai.langchain4j.tools;

import com.Liang.java.ai.langchain4j.entity.Appointment;
import com.Liang.java.ai.langchain4j.entity.Doctor;
import com.Liang.java.ai.langchain4j.entity.Schedule;
import com.Liang.java.ai.langchain4j.service.AppointmentService;
import com.Liang.java.ai.langchain4j.service.DoctorService;
import com.Liang.java.ai.langchain4j.service.ScheduleService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AppointmentTools {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private ScheduleService scheduleService;

    @Tool(name = "预约挂号", value = "根据参数,先调用queryAppointment方法查询是否可预约，并直接给用户回答是否可预约，并让用户确认所有预约信息，用户确认后再进行预约.如果用户没有提供具体的医生和姓名，请从向量存储中找到一位医生。注意：同一科室7天内仅允许预约1次。")
    public String bookAppointment(Appointment appointment) {
        // 精确重复检查
        Appointment appointmentDB = appointmentService.getOne(appointment);
        if (appointmentDB != null) {
            return "您已在 " + appointment.getDepartment() + " " +
                    appointment.getDate() + " " + appointment.getTime() + " 有预约，请勿重复预约";
        }

        // 7天内科室冲突检查
        List<Appointment> conflicts = appointmentService.checkDepartmentConflict(
                appointment.getIdCard(), appointment.getDepartment());
        if (!conflicts.isEmpty()) {
            Appointment conflict = conflicts.get(0);
            return "您已在 " + conflict.getDate() + " 预约过【" + appointment.getDepartment() +
                    "】科室，同一科室7天内仅允许预约1次，请选择其他科室或等待"
                    + conflict.getDate() + " 7天后重新预约";
        }

        appointment.setId(null);
        if (appointmentService.save(appointment)) {
            return "预约成功,并返回预约详情";
        } else {
            return "预约失败";
        }
    }

    @Tool(name = "取消预约", value = "根据用户姓名+身份证号+科室名称查找并取消预约。日期和时间不需要精确提供，系统会自动匹配用户在指定科室的最近预约。")
    public String cancelAppointment(
            @P(value = "用户姓名") String username,
            @P(value = "身份证号") String idCard,
            @P(value = "科室名称") String department
    ) {
        // 按用户+科室查找预约（不依赖AI记忆的日期时间，自动匹配最近一条）
        Appointment appointmentDB = appointmentService.findByUserAndDepartment(username, idCard, department);
        if (appointmentDB != null) {
            if (appointmentService.removeById(appointmentDB.getId())) {
                return "已成功取消您在 " + appointmentDB.getDepartment() + " " +
                        appointmentDB.getDate() + " " + appointmentDB.getTime() +
                        " 的预约（医生：" + appointmentDB.getDoctorName() + "）";
            } else {
                return "取消预约失败，请稍后重试";
            }
        }
        return "您没有【" + department + "】科室的预约记录，请确认科室名称是否正确。如需查看所有预约，请告知我帮您查询。";
    }

    @Tool(name = "查询是否有号源", value = "根据科室名称，日期，时间和医生查询是否有号源,如果有则返回号源详情,否则提示用户没有号源")
    public String queryAppointment(
            @P(value = "科室名称") String department,
            @P(value = "日期") String date,
            @P(value = "时间 ,可选值：上午,下午") String time,
            @P(value = "医生名称", required = false) String doctorName
    ) {
        if (doctorName != null && !doctorName.isEmpty()) {
            List<Doctor> doctors = doctorService.getByDepartment(department);
            Doctor targetDoctor = doctors.stream()
                    .filter(d -> d.getName().equals(doctorName))
                    .findFirst()
                    .orElse(null);
            if (targetDoctor == null) {
                return "未找到该科室的医生: " + doctorName;
            }
            Schedule schedule = scheduleService.getByDoctorAndDate(targetDoctor.getId(), date, time);
            if (schedule == null) {
                return "医生 " + doctorName + " 在 " + date + " " + time + " 没有排班";
            }
            int available = schedule.getTotalSlots() - (schedule.getBookedSlots() != null ? schedule.getBookedSlots() : 0);
            if (available > 0) {
                return "医生 " + doctorName + " (" + targetDoctor.getTitle() + ") 在 " + date + " " + time + " 有号源，剩余 " + available + " 个号";
            } else {
                return "医生 " + doctorName + " 在 " + date + " " + time + " 的号源已约满";
            }
        }

        List<Schedule> schedules = scheduleService.getAvailableSlots(department, date, time);
        if (schedules.isEmpty()) {
            return "科室 " + department + " 在 " + date + " " + time + " 没有可预约的号源";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("科室 ").append(department).append(" 在 ").append(date).append(" ").append(time).append(" 有以下可预约医生:\n");
        for (Schedule s : schedules) {
            int available = s.getTotalSlots() - (s.getBookedSlots() != null ? s.getBookedSlots() : 0);
            sb.append("- ").append(s.getDoctorName()).append("，剩余 ").append(available).append(" 个号\n");
        }
        return sb.toString();
    }
}