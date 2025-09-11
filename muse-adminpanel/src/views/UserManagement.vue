<template>
  <div class="user-management">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>用户管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="showAddUserDialog">
          <el-icon><Plus /></el-icon>
          添加用户
        </el-button>
        <el-button @click="refreshUsers">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 用户列表 -->
    <el-row :gutter="20">
      <!-- 左侧用户列表 -->
      <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="16">
        <el-card class="user-list-section" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>用户列表</span>
              <div class="header-actions">
                <el-input
                  v-model="searchKeyword"
                  placeholder="搜索用户名或手机号"
                  size="small"
                  style="width: 200px; margin-right: 10px;"
                  clearable
                  @keyup.enter="handleSearch"
                  @clear="handleSearch"
                >
                  <template #prefix>
                    <el-icon><Search /></el-icon>
                  </template>
                </el-input>
                <el-button size="small" @click="handleSearch">
                  <el-icon><Search /></el-icon>
                  搜索
                </el-button>
              </div>
            </div>
          </template>
          
          <el-table
            :data="filteredUsers"
            v-loading="userStore.loading"
            stripe
            style="width: 100%"
            @row-click="handleRowClick"
          >
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="用户名" width="120" />
            <el-table-column prop="email" label="邮箱" width="200" />
            <el-table-column prop="phone" label="手机号" width="150" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
                  {{ row.status === 'ACTIVE' ? '活跃' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="deviceCount" label="关联设备" width="100">
              <template #default="{ row }">
                {{ getUserDeviceCount(row.id) }}
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click.stop="editUser(row)">
                  编辑
                </el-button>
                <el-button 
                  size="small" 
                  :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
                  @click.stop="toggleUserStatus(row)"
                >
                  {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
                </el-button>
                <el-button size="small" type="danger" @click.stop="deleteUser(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <!-- 分页 -->
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="pagination.currentPage"
              v-model:page-size="pagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="pagination.total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </el-card>
      </el-col>

      <!-- 右侧用户详情 -->
      <el-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8">
        <el-card class="user-detail-section" shadow="hover">
          <template #header>
            <span>用户详情</span>
          </template>
          
          <div v-if="selectedUser" class="user-detail">
            <div class="user-info">
              <div class="user-avatar">
                <el-avatar :size="80" :src="selectedUser.avatar">
                  {{ selectedUser.username?.charAt(0) }}
                </el-avatar>
              </div>
              <div class="user-basic">
                <h3>{{ selectedUser.username }}</h3>
                <p>{{ selectedUser.email }}</p>
                <p>{{ selectedUser.phone }}</p>
                <el-tag :type="selectedUser.status === 'ACTIVE' ? 'success' : 'danger'">
                  {{ selectedUser.status === 'ACTIVE' ? '活跃' : '禁用' }}
                </el-tag>
              </div>
            </div>
            
            <el-divider />
            
            <div class="user-stats">
              <div class="stat-item">
                <label>关联设备:</label>
                <span>{{ getUserDeviceCount(selectedUser.id) }}台</span>
              </div>
              <div class="stat-item">
                <label>创建时间:</label>
                <span>{{ formatTime(selectedUser.createdAt) }}</span>
              </div>
              <div class="stat-item">
                <label>最后登录:</label>
                <span>{{ formatTime(selectedUser.lastLoginAt) }}</span>
              </div>
            </div>
            
            <el-divider />
            
            <div class="user-devices">
              <h4>关联设备</h4>
              <div class="device-list">
                <div 
                  v-for="device in getUserDevices(selectedUser.id)" 
                  :key="device.deviceId"
                  class="device-item"
                >
                  <div class="device-info">
                    <div class="device-id">{{ device.deviceId }}</div>
                    <DeviceStatusTag :status="device.status" />
                  </div>
                  <div class="device-actions">
                    <el-button size="small" @click="unbindDevice(device)">
                      解绑
                    </el-button>
                  </div>
                </div>
                <el-button 
                  v-if="getUserDevices(selectedUser.id).length === 0"
                  size="small" 
                  type="primary" 
                  @click="bindDevice(selectedUser)"
                >
                  绑定设备
                </el-button>
              </div>
            </div>
          </div>
          
          <el-empty v-else description="请选择用户查看详情" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 添加/编辑用户对话框 -->
    <el-dialog
      v-model="userDialogVisible"
      :title="isEdit ? '编辑用户' : '添加用户'"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="userForm" :rules="userRules" ref="userFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="userForm.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="userForm.status" placeholder="选择状态">
            <el-option label="活跃" value="ACTIVE" />
            <el-option label="禁用" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUser">确定</el-button>
      </template>
    </el-dialog>

    <!-- 绑定设备对话框 -->
    <el-dialog
      v-model="bindDeviceDialogVisible"
      title="绑定设备"
      width="400px"
    >
      <el-form :model="bindForm" label-width="80px">
        <el-form-item label="选择设备">
          <el-select v-model="bindForm.deviceId" placeholder="选择要绑定的设备">
            <el-option
              v-for="device in availableDevices"
              :key="device.deviceId"
              :label="`${device.deviceId} (${device.deviceType})`"
              :value="device.deviceId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bindDeviceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBindDevice">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

import DeviceStatusTag from '@/components/common/DeviceStatusTag.vue'
import { useDeviceStore } from '@/stores/device'
import { useUserStore } from '@/stores/user'

const deviceStore = useDeviceStore()
const userStore = useUserStore()

// 响应式数据
const selectedUser = ref(null)
const searchKeyword = ref('')
const userDialogVisible = ref(false)
const bindDeviceDialogVisible = ref(false)
const isEdit = ref(false)

const pagination = ref({
  currentPage: 1,
  pageSize: 20,
  total: 0
})

const userForm = ref({
  id: null,
  username: '',
  email: '',
  phone: '',
  password: '',
  status: 'ACTIVE'
})

const bindForm = ref({
  userId: null,
  deviceId: ''
})

// 表单验证规则
const userRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

// 计算属性
const filteredUsers = computed(() => {
  if (!searchKeyword.value) {
    return userStore.users
  }
  
  const keyword = searchKeyword.value.toLowerCase()
  return userStore.users.filter(user => 
    user.username.toLowerCase().includes(keyword) ||
    (user.email && user.email.toLowerCase().includes(keyword)) ||
    (user.phone && user.phone.includes(keyword))
  )
})

const availableDevices = computed(() => {
  // 获取所有设备，过滤掉已绑定用户的设备
  return deviceStore.devices.filter(device => !device.user || !device.user.id)
})

// 方法
const loadUsers = async () => {
  try {
    console.log('=== UserManagement: 开始加载用户列表 ===')
    const params = {
      page: pagination.value.currentPage,
      size: pagination.value.pageSize,
      keyword: searchKeyword.value || undefined
    }
    
    console.log('加载参数:', params)
    const response = await userStore.fetchUsers(params)
    console.log('=== UserManagement: 用户列表加载完成 ===')
    console.log('响应数据:', response)
    console.log('当前用户数量:', userStore.users.length)
    console.log('用户列表:', userStore.users)
    
    pagination.value.total = response.total || 0
  } catch (error) {
    console.error('加载用户列表失败:', error)
    ElMessage.error('加载用户列表失败')
  }
}

const loadDevices = async () => {
  try {
    await deviceStore.fetchDevices()
  } catch (error) {
    console.error('加载设备列表失败:', error)
  }
}

const refreshUsers = () => {
  loadUsers()
}


const handleRowClick = async (row) => {
  try {
    selectedUser.value = row
    // 获取用户详情（包含设备信息）
    await userStore.fetchUserById(row.id)
    selectedUser.value = userStore.selectedUser
  } catch (error) {
    console.error('获取用户详情失败:', error)
    ElMessage.error('获取用户详情失败')
  }
}

const showAddUserDialog = () => {
  isEdit.value = false
  userForm.value = {
    id: null,
    username: '',
    email: '',
    phone: '',
    password: '',
    status: 'ACTIVE'
  }
  userDialogVisible.value = true
}

const editUser = (user) => {
  isEdit.value = true
  userForm.value = { ...user }
  userDialogVisible.value = true
}

const submitUser = async () => {
  try {
    if (isEdit.value) {
      // 更新用户
      await userStore.updateUser(userForm.value.id, userForm.value)
      ElMessage.success('用户更新成功')
    } else {
      // 添加用户
      await userStore.createUser(userForm.value)
      ElMessage.success('用户添加成功')
    }
    
    userDialogVisible.value = false
  } catch (error) {
    console.error('保存用户失败:', error)
    ElMessage.error('保存用户失败')
  }
}

const toggleUserStatus = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确认${user.status === 'ACTIVE' ? '禁用' : '启用'}用户 ${user.username}？`,
      '确认操作',
      { type: 'warning' }
    )
    
    const newStatus = user.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    await userStore.updateUser(user.id, { ...user, status: newStatus })
    ElMessage.success(`用户已${newStatus === 'ACTIVE' ? '启用' : '禁用'}`)
  } catch (error) {
    if (error !== 'cancel') {
      console.error('更新用户状态失败:', error)
      ElMessage.error('更新用户状态失败')
    }
  }
}

const deleteUser = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确认删除用户 ${user.username}？此操作不可恢复。`,
      '确认删除',
      { type: 'warning' }
    )
    
    await userStore.deleteUser(user.id)
    ElMessage.success('用户删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除用户失败:', error)
      ElMessage.error('删除用户失败')
    }
  }
}

