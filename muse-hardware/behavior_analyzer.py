# -*- coding: utf-8 -*-
"""
行为分析模块
处理疲劳和分心检测逻辑，管理行为追踪和警报触发
"""

import time
import logging
import pandas as pd
import os
from datetime import datetime
from collections import deque
from config import (
    CONFIG, BEHAVIOR_WEIGHTS, FATIGUE_CLASSES, LABEL_MAP
)

class BehaviorAnalyzer:
    """行为分析器类"""
    
    def __init__(self, gpio_controller, socketio, network_manager=None):
        self.gpio_controller = gpio_controller
        self.socketio = socketio
        self.network_manager = network_manager
        
        # 行为追踪状态
        self.behavior_tracker = {}
        self.last_multi_event_time = 0.0
        self.last_level3_time = 0.0
        self.events = []
        self.event_log_file = "driving_events.csv"
        
        # 分心统计
        self.distracted_count = 0
        self.distracted_timestamps = deque(maxlen=100)
        self.last_distracted_time = 0.0
        self.last_safe_time = None
        
        # 进度评分
        self.progress_score = 0.0
        self.current_level = "Normal"
        
        # 测试模式状态
        self.is_test_mode = False
        self.test_score = 0.0
        self.test_behaviors = []
        self.test_mode_start_time = 0.0
        self.last_test_behavior_time = 0.0
        self.test_behavior_duration = 0.0
    
    def check_fatigue_or_distracted(self, detections, current_time):
        """检查疲劳或分心状态"""
        try:
            logging.debug(f"[BehaviorAnalyzer] 开始检查疲劳/分心状态，检测数量: {len(detections) if detections else 0}")
            is_fatigue = False
            is_distracted = False
            new_events = []
            
            # 如果是测试模式，使用测试逻辑但保持正常的分数回退机制
            if self.is_test_mode:
                return self._handle_test_mode_logic(current_time)
            
            # 存储当前检测结果用于清醒驾驶检测
            self._current_detections = detections
            
            # 确定当前等级（提高阈值，减缓等级上升速度）
            prev_level = self.current_level
            if self.progress_score >= 95:
                self.current_level = "Level 3"
            elif self.progress_score >= 80:  # 从75提高到80，减缓等级上升
                self.current_level = "Level 2"
            elif self.progress_score >= 60:  # 从50提高到60，减缓等级上升
                self.current_level = "Level 1"
            else:
                self.current_level = "Normal"
            
            # 清醒重置逻辑
            self._handle_safe_driving_reset(current_time)
            
            # 更新行为追踪
            self._update_behavior_tracking(detections, current_time)
            
            # 触发警报
            import threading
            self._trigger_alerts(prev_level, current_time)
            
            # 检查单一行为
            is_fatigue, is_distracted, new_events = self._check_single_behaviors(current_time)
            
            # 检查三级警报
            if self._check_level3_conditions(current_time):
                is_fatigue = True
                new_events.extend(self._create_level3_event(current_time))
            
            # 检查多行为分心
            if self._check_multi_behavior_distraction(current_time):
                is_distracted = True
                new_events.extend(self._create_multi_behavior_event(current_time))
            
            # 检查连续分心
            if self._check_continuous_distraction(current_time):
                new_events.extend(self._create_continuous_distraction_event(current_time))
            
            # 更新状态
            if is_fatigue or is_distracted:
                self.last_distracted_time = current_time
                self.last_safe_time = None
            
            # 保存事件（过滤掉None值）
            valid_events = [event for event in new_events if event is not None]
            self.events.extend(valid_events)
            if len(self.events) > 200:
                self.events = self.events[-200:]
            
            logging.debug(f"[BehaviorAnalyzer] 发送事件: {new_events}, 分心次数: {self.distracted_count}, 进度: {self.progress_score}, 等级: {self.current_level}")
            
            return is_fatigue, is_distracted, new_events, self.distracted_count, self.progress_score, self.current_level
        
        except Exception as e:
            import traceback
            error_traceback = traceback.format_exc()
            logging.error(f"[BehaviorAnalyzer] check_fatigue_or_distracted 函数错误: {e}")
            logging.error(f"[BehaviorAnalyzer] 错误堆栈: {error_traceback}")
            logging.error(f"[BehaviorAnalyzer] 当前检测结果: {detections}")
            logging.error(f"[BehaviorAnalyzer] 当前时间: {current_time}")
            # 返回默认值避免程序崩溃
            return False, False, [], self.distracted_count, self.progress_score, self.current_level
    
    def _handle_safe_driving_reset(self, current_time):
        """处理安全驾驶重置逻辑"""
        # 检查是否所有检测都是专注驾驶或低置信度
        # 放宽条件：如果有专注驾驶检测，即使有其他低置信度检测也认为是专注驾驶
        all_focused = False
        has_focused = False
        has_high_conf_other = False
        
        for detection in self._get_current_detections():
            if detection["label"] == "focused" and detection["confidence"] >= CONFIG["focused_min_confidence"]:
                has_focused = True
            elif detection["label"] != "focused" and detection["confidence"] >= CONFIG["min_confidence"]:
                has_high_conf_other = True
        
        # 如果有专注驾驶检测，且没有其他高置信度的非专注行为，认为是专注驾驶
        all_focused = has_focused and not has_high_conf_other
        
        if all_focused:
            if self.last_safe_time is None:
                self.last_safe_time = current_time
            elif current_time - self.last_safe_time >= CONFIG["safe_driving_confirm_time"]:
                if self.current_level == "Level 3":
                    self._reset_to_level1()
                elif current_time - self.last_safe_time >= CONFIG["level_reset_threshold"]:
                    self._reset_to_normal()
                self.progress_score = max(0.0, self.progress_score - CONFIG["progress_decrement_focused"])
        else:
            self.last_safe_time = None
            self.progress_score = max(0.0, min(100.0, self.progress_score - CONFIG["progress_decrement_normal"]))
    
    def _get_current_detections(self):
        """获取当前检测结果（用于清醒驾驶检测）"""
        return getattr(self, '_current_detections', [])
    
    def _reset_to_level1(self):
        """重置到一级"""
        self.behavior_tracker.clear()
        self.distracted_timestamps.clear()
        self.last_multi_event_time = 0.0
        self.last_level3_time = 0.0
        self.progress_score = 50.0
        self.current_level = "Level 1"
        logging.info("三级警报后检测到持续清醒，降至一级")
    
    def _reset_to_normal(self):
        """重置到正常状态"""
        self.behavior_tracker.clear()
        self.distracted_timestamps.clear()
        self.last_multi_event_time = 0.0
        self.last_level3_time = 0.0
        self.progress_score = 0.0
        self.current_level = "Normal"
        # logging.info("长时间无分心，恢复正常状态111")
    
    def _update_behavior_tracking(self, detections, current_time):
        """更新行为追踪"""
        for detection in detections:
            label = detection["label"]
            conf = detection["confidence"]
            conf_threshold = CONFIG["fatigue_min_confidence"] if label in FATIGUE_CLASSES else CONFIG["min_confidence"]
            
            if label not in BEHAVIOR_WEIGHTS or label == "focused" or conf < conf_threshold:
                continue
            
            if label not in self.behavior_tracker:
                self.behavior_tracker[label] = {
                    "detections": deque(maxlen=100), 
                    "last_single_alert_time": 0.0, 
                    "last_event_time": 0.0
                }
            
            self.behavior_tracker[label]["detections"].append((current_time, conf))
            
            # 调整累加逻辑：降低累加速度，减缓等级上升
            duration = current_time - (self.behavior_tracker[label]["detections"][0][0] if self.behavior_tracker[label]["detections"] else current_time)
            # 将持续时间因子从20.0秒改为30.0秒，降低累加速度
            progress_increment = CONFIG["progress_increment"] * BEHAVIOR_WEIGHTS.get(label, 0) * min(1.5, 1 + duration / 30.0)
            self.progress_score = min(100.0, self.progress_score + progress_increment)
            
            logging.debug(f"行为: {label}, 增量: {progress_increment:.2f}, 持续时间: {duration:.2f}s, 当前分数: {self.progress_score:.2f}")
        
        # 清理过期数据
        for label in list(self.behavior_tracker.keys()):
            data = self.behavior_tracker[label]
            while data["detections"] and data["detections"][0][0] < current_time - CONFIG["window_size"]:
                data["detections"].popleft()
            if not data["detections"]:
                del self.behavior_tracker[label]
    
    def _trigger_alerts(self, prev_level, current_time):
        """触发警报"""
        import threading
        
        level_changed = False
        
        # 使用新的 PLC 控制方法，实现正确的叠加和下降逻辑
        if self.current_level != prev_level:
            self.gpio_controller.update_alert_level(self.current_level)
            self.socketio.emit("level_update", {"level": self.current_level, "progress": self.progress_score})
            level_changed = True
            
            if self.current_level == "Level 3":
                logging.info("=============== 疲劳等级变化: 触发三级警报 (分数: {:.1f})".format(self.progress_score))
                print("=============== 疲劳等级变化: 触发三级警报 (分数: {:.1f})".format(self.progress_score))
            elif self.current_level == "Level 2":
                logging.info("=============== 疲劳等级变化: 触发二级警报 (分数: {:.1f})".format(self.progress_score))
                print("=============== 疲劳等级变化: 触发二级警报 (分数: {:.1f})".format(self.progress_score))
            elif self.current_level == "Level 1":
                logging.info("=============== 疲劳等级变化: 触发一级警报 (分数: {:.1f})".format(self.progress_score))
                print("=============== 疲劳等级变化: 触发一级警报 (分数: {:.1f})".format(self.progress_score))
            elif self.current_level == "Normal":
                logging.info("=============== 疲劳等级变化: 恢复正常状态 (分数: {:.1f})".format(self.progress_score))
                print("=============== 疲劳等级变化: 恢复正常状态 (分数: {:.1f})".format(self.progress_score))
        
        # 只在等级真正变化时触发网络上报
        if level_changed and self.network_manager:
            logging.info("=============== 网络上报: 疲劳等级变化 {} -> {}, 触发网络上报".format(prev_level, self.current_level))
            print("=============== 网络上报: 疲劳等级变化 {} -> {}, 触发网络上报".format(prev_level, self.current_level))
            threading.Thread(target=self._trigger_network_report, args=(current_time, self.current_level), daemon=True).start()
    
    def _check_single_behaviors(self, current_time):
        """检查单一行为"""
        is_fatigue = False
        is_distracted = False
        new_events = []
        
        for label, data in self.behavior_tracker.items():
            duration_threshold = CONFIG["fatigue_duration_threshold"] if label in FATIGUE_CLASSES else CONFIG["duration_threshold"]
            recent_detections = [t for t, c in data["detections"] if t >= current_time - duration_threshold]
            recent_long_detections = [t for t, c in data["detections"] if t >= current_time - 2.0]
            
            # 检查持续时间阈值
            if (len(recent_detections) >= CONFIG["min_detections_for_duration"] and
                    current_time - data["last_single_alert_time"] >= duration_threshold and
                    label not in FATIGUE_CLASSES):
                
                event = self._create_single_behavior_event(label, data, recent_detections, current_time, "Distracted")
                new_events.append(event)
                data["last_event_time"] = current_time
                data["last_single_alert_time"] = current_time
                is_distracted = True
                self.distracted_count += 1
                self.distracted_timestamps.append(current_time)
            
            # 检查短期疲劳行为
            elif (len(recent_long_detections) >= 2 and
                  current_time - data["last_single_alert_time"] >= 2.0):
                
                event_type = "Distracted" if label not in FATIGUE_CLASSES else "Fatigue"
                event = self._create_single_behavior_event(label, data, recent_long_detections, current_time, event_type)
                new_events.append(event)
                data["last_event_time"] = current_time
                data["last_single_alert_time"] = current_time
                
                if event_type == "Fatigue":
                    is_fatigue = True
                else:
                    is_distracted = True
                
                self.distracted_count += 1
                self.distracted_timestamps.append(current_time)
        
        return is_fatigue, is_distracted, new_events
    
    def _create_single_behavior_event(self, label, data, detections, current_time, event_type):
        """创建单一行为事件"""
        avg_conf = sum(c for t, c in data["detections"]) / len(data["detections"])
        level = "Level 1" if self.progress_score < 80 else "Level 2"  # 从75提高到80
        duration = 2.0 if len(detections) >= 2 else CONFIG["duration_threshold"]
        
        # 检查事件合并 - 添加更安全的检查
        can_merge = False
        if (self.events and len(self.events) > 0 and self.events[-1] is not None and 
            isinstance(self.events[-1], dict) and "behavior" in self.events[-1]):
            can_merge = (current_time - data["last_event_time"] < CONFIG["event_merge_window"] and 
                        self.events[-1]["behavior"] == label)
        
        if can_merge:
            self.events[-1]["duration"] = round(current_time - self.events[-1]["start_time"], 2)
            self.events[-1]["count"] += len(detections)
            self.events[-1]["confidence"] = round((self.events[-1]["confidence"] + avg_conf) / 2, 2)
            logging.debug(f"合并事件: {label}, 新持续时间: {self.events[-1]['duration']}, 新次数: {self.events[-1]['count']}")
            return None
        
        event = {
            "time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "start_time": current_time,
            "behavior": label,
            "duration": round(duration, 2),
            "count": len(detections),
            "confidence": round(avg_conf, 2),
            "event_type": event_type,
            "level": level
        }
        
        self._save_event_to_csv(event)
        return event
    
    def _check_level3_conditions(self, current_time):
        """检查三级警报条件"""
        fatigue_detections = sum(
            len([t for t, c in data["detections"] if t >= current_time - 5.0])
            for label, data in self.behavior_tracker.items() if label in FATIGUE_CLASSES
        )
        
        behavior_switches = self._calculate_behavior_switches(current_time)
        
        return (fatigue_detections >= 3 or behavior_switches >= 7) and \
               current_time - self.last_level3_time >= CONFIG["level3_cooldown"] and \
               self.progress_score >= 95  # Level 3阈值保持不变
    
    def _calculate_behavior_switches(self, current_time):
        """计算行为切换次数"""
        behavior_switches = 0
        last_behavior = None
        switch_times = deque(maxlen=100)
        
        for label, data in self.behavior_tracker.items():
            if len(data["detections"]) >= 2:
                if last_behavior and last_behavior != label:
                    switch_times.append(current_time)
                    if len(switch_times) >= 2 and switch_times[-1] - switch_times[0] <= 10:
                        behavior_switches += 1
                last_behavior = label
        
        return behavior_switches
    
    def _create_level3_event(self, current_time):
        """创建三级警报事件"""
        weighted_score = sum(
            BEHAVIOR_WEIGHTS.get(label, 0) * 
            (sum(c for t, c in data["detections"]) / len(data["detections"]) if data["detections"] else 0)
            for label, data in self.behavior_tracker.items() if label in FATIGUE_CLASSES
        )
        
        event = {
            "time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "start_time": current_time,
            "behavior": "多种疲劳行为",
            "duration": round(5.0, 2),
            "count": 3,
            "confidence": round(weighted_score, 2),
            "event_type": "Fatigue",
            "level": "Level 3"
        }
        
        self.last_level3_time = current_time
        self.last_multi_event_time = current_time
        self.distracted_count += 1
        self.distracted_timestamps.append(current_time)
        
        self._save_event_to_csv(event)
        return [event]
    
    def _check_multi_behavior_distraction(self, current_time):
        """检查多行为分心"""
        weighted_score = 0.0
        distinct_behaviors = set()
        
        for label, data in self.behavior_tracker.items():
            valid_detections = [(t, c) for t, c in data["detections"] if t >= current_time - CONFIG["window_size"]]
            if valid_detections:
                avg_conf = sum(d[1] for d in valid_detections) / len(valid_detections)
                weighted_score += BEHAVIOR_WEIGHTS.get(label, 0) * avg_conf
                distinct_behaviors.add(label)
        
        return (len(distinct_behaviors) >= CONFIG["count_threshold"] and
                weighted_score >= CONFIG["score_threshold"] and
                current_time - self.last_multi_event_time >= CONFIG["multi_event_cooldown"] and
                current_time - self.last_level3_time >= CONFIG["level3_cooldown"] and
                self.progress_score >= 80)  # 从75提高到80，减缓等级上升
    
    def _create_multi_behavior_event(self, current_time):
        """创建多行为分心事件"""
        weighted_score = sum(
            BEHAVIOR_WEIGHTS.get(label, 0) * 
            (sum(c for t, c in data["detections"]) / len(data["detections"]) if data["detections"] else 0)
            for label, data in self.behavior_tracker.items()
        )
        
        distinct_behaviors = len(self.behavior_tracker)
        
        # 检查事件合并 - 添加更安全的检查
        can_merge = False
        if (self.events and len(self.events) > 0 and self.events[-1] is not None and 
            isinstance(self.events[-1], dict) and "behavior" in self.events[-1]):
            can_merge = (current_time - self.last_multi_event_time < CONFIG["event_merge_window"] and 
                        self.events[-1]["behavior"] == "多种分心行为")
        
        if can_merge:
            self.events[-1]["duration"] = round(current_time - self.events[-1]["start_time"], 2)
            self.events[-1]["count"] += distinct_behaviors
            self.events[-1]["confidence"] = round((self.events[-1]["confidence"] + weighted_score) / 2, 2)
            logging.debug(f"合并多行为事件: 新持续时间: {self.events[-1]['duration']}, 新次数: {self.events[-1]['count']}")
            return []
        
        event = {
            "time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "start_time": current_time,
            "behavior": "多种分心行为",
            "duration": round(CONFIG["window_size"], 2),
            "count": distinct_behaviors,
            "confidence": round(weighted_score, 2),
            "event_type": "Distracted",
            "level": "Level 2"
        }
        
        self.last_multi_event_time = current_time
        self.distracted_count += 1
        self.distracted_timestamps.append(current_time)
        
        self._save_event_to_csv(event)
        return [event]
    
    def _check_continuous_distraction(self, current_time):
        """检查连续分心"""
        while self.distracted_timestamps and self.distracted_timestamps[0] < current_time - CONFIG["continuous_distracted_window"]:
            self.distracted_timestamps.popleft()
        
        return (len(self.distracted_timestamps) >= CONFIG["continuous_distracted_count"] and
                current_time - self.last_level3_time >= CONFIG["level3_cooldown"] and 
                self.progress_score >= 95)  # Level 3阈值保持不变
    
    def _create_continuous_distraction_event(self, current_time):
        """创建连续分心事件"""
        weighted_score = sum(
            BEHAVIOR_WEIGHTS.get(label, 0) * 
            (sum(c for t, c in data["detections"]) / len(data["detections"]) if data["detections"] else 0)
            for label, data in self.behavior_tracker.items()
        )
        
        event = {
            "time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "start_time": current_time,
            "behavior": "连续分心",
            "duration": round(CONFIG["continuous_distracted_window"], 2),
            "count": len(self.distracted_timestamps),
            "confidence": round(weighted_score, 2),
            "event_type": "Distracted",
            "level": "Level 3"
        }
        
        self.last_level3_time = current_time
        self.last_multi_event_time = current_time
        self.distracted_timestamps.clear()
        
        self._save_event_to_csv(event)
        return [event]
    
    def _save_event_to_csv(self, event):
        """保存事件到CSV文件"""
        try:
            event_copy = event.copy()
            event_copy["behavior"] = LABEL_MAP.get(event_copy["behavior"], event_copy["behavior"])
            df = pd.DataFrame([event_copy])
            file_exists = os.path.exists(self.event_log_file)
            file_is_empty = not file_exists or os.path.getsize(self.event_log_file) == 0
            df.to_csv(self.event_log_file, mode="a", header=file_is_empty, index=False, encoding="utf-8")
            logging.info(f"事件保存到 CSV: {event_copy}")
        except Exception as e:
            logging.error(f"保存事件到 CSV 失败: {e}")
    
    def clear_events(self):
        """清空事件记录"""
        self.events = []
        self.distracted_count = 0
        self.distracted_timestamps.clear()
        self.behavior_tracker.clear()
        self.progress_score = 0.0
        self.current_level = "Normal"
        
        if os.path.exists(self.event_log_file):
            try:
                os.remove(self.event_log_file)
                logging.info("事件日志文件已清空")
            except Exception as e:
                logging.error(f"清空事件日志文件失败: {e}")
        
        logging.info("事件记录和计数器已清空")
    
    def get_events(self):
        """获取事件列表"""
        return self.events
    
    def get_distracted_count(self):
        """获取分心次数"""
        return self.distracted_count
    
    def get_progress_score(self):
        """获取进度评分"""
        return self.progress_score
    
    def get_current_level(self):
        """获取当前等级"""
        return self.current_level
    
    def get_label_map(self):
        """获取标签映射"""
        return LABEL_MAP
    
    def get_fatigue_classes(self):
        """获取疲劳类别"""
        return FATIGUE_CLASSES
    
    def set_test_mode(self, enabled, score=0, behaviors=None):
        """设置测试模式"""
        self.is_test_mode = enabled
        if enabled:
            self.test_score = score
            self.test_behaviors = behaviors or []
            self.progress_score = score
            self.test_mode_start_time = time.time()
            self.last_test_behavior_time = time.time()
            self.test_behavior_duration = 0.0
            
            # 根据分数设置等级（同步更新阈值）
            if score >= 95:
                self.current_level = "Level 3"
            elif score >= 80:  # 从75提高到80，减缓等级上升
                self.current_level = "Level 2"
            elif score >= 60:  # 从50提高到60，减缓等级上升
                self.current_level = "Level 1"
            else:
                self.current_level = "Normal"
            logging.info(f"测试模式已启用: 分数={score}, 行为={behaviors}")
        else:
            self.test_score = 0.0
            self.test_behaviors = []
            self.test_mode_start_time = 0.0
            self.last_test_behavior_time = 0.0
            self.test_behavior_duration = 0.0
            logging.info("测试模式已禁用")
    
    def reset_test_mode(self):
        """重置测试模式"""
        self.is_test_mode = False
        self.test_score = 0.0
        self.test_behaviors = []
        self.progress_score = 0.0
        self.current_level = "Normal"
        self.clear_events()
        logging.info("测试模式已重置")
    
    
    def _handle_test_mode_logic(self, current_time):
        """处理测试模式逻辑，保持正常的分数回退和事件触发机制"""
        try:
            is_fatigue = False
            is_distracted = False
            new_events = []
            
            # 更新测试行为持续时间
            if self.test_behaviors and self.last_test_behavior_time > 0:
                self.test_behavior_duration = current_time - self.last_test_behavior_time
            
            # 检查是否有活跃的测试行为
            has_active_test_behavior = self._check_active_test_behavior(current_time)
            
            if has_active_test_behavior:
                # 有活跃测试行为时，保持或增加分数
                self._update_test_progress(current_time)
            else:
                # 没有活跃测试行为时，应用正常的分数回退逻辑
                self._apply_test_score_decay(current_time)
            
            # 确定当前等级（提高阈值，减缓等级上升速度）
            prev_level = self.current_level
            if self.progress_score >= 95:
                self.current_level = "Level 3"
            elif self.progress_score >= 80:  # 从75提高到80，减缓等级上升
                self.current_level = "Level 2"
            elif self.progress_score >= 60:  # 从50提高到60，减缓等级上升
                self.current_level = "Level 1"
            else:
                self.current_level = "Normal"
            
            # 触发警报（与正常模式相同的逻辑）
            self._trigger_alerts(prev_level, current_time)
            
            # 检查是否应该创建事件
            if has_active_test_behavior:
                new_events = self._create_test_behavior_events(current_time)
            
            # 确定疲劳和分心状态（同步更新阈值）
            is_fatigue = self.progress_score >= 80  # 从75提高到80
            is_distracted = self.progress_score >= 60 and self.progress_score < 80  # 从50提高到60
            
            logging.debug(f"[测试模式] 分数={self.progress_score:.1f}, 等级={self.current_level}, 疲劳={is_fatigue}, 分心={is_distracted}")
            
            return is_fatigue, is_distracted, new_events, self.distracted_count, self.progress_score, self.current_level
            
        except Exception as e:
            logging.error(f"[测试模式] 处理逻辑错误: {e}")
            return False, False, [], self.distracted_count, self.progress_score, self.current_level
    
    def _check_active_test_behavior(self, current_time):
        """检查是否有活跃的测试行为"""
        if not self.test_behaviors:
            return False
        
        # 如果测试行为持续时间超过阈值，认为行为已结束
        if self.test_behavior_duration > CONFIG["duration_threshold"]:
            return False
        
        # 如果距离上次测试行为时间太久，认为行为已结束
        if current_time - self.last_test_behavior_time > CONFIG["window_size"]:
            return False
        
        return True
    
    def _update_test_progress(self, current_time):
        """更新测试进度分数"""
        if not self.test_behaviors:
            return
        
        # 根据测试行为权重增加分数，但降低变化频率
        for behavior in self.test_behaviors:
            if behavior in BEHAVIOR_WEIGHTS:
                weight = BEHAVIOR_WEIGHTS[behavior]
                # 根据持续时间调整增量，但降低变化速度
                duration_factor = min(1.2, 1 + self.test_behavior_duration / 30.0)  # 降低变化速度
                progress_increment = CONFIG["progress_increment"] * weight * duration_factor * 0.3  # 降低增量
                self.progress_score = min(100.0, self.progress_score + progress_increment)
    
    def _apply_test_score_decay(self, current_time):
        """应用测试分数衰减"""
        # 应用正常的分数衰减逻辑，但降低衰减速度
        if self.progress_score > 0:
            # 检查是否长时间没有分心行为
            if current_time - self.last_test_behavior_time > CONFIG["safe_driving_confirm_time"]:
                # 长时间安全驾驶，快速衰减（但降低速度）
                self.progress_score = max(0.0, self.progress_score - CONFIG["progress_decrement_focused"] * 0.5)
            else:
                # 正常衰减（但降低速度）
                self.progress_score = max(0.0, self.progress_score - CONFIG["progress_decrement_normal"] * 0.3)
    
    def _create_test_behavior_events(self, current_time):
        """创建测试行为事件"""
        new_events = []
        
        if not self.test_behaviors:
            return new_events
        
        # 为每个测试行为创建事件
        for behavior in self.test_behaviors:
            if behavior in BEHAVIOR_WEIGHTS:
                event_type = "Fatigue" if behavior in FATIGUE_CLASSES else "Distracted"
                
                event = {
                    "time": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
                    "start_time": current_time,
                    "behavior": behavior,
                    "duration": round(self.test_behavior_duration, 2),
                    "count": 1,
                    "confidence": 0.85,  # 测试模式使用固定置信度
                    "event_type": event_type,
                    "level": self.current_level
                }
                
                new_events.append(event)
                self.distracted_count += 1
                self.distracted_timestamps.append(current_time)
                
                # 保存到CSV
                self._save_event_to_csv(event)
        
        return new_events
    
    def update_test_behavior(self, behaviors, current_time=None):
        """更新测试行为（用于模拟疲劳事件）"""
        if not self.is_test_mode:
            return
        
        if current_time is None:
            current_time = time.time()
        
        self.test_behaviors = behaviors
        self.last_test_behavior_time = current_time
        self.test_behavior_duration = 0.0
        
        logging.info(f"[测试模式] 更新测试行为: {behaviors}")
    
    def _trigger_network_report(self, current_time, level):
        """触发网络上报"""
        try:
            logging.info(f"开始触发网络上报: 等级={level}, 分数={self.progress_score}")
            
            if not self.network_manager:
                logging.warning("网络管理器未初始化，跳过网络上报")
                return
            
            # 获取准确的时间戳（整数）
            if hasattr(self.network_manager, 'module') and self.network_manager.module:
                try:
                    accurate_timestamp = self.network_manager.module.get_accurate_timestamp()
                except:
                    accurate_timestamp = int(current_time)
            else:
                accurate_timestamp = int(current_time)
            
            # 准备行为数据
            behavior_data = {
                "behavior": "fatigue_detection",
                "confidence": self.progress_score / 100.0,
                "progress_score": self.progress_score,
                "current_level": level,
                "distracted_count": self.distracted_count,
                "timestamp": accurate_timestamp
            }
            
            # 准备疲劳数据
            fatigue_data = {
                "fatigue_score": self.progress_score / 100.0,
                "eye_blink_rate": 0.45,  # 可以从实际检测中获取
                "head_movement_score": 0.32,
                "yawn_count": 2,
                "attention_score": 0.78,
                "timestamp": accurate_timestamp
            }
            
            logging.info(f"准备上报数据: 行为数据={behavior_data}, 疲劳数据={fatigue_data}")
            
            # 触发事件上报
            event_success = self.network_manager.trigger_event_report(behavior_data)
            logging.info(f"事件上报结果: {event_success}")
            
            # 触发数据上报
            data_success = self.network_manager.trigger_data_report(fatigue_data)
            logging.info(f"数据上报结果: {data_success}")
            
            logging.info("=============== 网络上报完成: 等级={}, 分数={:.1f}, 事件上报={}, 数据上报={}".format(level, self.progress_score, event_success, data_success))
            print("=============== 网络上报完成: 等级={}, 分数={:.1f}, 事件上报={}, 数据上报={}".format(level, self.progress_score, event_success, data_success))
            
        except Exception as e:
            logging.error(f"网络上报触发失败: {e}")
            import traceback
            logging.error(f"错误堆栈: {traceback.format_exc()}")