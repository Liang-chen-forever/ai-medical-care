package com.liang.medical.triage;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
public class EmergencyRiskRuleEngine {

    public Optional<RiskRuleMatch> match(String chiefComplaint) {
        if (chiefComplaint == null) {
            return Optional.empty();
        }

        String text = chiefComplaint.toLowerCase(Locale.ROOT);
        if ((text.contains("胸痛") || text.contains("胸口痛"))
                && (text.contains("呼吸困难") || text.contains("喘不过气") || text.contains("气短"))) {
            return Optional.of(new RiskRuleMatch("CHEST_PAIN_WITH_BREATHLESSNESS"));
        }
        if (text.contains("大出血") || text.contains("大量出血") || text.contains("止不住血")) {
            return Optional.of(new RiskRuleMatch("BLEEDING"));
        }
        if (text.contains("昏过去") || text.contains("意识丧失") || text.contains("失去意识")) {
            return Optional.of(new RiskRuleMatch("LOSS_OF_CONSCIOUSNESS"));
        }
        if (text.contains("偏瘫") || text.contains("一侧无力") || text.contains("言语不清") || text.contains("说话含糊")) {
            return Optional.of(new RiskRuleMatch("ACUTE_NEUROLOGICAL_DEFICIT"));
        }
        if (text.contains("自杀") || text.contains("自残") || text.contains("伤害自己") || text.contains("不想活")) {
            return Optional.of(new RiskRuleMatch("SELF_HARM_RISK"));
        }
        return Optional.empty();
    }
}
