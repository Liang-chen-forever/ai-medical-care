package com.liang.medical;

import com.liang.medical.entity.Appointment;
import com.liang.medical.service.AppointmentService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Tag("external")
public class AppointmentTest {

    @Autowired
    private AppointmentService appointmentService;

    @Test
    void testGetOne() {
        Appointment appointment = new Appointment();
        appointment.setUsername("张三");
        appointment.setIdCard("123456789012345678");
        appointment.setDepartment("内科");
        appointment.setDate("2025-04-14");
        appointment.setTime("上午");
        //appointment.setDoctorName("张医生");

        Appointment result = appointmentService.getOne(appointment);

        assertNotNull(result, "查询结果不应该为空");
        System.out.println(result);
    }

    @Test
    void testSave(){
        Appointment appointment = new Appointment();
        appointment.setUsername("张三");
        appointment.setIdCard("123456789012345678");
        appointment.setDepartment("内科");
        appointment.setDate("2025-04-14");
        appointment.setTime("上午");
        appointment.setDoctorName("张医生");
        appointmentService.save(appointment);
    }

    @Test
    void testRemoveById(){
        appointmentService.removeById(1L);
    }
}
