import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useConfigStore = defineStore('config', () => {
  // 配置参数
  const config = ref({
    duration_threshold: 1.5,
    fatigue_duration_threshold: 2.0,
    min_detections_for_duration: 2,
    count_threshold: 3,
    window_size: 30,
    score_threshold: 0.8,
    min_confidence: 0.8,
    fatigue_min_confidence: 0.85,
    multi_event_cooldown: 10,
    level3_cooldown: 30,
    level_reset_threshold: 300,
    safe_driving_confirm_time: 3,
    fps_target: 4.5
  })

  // 权重配置
  const weights = ref({
    eyes_closed: 0.8,
    yarning: 0.7,
    eyes_closed_head_left: 0.6,
    eyes_closed_head_right: 0.6,
    head_down: 0.5,
    seeing_left: 0.4,
    seeing_right: 0.4,
    head_up: 0.3
  })

  // 默认权重（用于重置）
  const defaultWeights = {
    eyes_closed: 0.8,
    yarning: 0.7,
    eyes_closed_head_left: 0.6,
    eyes_closed_head_right: 0.6,
    head_down: 0.5,
    seeing_left: 0.4,
    seeing_right: 0.4,
    head_up: 0.3
  }

  // 方法
  const updateConfig = (newConfig) => {
    config.value = { ...config.value, ...newConfig }
  }

  const updateWeights = (newWeights) => {
    weights.value = { ...weights.value, ...newWeights }
  }

  const resetWeights = () => {
    weights.value = { ...defaultWeights }
  }

  const getConfigForSocket = () => {
    return {
      duration_threshold: config.value.duration_threshold,
      fatigue_duration_threshold: config.value.fatigue_duration_threshold,
      min_detections_for_duration: config.value.min_detections_for_duration,
      count_threshold: config.value.count_threshold,
      window_size: config.value.window_size,
      score_threshold: config.value.score_threshold,
      min_confidence: config.value.min_confidence,
      fatigue_min_confidence: config.value.fatigue_min_confidence,
      multi_event_cooldown: config.value.multi_event_cooldown,
      level3_cooldown: config.value.level3_cooldown,
      level_reset_threshold: config.value.level_reset_threshold,
      safe_driving_confirm_time: config.value.safe_driving_confirm_time,
      fps_target: config.value.fps_target
    }
  }

  const getWeightsForSocket = () => {
    return {
      eyes_closed: weights.value.eyes_closed,
      yarning: weights.value.yarning,
      eyes_closed_head_left: weights.value.eyes_closed_head_left,
      eyes_closed_head_right: weights.value.eyes_closed_head_right,
      head_down: weights.value.head_down,
      seeing_left: weights.value.seeing_left,
      seeing_right: weights.value.seeing_right,
      head_up: weights.value.head_up
    }
  }

  return {
    // 状态
    config,
    weights,
    defaultWeights,
    // 方法
    updateConfig,
    updateWeights,
    resetWeights,
    getConfigForSocket,
    getWeightsForSocket
  }
})




