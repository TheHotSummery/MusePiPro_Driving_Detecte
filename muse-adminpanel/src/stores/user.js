import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  // 状态
  const users = ref([])
  const selectedUser = ref(null)
  const loading = ref(false)
  const error = ref(null)

  // 计算属性
  const userCount = computed(() => users.value.length)
  
  const activeUserCount = computed(() => 
    users.value.filter(user => user.status === 'ACTIVE').length
  )
  
  const inactiveUserCount = computed(() => 
    users.value.filter(user => user.status === 'INACTIVE').length
  )

  // 方法
  const fetchUsers = async (params = {}) => {
    loading.value = true
    error.value = null
    console.log('=== 用户Store: 开始获取用户列表 ===')
    console.log('请求参数:', params)
    
    try {
      const response = await userApi.getUsers(params)
      console.log('用户API响应:', response)
      console.log('响应中的用户数组:', response.users)
      console.log('用户数组长度:', response.users?.length)
      
      users.value = response.users || []
      console.log('设置用户列表:', users.value.length, '个用户')
      console.log('最终用户列表:', users.value)
      
      return response
    } catch (err) {
      console.error('获取用户列表失败:', err)
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const fetchUserById = async (userId) => {
    loading.value = true
    error.value = null
    console.log('=== 用户Store: 开始获取用户详情 ===')
    console.log('用户ID:', userId)
    
    try {
      const response = await userApi.getUserById(userId)
      console.log('用户详情API响应:', response)
      selectedUser.value = response
      return response
    } catch (err) {
      console.error('获取用户详情失败:', err)
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const createUser = async (userData) => {
    loading.value = true
    error.value = null
    console.log('=== 用户Store: 开始创建用户 ===')
    console.log('用户数据:', userData)
    
    try {
      const response = await userApi.createUser(userData)
      console.log('创建用户API响应:', response)
      // 创建成功后重新获取用户列表
      await fetchUsers()
      return response
    } catch (err) {
      console.error('创建用户失败:', err)
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const updateUser = async (userId, userData) => {
    loading.value = true
    error.value = null
    console.log('=== 用户Store: 开始更新用户 ===')
    console.log('用户ID:', userId)
    console.log('更新数据:', userData)
    
    try {
      const response = await userApi.updateUser(userId, userData)
      console.log('更新用户API响应:', response)
      // 更新成功后重新获取用户列表
      await fetchUsers()
      return response
    } catch (err) {
      console.error('更新用户失败:', err)
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const deleteUser = async (userId) => {
    loading.value = true
    error.value = null
    console.log('=== 用户Store: 开始删除用户 ===')
    console.log('用户ID:', userId)
    
    try {
      const response = await userApi.deleteUser(userId)
      console.log('删除用户API响应:', response)
      // 删除成功后重新获取用户列表
      await fetchUsers()
      return response
    } catch (err) {
      console.error('删除用户失败:', err)
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const clearError = () => {
    error.value = null
  }

  return {
    // 状态
    users,
    selectedUser,
    loading,
    error,
    
    // 计算属性
    userCount,
    activeUserCount,
    inactiveUserCount,
    
    // 方法
    fetchUsers,
    fetchUserById,
    createUser,
    updateUser,
    deleteUser,
    clearError
  }
})
