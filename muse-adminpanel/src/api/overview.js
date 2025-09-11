import request from './index'

// 系统概览相关API
export const overviewApi = {
  // 获取系统概览
  getOverview() {
    return request.get('/api/v1/platform/overview')
  }
}

