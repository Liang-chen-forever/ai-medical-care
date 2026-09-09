package com.liang.medical.tools;

import com.liang.medical.entity.Doctor;
import com.liang.medical.entity.Schedule;
import com.liang.medical.service.DoctorService;
import com.liang.medical.service.ScheduleService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AppointmentTools {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private ScheduleService scheduleService;

    @Tool(name = "预约挂号", value = "当用户想预约时，引导其在已登录的小程序预约页选择具体排班。不得通过对话收集或写入身份证、姓名等个人信息。")
    public String bookAppointment() {
        return "为保护您的个人信息并保证号源一致性，请先登录小程序，在“预约挂号”页选择具体医生和时段后提交预约。";
    }

    @Tool(name = "取消预约", value = "当用户想取消预约时，引导其在已登录的小程序“我的预约”页取消。不得通过对话收集身份证或执行取消操作。")
    public String cancelAppointment() {
        return "请登录小程序并进入“我的预约”，选择对应预约后取消。系统会校验预约归属并自动回补号源。";
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
