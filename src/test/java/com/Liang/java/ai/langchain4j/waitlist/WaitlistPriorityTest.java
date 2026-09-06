package com.Liang.java.ai.langchain4j.waitlist;

import com.Liang.java.ai.langchain4j.triage.TriageRiskLevel;
import com.Liang.java.ai.langchain4j.entity.WaitlistEntry;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WaitlistPriorityTest {

    @Test
    void emergencyEntriesPrecedeRoutineAndUnknownEntries() {
        assertThat(WaitlistEntry.priorityFor(TriageRiskLevel.EMERGENCY)).isLessThan(
                WaitlistEntry.priorityFor(TriageRiskLevel.ROUTINE));
        assertThat(WaitlistEntry.priorityFor(TriageRiskLevel.ROUTINE)).isLessThan(
                WaitlistEntry.priorityFor(TriageRiskLevel.UNKNOWN));
    }

    @Test
    void samePriorityUsesCreatedAtThenIdAsFifoTieBreakers() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 9, 5, 12, 0);
        WaitlistEntry newerId = entry(2L, createdAt);
        WaitlistEntry olderId = entry(1L, createdAt);

        assertThat(List.of(newerId, olderId).stream()
                .sorted(WaitlistEntry.PRIORITY_ORDER)
                .toList())
                .containsExactly(olderId, newerId);
    }

    @Test
    void legalOfferLifecycleTransitionsAreExplicit() {
        assertThat(WaitlistStatus.canTransition(WaitlistStatus.WAITING, WaitlistStatus.OFFERED)).isTrue();
        assertThat(WaitlistStatus.canTransition(WaitlistStatus.OFFERED, WaitlistStatus.ACCEPTED)).isTrue();
        assertThat(WaitlistStatus.canTransition(WaitlistStatus.OFFERED, WaitlistStatus.EXPIRED)).isTrue();
        assertThat(WaitlistStatus.canTransition(WaitlistStatus.ACCEPTED, WaitlistStatus.WAITING)).isFalse();
    }

    private static WaitlistEntry entry(Long id, LocalDateTime createdAt) {
        WaitlistEntry entry = new WaitlistEntry();
        entry.setId(id);
        entry.setPriority(WaitlistEntry.priorityFor(TriageRiskLevel.ROUTINE));
        entry.setCreatedAt(createdAt);
        return entry;
    }
}
