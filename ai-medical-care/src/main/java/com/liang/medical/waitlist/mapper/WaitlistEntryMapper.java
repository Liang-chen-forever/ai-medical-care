package com.liang.medical.waitlist.mapper;

import com.liang.medical.waitlist.entity.WaitlistEntry;
import com.liang.medical.waitlist.entity.WaitlistStatus;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WaitlistEntryMapper extends BaseMapper<WaitlistEntry> {
    @Select("SELECT * FROM waitlist_entry WHERE schedule_id = #{scheduleId} "
            + "AND status = 'WAITING' ORDER BY priority ASC, created_at ASC, id ASC LIMIT 1")
    WaitlistEntry findNextWaiting(@Param("scheduleId") Long scheduleId);

    @Select("SELECT EXISTS(SELECT 1 FROM waitlist_entry WHERE patient_id = #{patientId} "
            + "AND schedule_id = #{scheduleId} AND status IN ('WAITING','OFFERED'))")
    boolean existsActive(@Param("patientId") Long patientId, @Param("scheduleId") Long scheduleId);

    @Update("UPDATE waitlist_entry SET status = #{to}, offer_expires_at = #{expiresAt}, "
            + "offered_at = CURRENT_TIMESTAMP WHERE id = #{id} AND status = #{from}")
    int offer(@Param("id") Long id, @Param("from") WaitlistStatus from,
              @Param("to") WaitlistStatus to, @Param("expiresAt") LocalDateTime expiresAt);

    @Update("UPDATE waitlist_entry SET status = #{to}, appointment_id = #{appointmentId}, "
            + "accepted_at = CURRENT_TIMESTAMP WHERE id = #{id} AND status = 'OFFERED' "
            + "AND offer_expires_at > CURRENT_TIMESTAMP")
    int accept(@Param("id") Long id, @Param("to") WaitlistStatus to, @Param("appointmentId") Long appointmentId);

    @Update("UPDATE waitlist_entry SET status = #{to} WHERE id = #{id} AND status = #{from}")
    int cancel(@Param("id") Long id, @Param("from") WaitlistStatus from, @Param("to") WaitlistStatus to);

    @Update("UPDATE waitlist_entry SET status = 'EXPIRED' WHERE id = #{id} AND status = 'OFFERED'")
    int expireOne(@Param("id") Long id);

    @Update("UPDATE waitlist_entry SET status = 'EXPIRED' WHERE status = 'OFFERED' "
            + "AND offer_expires_at <= CURRENT_TIMESTAMP")
    int expireOffers();

    @Select("SELECT * FROM waitlist_entry WHERE patient_id = #{patientId} ORDER BY created_at DESC, id DESC")
    List<WaitlistEntry> findMine(@Param("patientId") Long patientId);

    @Select("SELECT COUNT(*) FROM waitlist_entry WHERE status = #{status}")
    long countByStatus(@Param("status") WaitlistStatus status);
}
