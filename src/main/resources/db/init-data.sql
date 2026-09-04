-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL,
    `password` VARCHAR(100) NOT NULL,
    `id_card` VARCHAR(18) NOT NULL,
    `phone` VARCHAR(20),
    `role` VARCHAR(16) NOT NULL DEFAULT 'PATIENT',
    `create_time` VARCHAR(20),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_id_card` (`id_card`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Fresh demo data deliberately provisions role-bearing doctor principals before mapping doctor profiles.
-- These BCrypt values are password verifiers, not deployable credentials; real accounts are provisioned outside this script.
INSERT INTO `user` (username, password, id_card, phone, role, create_time) VALUES
('doctor_zhang', '$2a$10$8B08yjQzT3jMgLPL2WBxKOacruT3vQE2aXotdMIcJ3TrGl15xkv8e', '110105198001010011', NULL, 'DOCTOR', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')),
('doctor_li', '$2a$10$hULAPysviNTtDUnPj5M9quG1ccM7YHiDZ7Kqwc.Hg.NREcwa3ePRm', '110105198002020022', NULL, 'DOCTOR', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')),
('doctor_wang', '$2a$10$NfEUg8d8j8LLHITIlr4cAexOmSFG.Vfe5BThyocw9EubXJvga0hG6', '110105198003030033', NULL, 'DOCTOR', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')),
('doctor_chen', '$2a$10$Dj7ZqHJeoE7JtKQErbeIKOU6F1WzozLntxwMJt0oPFrS3pmYH.z.m', '110105198004040044', NULL, 'DOCTOR', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')),
('doctor_zhao', '$2a$10$Z3Erpa11oeLBwqaEKSh40eZqSRZ2lNEAN2WStCQG3E5gYhVx6T0C2', '110105198005050055', NULL, 'DOCTOR', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')),
('doctor_liu', '$2a$10$OoOWZoJ9ge1rEuBHLB9fJeAtAI4GcU8C0z2d5EeVqUuQJwDdT5M.u', '110105198006060066', NULL, 'DOCTOR', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s'));

-- 医生表
CREATE TABLE IF NOT EXISTS `doctor` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id` BIGINT NULL,
    `name` VARCHAR(50) NOT NULL,
    `title` VARCHAR(50) NOT NULL,
    `department` VARCHAR(50) NOT NULL,
    `specialty` VARCHAR(500),
    `description` VARCHAR(1000),
    UNIQUE KEY `uk_doctor_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 排班表
CREATE TABLE IF NOT EXISTS `schedule` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `doctor_id` BIGINT NOT NULL,
    `doctor_name` VARCHAR(50) NOT NULL,
    `department` VARCHAR(50) NOT NULL,
    `date` VARCHAR(20) NOT NULL,
    `time` VARCHAR(10) NOT NULL,
    `total_slots` INT DEFAULT 30,
    `booked_slots` INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 预约表：预约归属以用户 ID 和排班 ID 为准，姓名、身份证和排班信息作为快照保留。
CREATE TABLE IF NOT EXISTS `appointment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL,
    `id_card` VARCHAR(18) NOT NULL,
    `department` VARCHAR(100) NOT NULL,
    `date` VARCHAR(20) NOT NULL,
    `time` VARCHAR(10) NOT NULL,
    `doctor_name` VARCHAR(50),
    `user_id` BIGINT NOT NULL,
    `schedule_id` BIGINT NOT NULL,
    `doctor_id` BIGINT NOT NULL,
    `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    `cancel_reason` VARCHAR(200),
    `handled_by` BIGINT,
    `handled_at` DATETIME,
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_appointment_user_schedule` (`user_id`, `schedule_id`),
    KEY `idx_appointment_user_id` (`user_id`),
    KEY `idx_appointment_schedule_id` (`schedule_id`),
    KEY `idx_appointment_doctor_status` (`doctor_id`, `status`),
    KEY `idx_appointment_user_status` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入医生数据（神经内科）
INSERT INTO doctor (user_id, name, title, department, specialty, description) VALUES
((SELECT id FROM `user` WHERE username = 'doctor_zhang'), '张明远', '主任医师', '神经内科', '脑血管疾病、帕金森病、癫痫的诊断与治疗', '从事神经内科临床工作30余年，擅长脑血管疾病的介入治疗，在帕金森病的早期诊断方面有丰富经验。'),
((SELECT id FROM `user` WHERE username = 'doctor_li'), '李慧芳', '副主任医师', '神经内科', '头痛、眩晕、失眠、周围神经病变', '北京协和医学院博士毕业，专注于头痛和眩晕的临床研究，发表SCI论文20余篇。'),
((SELECT id FROM `user` WHERE username = 'doctor_wang'), '王建国', '主治医师', '神经内科', '脑卒中康复、神经肌肉疾病、阿尔茨海默病', '擅长脑卒中后康复治疗，对阿尔茨海默病的早期干预有深入研究。');

