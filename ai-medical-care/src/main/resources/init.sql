-- 智能医疗系统数据库初始化脚本
-- 数据库: guiguxiaozhi

-- 创建预约表
CREATE TABLE IF NOT EXISTS appointment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL COMMENT '用户姓名',
    id_card VARCHAR(18) NOT NULL COMMENT '身份证号',
    department VARCHAR(100) NOT NULL COMMENT '科室名称',
    date VARCHAR(20) NOT NULL COMMENT '预约日期',
    time VARCHAR(10) NOT NULL COMMENT '预约时间(上午/下午)',
    doctor_name VARCHAR(50) COMMENT '医生姓名',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约挂号表';

-- 创建医生表
CREATE TABLE IF NOT EXISTS doctor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '医生姓名',
    title VARCHAR(50) NOT NULL COMMENT '职称',
    department VARCHAR(100) NOT NULL COMMENT '所属科室',
    specialty VARCHAR(200) COMMENT '擅长领域',
    description TEXT COMMENT '医生简介'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生信息表';

-- 创建排班表
CREATE TABLE IF NOT EXISTS schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    doctor_name VARCHAR(50) NOT NULL COMMENT '医生姓名',
    department VARCHAR(100) NOT NULL COMMENT '科室名称',
    date VARCHAR(20) NOT NULL COMMENT '排班日期',
    time VARCHAR(10) NOT NULL COMMENT '排班时间(上午/下午)',
    total_slots INT DEFAULT 20 COMMENT '总号源数',
    booked_slots INT DEFAULT 0 COMMENT '已预约数'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生排班表';

-- 插入医生数据 (神经内科)
INSERT INTO doctor (name, title, department, specialty, description) VALUES
('彭斌', '主任医师 教授 博士研究生导师', '神经内科', '神经系统常见病多发病，脑血管病及神经系统危重疾病', '彭斌，男，主任医师，教授，神经内科主任，博士研究生导师。在神经系统常见病、疑难病的诊治方面具有较丰富的经验。'),
('崔丽英', '主任医师 教授 博士研究生导师', '神经内科', '运动神经元病、周围神经病、肌肉病等神经系统的各种疑难杂症', '崔丽英，曾任神经内科主任。中华医学会神经病学分会主任委员。主要从事运动神经元病、神经肌肉病和神经系统各种疑难杂症的诊治。'),
('朱以诚', '主任医师 教授 博士研究生导师', '神经内科', '脑血管病，脑小血管病，血管性帕金森，血管性认知功能障碍', '朱以诚，女，主任医师，教授，神经内科主任，神经病学系主任，博士研究生导师。在神经系统常见病、罕见病的诊治方面具有较丰富的经验。');

-- 插入医生数据 (口腔科)
INSERT INTO doctor (name, title, department, specialty, description) VALUES
('赵继志', '主任医师 教授', '口腔科', '口腔颌面部肿瘤、畸形的诊治及整形美容修复', '赵继志，教授、主任医师、医学博士。擅长口腔激光治疗、口腔颌面部肿瘤的诊治及颌面部缺损与畸形的整形与修复。'),
('万阔', '主任医师 教授', '口腔科', '无痛牙科治疗、牙科恐惧治疗、口腔内科治疗', '万阔，北京协和医院口腔科主任。中华口腔医学会镇静镇痛专业委员会主任委员。国内最早实施牙科无痛治疗与镇静技术的学者之一。'),
('赖钦声', '主任医师 教授', '口腔科', '口腔颌面部肿瘤、损伤、涎腺疾病、颞下颌关节疾病', '赖钦声，口腔医学教授，对口腔颌面部肿瘤、损伤、涎腺疾病、颞下颌关节疾病等的诊断和治疗均有丰富的经验。');

-- 生成未来7天的排班数据 (神经内科)
INSERT INTO schedule (doctor_id, doctor_name, department, date, time, total_slots, booked_slots)
SELECT d.id, d.name, d.department, DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL n DAY), '%Y-%m-%d'), '上午', 20, FLOOR(RAND() * 15)
FROM doctor d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) AS days
WHERE d.department = '神经内科';

INSERT INTO schedule (doctor_id, doctor_name, department, date, time, total_slots, booked_slots)
SELECT d.id, d.name, d.department, DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL n DAY), '%Y-%m-%d'), '下午', 15, FLOOR(RAND() * 10)
FROM doctor d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) AS days
WHERE d.department = '神经内科';

-- 生成未来7天的排班数据 (口腔科)
INSERT INTO schedule (doctor_id, doctor_name, department, date, time, total_slots, booked_slots)
SELECT d.id, d.name, d.department, DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL n DAY), '%Y-%m-%d'), '上午', 15, FLOOR(RAND() * 10)
FROM doctor d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) AS days
WHERE d.department = '口腔科';

INSERT INTO schedule (doctor_id, doctor_name, department, date, time, total_slots, booked_slots)
SELECT d.id, d.name, d.department, DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL n DAY), '%Y-%m-%d'), '下午', 10, FLOOR(RAND() * 8)
FROM doctor d
CROSS JOIN (SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) AS days
WHERE d.department = '口腔科';