const getUserDeviceCount = (userId) => {
  // 如果当前选中的用户有详情信息，使用详情中的设备数量
  if (selectedUser.value && selectedUser.value.id === userId && selectedUser.value.deviceCount !== undefined) {
    return selectedUser.value.deviceCount
  }
  // 否则从设备store中计算
  return deviceStore.devices.filter(device => device.userId === userId).length
}

const getUserDevices = (userId) => {
  // 如果当前选中的用户有详情信息，使用详情中的设备信息
  if (selectedUser.value && selectedUser.value.id === userId && selectedUser.value.devices) {
    return selectedUser.value.devices
  }
  // 否则从设备store中查找
  return deviceStore.devices.filter(device => device.userId === userId)
}

const bindDevice = (user) => {
  bindForm.value.userId = user.id
  bindForm.value.deviceId = ''
  bindDeviceDialogVisible.value = true
}

const unbindDevice = async (device) => {
  try {
    await ElMessageBox.confirm(
      `确认解绑设备 ${device.deviceId}？`,
      '确认解绑',
      { type: 'warning' }
    )
    
    // 调用设备API解绑用户
    await deviceStore.updateDevice(device.deviceId, { userId: null })
    ElMessage.success('设备解绑成功')
    
    // 重新加载用户详情和设备列表
    if (selectedUser.value) {
      await userStore.fetchUserById(selectedUser.value.id)
      selectedUser.value = userStore.selectedUser
    }
    await deviceStore.fetchDevices()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('解绑设备失败:', error)
      ElMessage.error('解绑设备失败')
    }
  }
}