-- 插入医生数据（口腔科）
INSERT INTO doctor (user_id, name, title, department, specialty, description) VALUES
((SELECT id FROM `user` WHERE username = 'doctor_chen'), '陈思远', '主任医师', '口腔科', '口腔种植、牙体牙髓病、口腔颌面外科', '口腔种植领域权威专家，完成种植手术超过5000例，承担多项国家级科研项目。'),
((SELECT id FROM `user` WHERE username = 'doctor_zhao'), '赵晓琳', '副主任医师', '口腔科', '牙齿正畸、儿童口腔、口腔美容修复', '擅长隐形矫正和儿童早期矫治，在口腔美容修复方面有独特见解。'),
((SELECT id FROM `user` WHERE username = 'doctor_liu'), '刘伟', '主治医师', '口腔科', '牙周病治疗、口腔黏膜病、口腔预防保健', '专注于牙周病的系统治疗，对口腔黏膜病的诊断有丰富经验。');

-- 插入未来7天的排班数据
INSERT INTO schedule (doctor_id, doctor_name, department, date, time, total_slots, booked_slots) VALUES
-- 今天
(1, '张明远', '神经内科', CURDATE(), '上午', 30, 12),
(1, '张明远', '神经内科', CURDATE(), '下午', 30, 8),
(2, '李慧芳', '神经内科', CURDATE(), '上午', 25, 15),
(2, '李慧芳', '神经内科', CURDATE(), '下午', 25, 5),
(3, '王建国', '神经内科', CURDATE(), '上午', 20, 10),
(3, '王建国', '神经内科', CURDATE(), '下午', 20, 3),
(4, '陈思远', '口腔科', CURDATE(), '上午', 20, 18),
(4, '陈思远', '口腔科', CURDATE(), '下午', 20, 12),
(5, '赵晓琳', '口腔科', CURDATE(), '上午', 25, 20),
(5, '赵晓琳', '口腔科', CURDATE(), '下午', 25, 10),
(6, '刘伟', '口腔科', CURDATE(), '上午', 30, 15),
(6, '刘伟', '口腔科', CURDATE(), '下午', 30, 8),

-- 明天
(1, '张明远', '神经内科', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '上午', 30, 5),
(1, '张明远', '神经内科', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '下午', 30, 0),
(2, '李慧芳', '神经内科', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '上午', 25, 10),
(2, '李慧芳', '神经内科', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '下午', 25, 0),
(3, '王建国', '神经内科', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '上午', 20, 8),
(4, '陈思远', '口腔科', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '上午', 20, 15),
(4, '陈思远', '口腔科', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '下午', 20, 0),
(5, '赵晓琳', '口腔科', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '上午', 25, 5),
(6, '刘伟', '口腔科', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '上午', 30, 20),
(6, '刘伟', '口腔科', DATE_ADD(CURDATE(), INTERVAL 1 DAY), '下午', 30, 0),

-- 后天
(1, '张明远', '神经内科', DATE_ADD(CURDATE(), INTERVAL 2 DAY), '上午', 30, 0),
(2, '李慧芳', '神经内科', DATE_ADD(CURDATE(), INTERVAL 2 DAY), '上午', 25, 0),
(2, '李慧芳', '神经内科', DATE_ADD(CURDATE(), INTERVAL 2 DAY), '下午', 25, 0),
(3, '王建国', '神经内科', DATE_ADD(CURDATE(), INTERVAL 2 DAY), '下午', 20, 0),
(4, '陈思远', '口腔科', DATE_ADD(CURDATE(), INTERVAL 2 DAY), '上午', 20, 0),
(5, '赵晓琳', '口腔科', DATE_ADD(CURDATE(), INTERVAL 2 DAY), '上午', 25, 0),
(5, '赵晓琳', '口腔科', DATE_ADD(CURDATE(), INTERVAL 2 DAY), '下午', 25, 0),
(6, '刘伟', '口腔科', DATE_ADD(CURDATE(), INTERVAL 2 DAY), '上午', 30, 0);
