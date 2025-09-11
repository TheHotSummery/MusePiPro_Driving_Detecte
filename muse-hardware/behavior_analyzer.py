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
    
    def __init__(self, gpio_controller, socketio):
        self.gpio_controller = gpio_controller
        self.socketio = socketio
        
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
    
    def check_fatigue_or_distracted(self, detections, current_time):
        """检查疲劳或分心状态"""
        try:
            logging.debug(f"[BehaviorAnalyzer] 开始检查疲劳/分心状态，检测数量: {len(detections) if detections else 0}")
            is_fatigue = False
            is_distracted = False
            new_events = []
            
            # 存储当前检测结果用于清醒驾驶检测
            self._current_detections = detections
            
            # 确定当前等级
            prev_level = self.current_level
            if self.progress_score >= 95:
                self.current_level = "Level 3"
            elif self.progress_score >= 75:
                self.current_level = "Level 2"
            elif self.progress_score >= 50:
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
        all_focused = True
        for detection in self._get_current_detections():
            if detection["label"] != "focused" and detection["confidence"] >= CONFIG["min_confidence"]:
                all_focused = False
                break
        
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
            
            # 调整累加逻辑：每秒累加约5分（10秒50，15秒75，20秒95）
            duration = current_time - (self.behavior_tracker[label]["detections"][0][0] if self.behavior_tracker[label]["detections"] else current_time)
            progress_increment = CONFIG["progress_increment"] * BEHAVIOR_WEIGHTS.get(label, 0) * min(1.5, 1 + duration / 20.0)
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
        
        if (self.progress_score >= 95 and prev_level != "Level 3" and 
            current_time - self.last_level3_time >= CONFIG["level3_cooldown"]):
            threading.Thread(target=self.gpio_controller.trigger_level3_alert, daemon=True).start()
            self.socketio.emit("level_update", {"level": "Level 3", "progress": self.progress_score})
            logging.info("触发三级警报")
        elif self.progress_score >= 75 and prev_level != "Level 2":
            threading.Thread(target=self.gpio_controller.trigger_level2_alert, daemon=True).start()
            self.socketio.emit("level_update", {"level": "Level 2", "progress": self.progress_score})
            logging.info("触发二级警报")
        elif self.progress_score >= 50 and prev_level != "Level 1":
            threading.Thread(target=self.gpio_controller.trigger_level1_alert, daemon=True).start()
            self.socketio.emit("level_update", {"level": "Level 1", "progress": self.progress_score})
            logging.info("触发一级警报")
    
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
        level = "Level 1" if self.progress_score < 75 else "Level 2"
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
               self.progress_score >= 95
    
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
                self.progress_score >= 75)
    
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
                self.progress_score >= 95)
    
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