package com.Liang.java.ai.langchain4j.controller;

import com.Liang.java.ai.langchain4j.entity.Doctor;
import com.Liang.java.ai.langchain4j.entity.Schedule;
import com.Liang.java.ai.langchain4j.auth.JwtTokenService;
import com.Liang.java.ai.langchain4j.service.DoctorService;
import com.Liang.java.ai.langchain4j.service.ScheduleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DepartmentController.class)
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DoctorService doctorService;

    @MockBean
    private ScheduleService scheduleService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @Test
    void listsDistinctDepartmentsInDoctorInsertionOrder() throws Exception {
        Doctor neurologyDoctor = new Doctor();
        neurologyDoctor.setDepartment("神经内科");
        Doctor dentistryDoctor = new Doctor();
        dentistryDoctor.setDepartment("口腔科");
        Doctor secondNeurologyDoctor = new Doctor();
        secondNeurologyDoctor.setDepartment("神经内科");
        when(doctorService.list()).thenReturn(List.of(neurologyDoctor, dentistryDoctor, secondNeurologyDoctor));

        mockMvc.perform(get("/api/v1/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0]").value("神经内科"))
                .andExpect(jsonPath("$.data[1]").value("口腔科"))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void delegatesDoctorsByDepartmentPathVariable() throws Exception {
        when(doctorService.getByDepartment("神经内科")).thenReturn(List.of(new Doctor()));

        mockMvc.perform(get("/api/v1/departments/神经内科/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(doctorService).getByDepartment("神经内科");
    }

    @Test
    void mapsPeriodQueryParameterToExistingScheduleTimeArgument() throws Exception {
        when(scheduleService.getAvailableSlots("神经内科", "2026-09-02", "上午"))
                .thenReturn(List.of(new Schedule()));

        mockMvc.perform(get("/api/v1/schedules")
                        .param("department", "神经内科")
                        .param("date", "2026-09-02")
                        .param("period", "上午"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(scheduleService).getAvailableSlots("神经内科", "2026-09-02", "上午");
    }
}
