-- phpMyAdmin SQL Dump
-- version 5.1.1
-- https://www.phpmyadmin.net/
--
-- 主机： localhost
-- 生成日期： 2025-09-11 13:08:20
-- 服务器版本： 5.6.50-log
-- PHP 版本： 8.0.26

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- 数据库： `spacemit`
--

-- --------------------------------------------------------

--
-- 表的结构 `devices`
--

CREATE TABLE `devices` (
  `id` int(11) NOT NULL,
  `device_id` varchar(50) NOT NULL,
  `device_type` varchar(100) DEFAULT NULL,
  `version` varchar(20) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `last_seen` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `status` enum('ONLINE','OFFLINE','LOST') DEFAULT 'OFFLINE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- 转存表中的数据 `devices`
--

INSERT INTO `devices` (`id`, `device_id`, `device_type`, `version`, `user_id`, `last_seen`, `created_at`, `status`) VALUES
(1, 'A8888', 'Muse Pi Pro Plus', '1.0.0', 1, '2025-09-10 20:44:37', '2025-09-05 09:01:47', 'LOST'),
(2, 'A6666', 'Muse Pi Pro Plus', '1.0.0', 2, '2025-09-05 21:19:47', '2025-09-05 09:01:47', 'LOST'),
(3, 'A5555', 'Muse Pi Pro Plus', '1.0.0', 2, '2025-09-06 05:04:25', '2025-09-05 01:14:45', 'LOST');

-- --------------------------------------------------------

--
-- 表的结构 `events`
--

CREATE TABLE `events` (
  `id` int(11) NOT NULL,
  `event_id` varchar(50) NOT NULL,
  `device_id` varchar(50) NOT NULL,
  `timestamp` timestamp NOT NULL,
  `event_type` enum('FATIGUE','DISTRACTION','EMERGENCY','SYSTEM') NOT NULL,
  `severity` enum('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,
  `location_lat` decimal(10,8) DEFAULT NULL,
  `location_lng` decimal(11,8) DEFAULT NULL,
  `behavior` varchar(50) DEFAULT NULL,
  `confidence` decimal(4,3) DEFAULT NULL,
  `duration` decimal(8,2) DEFAULT NULL,
  `alert_level` varchar(20) DEFAULT NULL,
  `gpio_triggered` text,
  `context` text,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- 转存表中的数据 `events`
--

INSERT INTO `events` (`id`, `event_id`, `device_id`, `timestamp`, `event_type`, `severity`, `location_lat`, `location_lng`, `behavior`, `confidence`, `duration`, `alert_level`, `gpio_triggered`, `context`, `created_at`) VALUES
(2, 'FATIGUE_001_131750', 'A8888', '2025-09-07 05:17:50', 'FATIGUE', 'HIGH', '33.55373300', '119.03095300', 'FATIGUE_DETECTED', '0.850', '120.50', 'MODERATE', 'GPIO_12', '疲劳驾驶检测：眨眼频率异常，建议休息', '2025-09-06 12:08:48'),
(3, 'FATIGUE_002_131810', 'A8888', '2025-09-07 05:19:10', 'FATIGUE', 'CRITICAL', '33.55374500', '119.03096800', 'SEVERE_FATIGUE', '0.920', '180.00', 'SEVERE', 'GPIO_12,GPIO_13', '严重疲劳驾驶：多次打哈欠，注意力严重下降，立即停车休息', '2025-09-06 12:08:48'),
(4, 'DISTRACTION_001_131755', 'A6666', '2025-09-07 05:22:55', 'DISTRACTION', 'MEDIUM', '33.55410000', '119.03120000', 'PHONE_USAGE', '0.750', '45.20', 'MEDIUM', 'GPIO_15', '分心驾驶：检测到手机使用行为', '2025-09-06 12:08:48'),
(5, 'EMERGENCY_001_131800', 'A6666', '2025-09-07 06:18:00', 'EMERGENCY', 'CRITICAL', '33.55280000', '119.02950000', 'SUDDEN_BRAKE', '0.950', '2.10', 'CRITICAL', 'GPIO_8', '紧急制动：检测到急刹车行为', '2025-09-06 12:08:48'),
(6, 'EVENT_001_120000', 'A5555', '2025-09-07 06:35:55', 'FATIGUE', 'HIGH', NULL, NULL, 'eye_closed', '0.950', '3.50', NULL, NULL, '{\"detection_count\": 3, \"consecutive_frames\": 15}', '2025-09-06 06:15:18'),
(14, 'EVENT_001_120001', 'A8888', '2025-09-08 01:07:00', 'FATIGUE', 'HIGH', '33.52373300', '119.03095300', 'eye_closed', '0.950', NULL, NULL, NULL, NULL, '2025-09-06 17:36:04'),
(15, 'EVENT_001_120002', 'A8888', '2025-09-08 01:10:40', 'FATIGUE', 'HIGH', '33.55373300', '119.03095300', 'eye_closed', '0.950', '3.50', 'HIGH', '{\"vibrator\": true, \"led\": true, \"buzzer\": false}', '疲劳驾驶检测：检测到开车瞌睡的情况', '2025-09-06 17:50:15'),
(16, 'EVENT_001_120012', 'A8888', '2025-09-08 01:11:07', 'FATIGUE', 'HIGH', '33.55373300', '119.03095300', 'eye_closed', '0.950', '3.50', 'HIGH', '{\"vibrator\": true, \"led\": true, \"buzzer\": false}', '疲劳驾驶检测：检测到开车瞌睡的情况', '2025-09-06 17:57:29'),
(17, 'EVENT_001_121012', 'A8888', '2025-09-06 20:00:00', 'FATIGUE', 'HIGH', '33.55373300', '119.03095300', 'eye_closed', '0.950', '3.50', 'HIGH', '{\"vibrator\": true, \"led\": true, \"buzzer\": false}', '疲劳驾驶检测：检测到开车瞌睡的情况', '2025-09-06 17:58:40'),
(18, 'EVENT_001_121312', 'A8888', '2025-09-06 20:00:00', 'FATIGUE', 'HIGH', '33.55373300', '119.03095300', 'eye_closed', '0.950', '3.50', 'HIGH', '{\"vibrator\": true, \"led\": true, \"buzzer\": false}', '疲劳驾驶检测：检测到开车瞌睡的情况', '2025-09-06 19:02:02'),
(19, 'EVENT_001_122312', 'A8888', '2025-09-06 20:00:00', 'FATIGUE', 'HIGH', '33.55373300', '119.03095300', 'eye_closed', '0.950', '3.50', 'HIGH', '{\"vibrator\": true, \"led\": true, \"buzzer\": false}', '疲劳驾驶检测：检测到开车瞌睡的情况', '2025-09-06 19:07:15'),
(20, 'EVENT_001_121313', 'A8888', '2025-09-06 20:02:00', 'FATIGUE', 'HIGH', '33.55373300', '119.03095300', 'eye_closed', '0.950', '3.50', 'HIGH', '{\"vibrator\": true, \"led\": true, \"buzzer\": false}', '疲劳驾驶检测：检测到开车瞌睡的情况', '2025-09-06 22:09:32'),
(21, 'EVT_1_1757405315', 'A8888', '2025-09-08 16:08:35', 'FATIGUE', 'HIGH', '39.78450000', '116.57686100', NULL, NULL, '0.00', 'HIGH', '{}', '疲劳驾驶检测', '2025-09-09 00:08:36'),
(22, 'EVT_1_1757407496', 'A8888', '2025-09-08 16:44:56', 'FATIGUE', 'HIGH', '33.55376833', '119.03093500', NULL, NULL, '0.00', 'HIGH', '{}', '疲劳驾驶检测', '2025-09-09 00:44:57');

-- --------------------------------------------------------

--
-- 表的结构 `gps_data`
--

CREATE TABLE `gps_data` (
  `id` int(11) NOT NULL,
  `device_id` varchar(50) NOT NULL,
  `timestamp` timestamp NOT NULL,
  `utc_time` varchar(20) DEFAULT NULL,
  `utc_date` varchar(10) DEFAULT NULL,
  `latitude` decimal(10,8) DEFAULT NULL,
  `longitude` decimal(11,8) DEFAULT NULL,
  `hdop` decimal(4,2) DEFAULT NULL,
  `altitude` decimal(8,2) DEFAULT NULL,
  `fix_mode` int(11) DEFAULT NULL,
  `course_over_ground` decimal(6,2) DEFAULT NULL,
  `speed_kmh` decimal(6,2) DEFAULT NULL,
  `speed_knots` decimal(6,2) DEFAULT NULL,
  `satellites` int(11) DEFAULT NULL,
  `raw_gps_data` text,
  `fatigue_score` decimal(4,3) DEFAULT NULL,
  `fatigue_level` enum('NORMAL','MILD','MODERATE','SEVERE') DEFAULT 'NORMAL',
  `eye_blink_rate` decimal(4,2) DEFAULT NULL,
  `head_movement_score` decimal(4,2) DEFAULT NULL,
  `yawn_count` int(11) DEFAULT '0',
  `attention_score` decimal(4,2) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- 转存表中的数据 `gps_data`
--

INSERT INTO `gps_data` (`id`, `device_id`, `timestamp`, `utc_time`, `utc_date`, `latitude`, `longitude`, `hdop`, `altitude`, `fix_mode`, `course_over_ground`, `speed_kmh`, `speed_knots`, `satellites`, `raw_gps_data`, `fatigue_score`, `fatigue_level`, `eye_blink_rate`, `head_movement_score`, `yawn_count`, `attention_score`, `created_at`) VALUES
(1, 'A8888', '2024-01-01 05:17:50', '131750.00', '030925', '33.55373300', '119.03095300', '1.23', '32.40', 3, NULL, '0.18', '0.10', 11, '131750.00,3333.2240N,11901.8572E,1.23,32.4,3,,0.179,0.098,030925,11', '0.850', 'MODERATE', '0.45', '0.32', 2, '0.78', '2025-09-06 12:08:48'),
(2, 'A8888', '2025-09-09 18:27:20', '102720.00', '100925', '33.55373333', '119.03095333', '1.23', '32.40', 3, NULL, '0.18', '0.10', 11, '102720.00,3333.2240N,11901.8572E,1.23,32.4,3,,0.179,0.098,100925,11', '0.850', 'NORMAL', '0.45', '0.32', 2, '0.78', '2025-09-06 12:08:48'),
(3, 'A6666', '2024-01-01 05:17:55', '131755.00', '030925', '33.55410000', '119.03120000', '0.98', '35.20', 3, '90.00', '25.80', '13.90', 13, '131755.00,3333.2460N,11901.8720E,0.98,35.2,3,90.0,25.8,13.9,030925,13', '0.450', 'NORMAL', '0.52', '0.15', 0, '0.88', '2025-09-06 12:08:48'),
(4, 'A6666', '2024-01-01 05:18:15', '131815.00', '030925', '33.55412000', '119.03122000', '1.05', '35.40', 3, '88.50', '28.30', '15.30', 12, '131815.00,3333.2472N,11901.8732E,1.05,35.4,3,88.5,28.3,15.3,030925,12', '0.670', 'MILD', '0.48', '0.22', 1, '0.82', '2025-09-06 12:08:48'),
(5, 'A5555', '2024-01-01 05:18:00', '131800.00', '030925', '33.55280000', '119.02950000', '1.45', '28.80', 3, '180.00', '0.00', '0.00', 10, '131800.00,3333.1680N,11901.7700E,1.45,28.8,3,180.0,0.0,0.0,030925,10', '0.780', 'MODERATE', '0.42', '0.35', 2, '0.71', '2025-09-06 12:08:48'),
(6, 'A5555', '2024-01-01 05:18:20', '131820.00', '030925', '33.55282000', '119.02952000', '1.38', '29.00', 3, '182.30', '2.10', '1.10', 11, '131820.00,3333.1692N,11901.7712E,1.38,29.0,3,182.3,2.1,1.1,030925,11', '0.890', 'SEVERE', '0.35', '0.42', 4, '0.58', '2025-09-06 12:08:48');

-- --------------------------------------------------------

--
-- 表的结构 `realtime_data`
--

CREATE TABLE `realtime_data` (
  `id` int(11) NOT NULL,
  `device_id` varchar(50) NOT NULL,
  `timestamp` timestamp NOT NULL,
  `location_lat` decimal(10,8) DEFAULT NULL,
  `location_lng` decimal(11,8) DEFAULT NULL,
  `speed` decimal(6,2) DEFAULT NULL,
  `direction` decimal(6,2) DEFAULT NULL,
  `altitude` decimal(8,2) DEFAULT NULL,
  `hdop` decimal(4,2) DEFAULT NULL,
  `satellites` int(11) DEFAULT NULL,
  `fix_mode` int(11) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- 转存表中的数据 `realtime_data`
--

INSERT INTO `realtime_data` (`id`, `device_id`, `timestamp`, `location_lat`, `location_lng`, `speed`, `direction`, `altitude`, `hdop`, `satellites`, `fix_mode`, `created_at`) VALUES
(1, 'A8888', '2025-09-09 16:27:20', '33.55373333', '119.03095333', '132.32', NULL, '32.40', '1.23', 11, 3, '2025-09-05 01:44:41'),
(2, 'A6666', '2025-09-09 00:00:00', '33.55353200', '119.03096200', '23.50', '90.00', '32.40', '1.23', 11, NULL, '2025-09-05 21:51:00'),
(3, 'A5555', '2025-09-08 01:20:07', '33.55473100', '119.03085100', '60.50', '90.00', '32.40', '1.23', 11, NULL, '2025-09-05 21:51:08');

-- --------------------------------------------------------

--
-- 表的结构 `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `username` varchar(50) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `email` varchar(100) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `status` enum('ACTIVE','INACTIVE') DEFAULT 'ACTIVE',
  `last_login_at` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- 转存表中的数据 `users`
--

INSERT INTO `users` (`id`, `username`, `password_hash`, `email`, `phone`, `created_at`, `updated_at`, `status`, `last_login_at`) VALUES
(1, '用户A', 'testpassword', 'test@example.com', '19516161616', '2025-09-05 09:01:47', '2025-09-08 04:18:16', 'ACTIVE', NULL),
(2, '用户B', 'admin123', 'admin@example.com', NULL, '2025-09-05 09:01:47', '2025-09-08 04:18:25', 'ACTIVE', NULL),
(3, '用户C', 'driver123', 'driver001@example.com', '13700137000', '2025-09-06 13:23:16', '2025-09-08 04:18:33', 'ACTIVE', '2024-01-14 08:45:00'),
(4, '用户D', 'driver456', 'driver002@example.com', '13600136000', '2025-09-06 13:23:16', '2025-09-08 04:18:39', 'INACTIVE', '2024-01-10 06:20:00');

--
-- 转储表的索引
--

--
-- 表的索引 `devices`
--
ALTER TABLE `devices`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `device_id` (`device_id`);

--
-- 表的索引 `events`
--
ALTER TABLE `events`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `event_id` (`event_id`);

--
-- 表的索引 `gps_data`
--
ALTER TABLE `gps_data`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_device_timestamp` (`device_id`,`timestamp`),
  ADD KEY `idx_timestamp` (`timestamp`);

--
-- 表的索引 `realtime_data`
--
ALTER TABLE `realtime_data`
  ADD PRIMARY KEY (`id`);

--
-- 表的索引 `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `username` (`username`);

--
-- 在导出的表使用AUTO_INCREMENT
--

--
-- 使用表AUTO_INCREMENT `devices`
--
ALTER TABLE `devices`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- 使用表AUTO_INCREMENT `events`
--
ALTER TABLE `events`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=27;

--
-- 使用表AUTO_INCREMENT `gps_data`
--
ALTER TABLE `gps_data`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- 使用表AUTO_INCREMENT `realtime_data`
--
ALTER TABLE `realtime_data`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- 使用表AUTO_INCREMENT `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
