package com.spacemit.musebackend.service;

import com.spacemit.musebackend.entity.GpsData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class FatigueAnalysisService {

    /**
     * 分析疲劳等级
     */
    public GpsData.FatigueLevel analyzeFatigueLevel(BigDecimal fatigueScore, BigDecimal eyeBlinkRate, 
                                                   BigDecimal headMovementScore, Integer yawnCount, 
                                                   BigDecimal attentionScore) {
        if (fatigueScore == null) fatigueScore = BigDecimal.ZERO;
        if (eyeBlinkRate == null) eyeBlinkRate = BigDecimal.ZERO;
        if (headMovementScore == null) headMovementScore = BigDecimal.ZERO;
        if (yawnCount == null) yawnCount = 0;
        if (attentionScore == null) attentionScore = BigDecimal.ONE;

        double fatigue = fatigueScore.doubleValue();
        double blinkRate = eyeBlinkRate.doubleValue();
        double headMovement = headMovementScore.doubleValue();
        int yawns = yawnCount;
        double attention = attentionScore.doubleValue();

        // 疲劳评分权重计算
        double totalScore = 0;
        int factors = 0;

        // 疲劳评分 (0-1, 越高越疲劳)
        if (fatigue > 0) {
            totalScore += fatigue * 0.4;
            factors++;
        }

        // 眨眼频率 (正常约0.5-0.6, 疲劳时降低)
        if (blinkRate > 0) {
            double blinkScore = Math.max(0, 0.6 - blinkRate) / 0.6; // 眨眼频率越低，疲劳分数越高
            totalScore += blinkScore * 0.2;
            factors++;
        }

        // 头部运动 (疲劳时头部运动增加)
        if (headMovement > 0) {
            double headScore = Math.min(1.0, headMovement / 0.5); // 头部运动超过0.5认为异常
            totalScore += headScore * 0.2;
            factors++;
        }

        // 打哈欠次数 (疲劳时增加)
        if (yawns > 0) {
            double yawnScore = Math.min(1.0, yawns / 3.0); // 3次以上认为严重疲劳
            totalScore += yawnScore * 0.15;
            factors++;
        }

        // 注意力评分 (0-1, 越低越疲劳)
        if (attention < 1) {
            double attentionScoreValue = 1.0 - attention;
            totalScore += attentionScoreValue * 0.05;
            factors++;
        }

        if (factors > 0) {
            totalScore = totalScore / factors;
        }

        // 根据总分确定疲劳等级
        if (totalScore >= 0.8) {
            return GpsData.FatigueLevel.SEVERE;
        } else if (totalScore >= 0.6) {
            return GpsData.FatigueLevel.MODERATE;
        } else if (totalScore >= 0.3) {
            return GpsData.FatigueLevel.MILD;
        } else {
            return GpsData.FatigueLevel.NORMAL;
        }
    }

    /**
     * 判断是否需要触发疲劳事件
     */
    public boolean shouldTriggerFatigueEvent(GpsData.FatigueLevel fatigueLevel, BigDecimal speedKmh) {
        if (fatigueLevel == null) return false;
        if (speedKmh == null) speedKmh = BigDecimal.ZERO;

        // 只有在行驶状态下才触发疲劳事件
        double speed = speedKmh.doubleValue();
        if (speed < 5.0) return false; // 静止或低速时不触发

        // 根据疲劳等级和速度判断
        switch (fatigueLevel) {
            case SEVERE:
                return speed >= 10.0; // 严重疲劳时，速度超过10km/h就触发
            case MODERATE:
                return speed >= 30.0; // 中度疲劳时，速度超过30km/h才触发
            case MILD:
                return speed >= 60.0; // 轻度疲劳时，速度超过60km/h才触发
            default:
                return false;
        }
    }

    /**
     * 获取疲劳等级描述
     */
    public String getFatigueLevelDescription(GpsData.FatigueLevel fatigueLevel) {
        if (fatigueLevel == null) return "未知";
        
        switch (fatigueLevel) {
            case NORMAL:
                return "正常状态";
            case MILD:
                return "轻度疲劳";
            case MODERATE:
                return "中度疲劳";
            case SEVERE:
                return "严重疲劳";
            default:
                return "未知状态";
        }
    }

    /**
     * 获取疲劳建议
     */
    public String getFatigueAdvice(GpsData.FatigueLevel fatigueLevel) {
        if (fatigueLevel == null) return "请保持正常驾驶";
        
        switch (fatigueLevel) {
            case NORMAL:
                return "保持良好驾驶状态";
            case MILD:
                return "建议适当休息，保持注意力集中";
            case MODERATE:
                return "建议停车休息15-20分钟，避免疲劳驾驶";
            case SEVERE:
                return "立即停车休息，避免危险驾驶";
            default:
                return "请保持正常驾驶";
        }
    }
}
