-- 为已有演示数据库补充从今天开始的未来 7 天排班。
-- 该脚本只插入缺失的 doctor/date/time 组合，不删除历史排班和预约，可重复执行。
USE guiguxiaozhi;
SET NAMES utf8mb4;

INSERT INTO schedule (doctor_id, doctor_name, department, date, time, total_slots, booked_slots)
SELECT d.id,
       d.name,
       d.department,
       DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL days.n DAY), '%Y-%m-%d'),
       '上午',
       20,
       0
FROM doctor d
CROSS JOIN (
    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
    UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
) days
WHERE NOT EXISTS (
    SELECT 1
    FROM schedule s
    WHERE s.doctor_id = d.id
      AND s.date = DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL days.n DAY), '%Y-%m-%d')
      AND s.time = '上午'
);

INSERT INTO schedule (doctor_id, doctor_name, department, date, time, total_slots, booked_slots)
SELECT d.id,
       d.name,
       d.department,
       DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL days.n DAY), '%Y-%m-%d'),
       '下午',
       15,
       0
FROM doctor d
CROSS JOIN (
    SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
    UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
) days
WHERE NOT EXISTS (
    SELECT 1
    FROM schedule s
    WHERE s.doctor_id = d.id
      AND s.date = DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL days.n DAY), '%Y-%m-%d')
      AND s.time = '下午'
);
