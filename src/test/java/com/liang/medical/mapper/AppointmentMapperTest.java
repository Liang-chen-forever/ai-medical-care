package com.liang.medical.mapper;

import com.liang.medical.appointment.AppointmentStatus;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AppointmentMapperTest {

    @Test
    void doctorQueueUsesTheCurrentScheduleDoctor() throws Exception {
        Configuration configuration = new Configuration();
        try (Reader reader = Resources.getResourceAsReader("mapper/AppointmentMapper.xml")) {
            XMLMapperBuilder mapperBuilder = new XMLMapperBuilder(
                    reader, configuration, "mapper/AppointmentMapper.xml", configuration.getSqlFragments());
            mapperBuilder.parse();
        }

        BoundSql boundSql = configuration
                .getMappedStatement("com.liang.medical.mapper.AppointmentMapper.findForDoctor")
                .getBoundSql(Map.of("doctorUserId", 17L, "status", AppointmentStatus.PENDING));

        String sql = boundSql.getSql().replaceAll("\\s+", " ").trim();
        assertThat(sql).contains("INNER JOIN schedule s ON s.id = a.schedule_id")
                .contains("INNER JOIN doctor d ON d.id = s.doctor_id");
    }
}