const submitBindDevice = async () => {
  if (!bindForm.value.deviceId) {
    ElMessage.warning('请选择设备')
    return
  }
  
  try {
    // 调用设备API绑定用户
    await deviceStore.updateDevice(bindForm.value.deviceId, { userId: bindForm.value.userId })
    ElMessage.success('设备绑定成功')
    bindDeviceDialogVisible.value = false
    
    // 重新加载用户详情和设备列表
    if (selectedUser.value) {
      await userStore.fetchUserById(selectedUser.value.id)
      selectedUser.value = userStore.selectedUser
    }
    await deviceStore.fetchDevices()
  } catch (error) {
    console.error('绑定设备失败:', error)
    ElMessage.error('绑定设备失败')
  }
}

const handleSizeChange = (size) => {
  pagination.value.pageSize = size
  pagination.value.currentPage = 1
}

const handleCurrentChange = (page) => {
  pagination.value.currentPage = page
}

const formatTime = (time) => {
  if (!time) return '未知'
  return new Date(time).toLocaleString()
}

// 搜索功能
const handleSearch = () => {
  pagination.value.currentPage = 1
  loadUsers()
}

// 生命周期
onMounted(() => {
  loadUsers()
  loadDevices()
})
</script>

<style scoped lang="scss">
.user-management {
  padding: 20px;
  background: #f5f7fa;
  height: calc(100vh - 60px);
  overflow-y: auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h2 {
    margin: 0;
    color: #303133;
  }
}

.user-list-section,
.user-detail-section {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.user-detail {
  .user-info {
    display: flex;
    gap: 16px;
    margin-bottom: 16px;
  }

  .user-avatar {
    flex-shrink: 0;
  }

  .user-basic {
    flex: 1;

    h3 {
      margin: 0 0 8px 0;
      color: #303133;
    }

    p {
      margin: 4px 0;
      color: #606266;
      font-size: 14px;
    }
  }

  .user-stats {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .stat-item {
    display: flex;
    justify-content: space-between;
    align-items: center;

    label {
      color: #606266;
      font-size: 14px;
    }

    span {
      color: #303133;
      font-weight: 500;
    }
  }

  .user-devices {
    h4 {
      margin: 0 0 12px 0;
      color: #303133;
    }
  }

  .device-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .device-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    background: #f8f9fa;
    border-radius: 4px;
  }

  .device-info {
    flex: 1;

    .device-id {
      font-weight: 500;
      color: #303133;
      font-size: 14px;
      margin-bottom: 4px;
    }
  }

  .device-actions {
    flex-shrink: 0;
  }
}

// 响应式布局
@media (max-width: 768px) {
  .user-management {
    padding: 10px;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .user-detail .user-info {
    flex-direction: column;
    text-align: center;
  }
  
  .device-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
