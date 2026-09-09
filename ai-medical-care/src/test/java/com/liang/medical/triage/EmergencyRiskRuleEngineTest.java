package com.liang.medical.triage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class EmergencyRiskRuleEngineTest {

    private final EmergencyRiskRuleEngine engine = new EmergencyRiskRuleEngine();

    @ParameterizedTest
    @CsvSource({
            "胸痛并且喘不过气,CHEST_PAIN_WITH_BREATHLESSNESS",
            "突然大出血,BLEEDING",
            "刚刚昏过去了,LOSS_OF_CONSCIOUSNESS",
            "一侧手脚无力说话含糊,ACUTE_NEUROLOGICAL_DEFICIT",
            "我不想活了,SELF_HARM_RISK"
    })
    void emergencySymptomsReturnStableRuleCodes(String complaint, String code) {
        assertThat(engine.match(complaint)).contains(new RiskRuleMatch(code));
    }

    @Test
    void nonEmergencyComplaintHasNoEmergencyMatch() {
        assertThat(engine.match("最近反复头痛，想了解应该挂什么科")).isEmpty();
    }
}
