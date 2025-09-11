import request from './index'

// 用户管理相关API
export const userApi = {
  // 获取用户列表
  getUsers(params = {}) {
    console.log('=== API调用: 获取用户列表 ===')
    console.log('请求参数:', params)
    return request.get('/api/v1/users', { params })
  },

  // 获取用户详情
  getUserById(userId) {
    console.log('=== API调用: 获取用户详情 ===')
    console.log('用户ID:', userId)
    return request.get(`/api/v1/users/${userId}`)
  },

  // 创建用户
  createUser(userData) {
    console.log('=== API调用: 创建用户 ===')
    console.log('用户数据:', userData)
    return request.post('/api/v1/users', userData)
  },

  // 更新用户
  updateUser(userId, userData) {
    console.log('=== API调用: 更新用户 ===')
    console.log('用户ID:', userId)
    console.log('更新数据:', userData)
    return request.put(`/api/v1/users/${userId}`, userData)
  },

  // 删除用户
  deleteUser(userId) {
    console.log('=== API调用: 删除用户 ===')
    console.log('用户ID:', userId)
    return request.post(`/api/v1/users/${userId}/delete`)
  }
}
