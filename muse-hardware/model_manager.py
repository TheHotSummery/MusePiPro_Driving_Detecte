# -*- coding: utf-8 -*-
"""
模型管理模块
处理YOLO模型的加载、推理和后处理
"""

import os
import logging
import numpy as np
import torch
import torchvision
import onnxruntime as ort
from config import (
    MODEL_TYPE, ONNX_MODEL_PATH, PT_MODEL_PATH, 
    MODEL_INPUT_SIZE, MODEL_CLASSES, NUM_CLASSES, 
    LABEL_MAP, FATIGUE_CLASSES, CONFIG
)

class ModelManager:
    """模型管理器类"""
    
    def __init__(self):
        self.session = None
        self.pt_model = None
        self.model_loaded = False
        self._load_model()
    
    def _load_model(self):
        """加载模型"""
        try:
            logging.info("开始加载模型...")
            if MODEL_TYPE == "onnx":
                self._load_onnx_model()
            else:
                self._load_pytorch_model()
            logging.info("模型加载完成")
        except Exception as e:
            logging.error(f"模型加载失败: {e}")
            self.model_loaded = False
    
    def _load_onnx_model(self):
        """加载ONNX模型"""
        if not os.path.exists(ONNX_MODEL_PATH):
            raise FileNotFoundError(f"ONNX模型文件 {ONNX_MODEL_PATH} 不存在")
        
        try:
            logging.info("尝试加载 SpaceMITExecutionProvider...")
            import spacemit_ort
            session_options = ort.SessionOptions()
            session_options.intra_op_num_threads = 4
            session_options.inter_op_num_threads = 2
            self.session = ort.InferenceSession(
                ONNX_MODEL_PATH, 
                sess_options=session_options, 
                providers=["SpaceMITExecutionProvider"]
            )
            self.model_loaded = True
            logging.info(f"ONNX模型加载成功：{ONNX_MODEL_PATH} (使用 SpaceMITExecutionProvider)")
        except Exception as e:
            logging.error(f"加载 SpaceMITExecutionProvider 失败: {e}")
            # 尝试使用CPU执行器作为备选方案
            try:
                logging.info("尝试使用CPU执行器作为备选方案...")
                session_options = ort.SessionOptions()
                session_options.intra_op_num_threads = 4
                session_options.inter_op_num_threads = 2
                self.session = ort.InferenceSession(
                    ONNX_MODEL_PATH, 
                    sess_options=session_options, 
                    providers=["CPUExecutionProvider"]
                )
                self.model_loaded = True
                logging.info(f"ONNX模型加载成功：{ONNX_MODEL_PATH} (使用 CPUExecutionProvider)")
            except Exception as e2:
                logging.error(f"CPU执行器也失败: {e2}")
                raise
    
    def _load_pytorch_model(self):
        """加载PyTorch模型"""
        try:
            from ultralytics import YOLO
            self.pt_model = YOLO(PT_MODEL_PATH)
            self.model_loaded = True
            logging.info(f"PyTorch模型加载成功：{PT_MODEL_PATH}")
        except Exception as e:
            logging.error(f"PyTorch模型加载失败: {e}")
            raise
    
    def predict(self, input_tensor):
        """执行模型推理"""
        if not self.model_loaded:
            raise RuntimeError("模型未加载")
        
        try:
            if MODEL_TYPE == "onnx":
                outputs = self.session.run(None, {self.session.get_inputs()[0].name: input_tensor})[0]
                return outputs
            else:
                results = self.pt_model.predict(
                    input_tensor, 
                    imgsz=MODEL_INPUT_SIZE[0], 
                    conf=CONFIG["min_confidence"], 
                    iou=CONFIG["iou_threshold"], 
                    verbose=False, 
                    device="cpu"
                )
                return results
        except Exception as e:
            logging.error(f"模型推理失败: {e}")
            raise
    
    def postprocess_onnx(self, outputs, conf_threshold, iou_threshold, num_classes, img_h, img_w):
        """后处理ONNX模型输出"""
        output_squeezed = outputs.squeeze()
        raw_output_transposed = np.transpose(output_squeezed, (1, 0))
        
        logging.debug(f"ONNX 输出形状: {raw_output_transposed.shape}, 数值范围: [{raw_output_transposed.min():.4f}, {raw_output_transposed.max():.4f}]")
        
        if raw_output_transposed.shape[1] != (4 + num_classes):
            logging.error(f"ONNX 输出维度不匹配: {raw_output_transposed.shape[1]} vs {4 + num_classes}")
            return []
        
        boxes_raw_all = raw_output_transposed[:, :4]
        class_scores_all = raw_output_transposed[:, 4:]
        
        # 坐标转换
        max_coord = boxes_raw_all.max()
        if max_coord <= 1.0:
            logging.debug("检测到归一化坐标，转换为像素坐标")
            boxes_raw_all = boxes_raw_all * np.array([MODEL_INPUT_SIZE[1], MODEL_INPUT_SIZE[0], MODEL_INPUT_SIZE[1], MODEL_INPUT_SIZE[0]])
        
        # 将模型输入尺寸的坐标缩放到原始图像尺寸
        scale_x = img_w / MODEL_INPUT_SIZE[1]
        scale_y = img_h / MODEL_INPUT_SIZE[0]
        boxes_raw_all[:, [0, 2]] *= scale_x  # x坐标
        boxes_raw_all[:, [1, 3]] *= scale_y  # y坐标
        
        boxes_raw_all = np.clip(boxes_raw_all, 0, max(img_w, img_h) - 1)
        
        # 置信度处理
        max_score = class_scores_all.max()
        if max_score > 1.0:
            logging.debug("检测到 logits，应用 sigmoid")
            class_scores_clipped = np.clip(class_scores_all, -50, 50)
            class_probabilities_all = 1 / (1 + np.exp(-class_scores_clipped))
        else:
            logging.debug("检测到概率值，直接使用")
            class_probabilities_all = class_scores_all
        
        if np.any(np.isnan(class_probabilities_all)) or np.any(np.isinf(class_probabilities_all)):
            logging.warning("检测到NaN或Inf值，使用备用方案")
            class_probabilities_all = np.nan_to_num(class_probabilities_all, nan=0.0, posinf=1.0, neginf=0.0)
        
        # 专注驾驶置信度提升和优化
        focused_index = MODEL_CLASSES.index("focused")
        max_other_conf = np.max(class_probabilities_all[:, [i for i in range(num_classes) if i != focused_index]], axis=1)
        focused_conf = class_probabilities_all[:, focused_index]
        conf_diff = max_other_conf - focused_conf
        
        # 优化专注驾驶检测：当专注驾驶置信度较高且与其他行为差距不大时，提升专注驾驶
        # 放宽条件：提高置信度差距阈值（从0.15提高到0.20），更容易触发提升
        boost_mask = (focused_conf > CONFIG["focused_min_confidence"]) & (conf_diff < 0.20)
        class_probabilities_all[boost_mask, focused_index] += CONFIG["focused_confidence_boost"]
        
        # 降低左右看的置信度，避免挤占专注驾驶
        seeing_left_index = MODEL_CLASSES.index("seeing_left")
        seeing_right_index = MODEL_CLASSES.index("seeing_right")
        
        # 当专注驾驶置信度较高时，降低左右看的置信度（降低更多，从0.8改为0.7）
        focused_high_mask = focused_conf > CONFIG["focused_min_confidence"]
        class_probabilities_all[focused_high_mask, seeing_left_index] *= 0.7  # 从0.8降低到0.7
        class_probabilities_all[focused_high_mask, seeing_right_index] *= 0.7  # 从0.8降低到0.7
        
        class_probabilities_all = np.clip(class_probabilities_all, 0.0, 1.0)
        
        # 获取最终检测结果
        final_confidences_all = np.max(class_probabilities_all, axis=1)
        class_ids_all = np.argmax(class_probabilities_all, axis=1)
        
        # 对专注驾驶使用更低的置信度阈值，提高识别灵敏度
        focused_indices = np.where(class_ids_all == focused_index)[0]
        other_indices = np.where(class_ids_all != focused_index)[0]
        
        # 专注驾驶使用更低的阈值（focused_min_confidence），其他行为使用正常阈值
        candidate_indices = []
        if len(focused_indices) > 0:
            focused_candidates = focused_indices[final_confidences_all[focused_indices] > CONFIG["focused_min_confidence"]]
            candidate_indices.extend(focused_candidates)
        if len(other_indices) > 0:
            other_candidates = other_indices[final_confidences_all[other_indices] > conf_threshold]
            candidate_indices.extend(other_candidates)
        
        candidate_indices = np.array(candidate_indices, dtype=np.int32)
        if len(candidate_indices) == 0:
            return []
        
        filtered_boxes_raw = boxes_raw_all[candidate_indices]
        filtered_confidences = final_confidences_all[candidate_indices]
        filtered_class_ids = class_ids_all[candidate_indices]
        
        # 转换为xyxy格式
        x1 = filtered_boxes_raw[:, 0] - filtered_boxes_raw[:, 2] / 2
        y1 = filtered_boxes_raw[:, 1] - filtered_boxes_raw[:, 3] / 2
        x2 = filtered_boxes_raw[:, 0] + filtered_boxes_raw[:, 2] / 2
        y2 = filtered_boxes_raw[:, 1] + filtered_boxes_raw[:, 3] / 2
        
        x1 = np.clip(x1, 0, img_w - 1)
        y1 = np.clip(y1, 0, img_h - 1)
        x2 = np.clip(x2, 0, img_w - 1)
        y2 = np.clip(y2, 0, img_h - 1)
        
        raw_boxes_xyxy = np.stack([x1, y1, x2, y2], axis=1)
        
        # 应用NMS
        boxes_tensor = torch.tensor(raw_boxes_xyxy, dtype=torch.float32)
        scores_tensor = torch.tensor(filtered_confidences, dtype=torch.float32)
        class_ids_tensor = torch.tensor(filtered_class_ids, dtype=torch.int64)
        
        final_detections = []
        unique_classes = torch.unique(class_ids_tensor)
        
        for cls_id in unique_classes:
            cls_mask = (class_ids_tensor == cls_id)
            cls_indices = torch.nonzero(cls_mask).squeeze(1)
            
            if cls_indices.numel() == 0:
                continue
                
            cls_boxes = boxes_tensor[cls_indices]
            cls_scores = scores_tensor[cls_indices]
            
            if cls_boxes.numel() > 0:
                keep_indices = torchvision.ops.nms(cls_boxes, cls_scores, iou_threshold)
                kept_filtered_indices = cls_indices[keep_indices]
                
                for kept_filtered_idx in kept_filtered_indices:
                    box_kept_xyxy = boxes_tensor[kept_filtered_idx].cpu().numpy()
                    confidence = scores_tensor[kept_filtered_idx].item()
                    class_id = class_ids_tensor[kept_filtered_idx].item()
                    
                    box_width = box_kept_xyxy[2] - box_kept_xyxy[0]
                    box_height = box_kept_xyxy[3] - box_kept_xyxy[1]
                    
                    if box_width > 5 and box_height > 5:
                        label = MODEL_CLASSES[class_id]
                        final_detections.append({
                            "label": label,
                            "label_cn": LABEL_MAP.get(label, label),
                            "confidence": round(float(confidence), 2),
                            "bbox": [int(box_kept_xyxy[0]), int(box_kept_xyxy[1]), 
                                     int(box_kept_xyxy[2]), int(box_kept_xyxy[3])],
                            "raw_bbox": [float(box_kept_xyxy[0]), float(box_kept_xyxy[1]), 
                                         float(box_kept_xyxy[2]), float(box_kept_xyxy[3])]
                        })
        
        return final_detections
    
    def postprocess_pytorch(self, results, conf_threshold, iou_threshold, num_classes, img_h, img_w):
        """后处理PyTorch模型输出"""
        detections = []
        
        for r in results:
            if r.boxes:
                boxes_xyxy = r.boxes.xyxy.cpu().numpy()
                scores = r.boxes.conf.cpu().numpy()
                classes_raw = r.boxes.cls.cpu().numpy()
                
                for i in range(len(boxes_xyxy)):
                    score = scores[i]
                    cls_id = int(classes_raw[i])
                    
                    if score < conf_threshold or cls_id >= num_classes:
                        continue
                    
                    x1, y1, x2, y2 = boxes_xyxy[i]
                    x1 = int(max(0, min(x1, MODEL_INPUT_SIZE[1] - 1)))
                    y1 = int(max(0, min(y1, MODEL_INPUT_SIZE[0] - 1)))
                    x2 = int(max(0, min(x2, MODEL_INPUT_SIZE[1] - 1)))
                    y2 = int(max(0, min(y2, MODEL_INPUT_SIZE[0] - 1)))
                    
                    if x2 <= x1 or y2 <= y1:
                        continue
                    
                    box_width = x2 - x1
                    box_height = y2 - y1
                    
                    if box_width > 5 and box_height > 5:
                        label = MODEL_CLASSES[cls_id]
                        detections.append({
                            "label": label,
                            "label_cn": LABEL_MAP.get(label, label),
                            "confidence": round(float(score), 2),
                            "bbox": [x1, y1, x2, y2],
                            "raw_bbox": [float(boxes_xyxy[i][0]), float(boxes_xyxy[i][1]), 
                                         float(boxes_xyxy[i][2]), float(boxes_xyxy[i][3])]
                        })
        
        return detections
    
    def is_loaded(self):
        """检查模型是否已加载"""
        return self.model_loaded
    
    def get_model_type(self):
        """获取模型类型"""
        return MODEL_TYPE
