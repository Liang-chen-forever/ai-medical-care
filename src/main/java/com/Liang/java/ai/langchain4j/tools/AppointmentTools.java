package com.Liang.java.ai.langchain4j.tools;


import com.Liang.java.ai.langchain4j.entity.Appointment;
import com.Liang.java.ai.langchain4j.service.AppointmentService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppointmentTools {

    @Autowired
    private AppointmentService appointmentService;

    @Tool(name = "预约挂号", value = "根据参数,先执行工方法queryDepartment查询是否可预约，并直接给用户回答是否可预约，并让用户确认所有预约信息，用户确认后再进行预约.如果用户没有提供具体的医生和姓名，请从向量存储中找到一位医生。")
    public String bookAppointment(Appointment appointment) {

        //查找数据库中是否包含对应的预约信息
        Appointment appointmentDB = appointmentService.getOne(appointment);

        if(appointmentDB == null){
            appointment.setId(null);  //防止大模型幻觉设置了id导致插入失败
            if(appointmentService.save(appointment)){
                return "预约成功,并返回预约详情";
            }else{
                return "预约失败";
            }
        }

        return "您在相同的科室和时间已有预约,请重新选择";
    }

    @Tool(name = "取消预约", value = "根据参数,查询预约是否存在,如果存在则取消预约,否则提示用户没有预约")
    public String cancelAppointment(
            @P(value = "用户姓名") String username,
            @P(value = "身份证号") String idCard,
            @P(value = "科室名称") String department,
            @P(value = "日期") String date,
            @P(value = "时间 ,可选值：上午,下午") String time
    ) {
        Appointment appointment = new Appointment();
        appointment.setUsername(username);
        appointment.setIdCard(idCard);
        appointment.setDepartment(department);
        appointment.setDate(date);
        appointment.setTime(time);

        Appointment appointmentDB = appointmentService.getOne(appointment);
        if (appointmentDB != null) {
            if (appointmentService.removeById(appointmentDB.getId())) {
                return "取消预约成功";
            } else {
                return "取消预约失败";
            }
        }
        return "您没有预约,不能取消";
    }

    @Tool(name = "查询是否有号源",value = "根据科室名称，日期，时间和医生查询是否有号源,如果有则返回号源详情,否则提示用户没有号源")
    public boolean queryAppointment(
            @P(value = "科室名称") String name,
            @P(value = "日期") String date,
            @P(value = "时间 ,可选值：上午,下午") String time,
            @P(value = "医生名称",required = false) String doctorName
    ){
        System.out.println("查询是否有号源");
        System.out.println("科室名称: " + name);
        System.out.println("日期: " + date);
        System.out.println("时间: " + time);
        System.out.println("医生名称: " + doctorName);

        //TODO 维护医生的排班信息;
        //如果没有指定的医生名字，则根据其他条件查询是否有可以预约的医生（有返回true,无返回false）

        //如果制定了医生名字，则判断医生是否有排班（没有排班返回false）
        //如果有排班,则判断医生排班时间是否已约满（约满返回false,有空闲时间返回true）

        return true;
    }
}