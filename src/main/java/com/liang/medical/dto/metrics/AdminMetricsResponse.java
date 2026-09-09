package com.liang.medical.dto.metrics;

import java.util.Map;

public record AdminMetricsResponse(Map<String, Long> appointments,
                                   Map<String, Long> triageCases,
                                   Map<String, Long> waitlistEntries) {
}
