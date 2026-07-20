<template>
  <el-container style="height: 100vh">
    <el-aside width="220px" style="background-color: #304156">
      <div class="logo">
        <el-icon style="font-size: 24px; margin-right: 8px; color: #409eff"><Monitor /></el-icon>
        <span>主题邮局机器人</span>
      </div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>首页看板</span>
        </el-menu-item>
        <el-menu-item index="/products" v-if="hasRole('ADMIN','OPERATOR')">
          <el-icon><Goods /></el-icon>
          <span>商品管理</span>
        </el-menu-item>
        <el-menu-item index="/orders" v-if="hasRole('ADMIN','OPERATOR')">
          <el-icon><List /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/inventory" v-if="hasRole('ADMIN','OPERATOR')">
          <el-icon><Coin /></el-icon>
          <span>库存管理</span>
        </el-menu-item>
        <el-menu-item index="/alerts" v-if="hasRole('ADMIN','MAINTAINER')">
          <el-icon><WarningFilled /></el-icon>
          <span>告警管理</span>
        </el-menu-item>
        <el-menu-item index="/tasks" v-if="hasRole('ADMIN','MAINTAINER')">
          <el-icon><List /></el-icon>
          <span>任务监控</span>
        </el-menu-item>
        <el-menu-item index="/users" v-if="hasRole('ADMIN')">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/audit-logs" v-if="hasRole('ADMIN')">
          <el-icon><Clock /></el-icon>
          <span>审计日志</span>
        </el-menu-item>
        <el-menu-item index="/postal" v-if="hasRole('ADMIN')">
          <el-icon><Connection /></el-icon>
          <span>邮政对接</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="display: flex; align-items: center; justify-content: space-between; background: #fff; border-bottom: 1px solid #e6e6e6; height: 56px; padding: 0 20px;">
        <el-breadcrumb>
          <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
        </el-breadcrumb>
        <el-dropdown @command="handleCommand">
          <span style="cursor: pointer; display: flex; align-items: center; gap: 6px;">
            <el-avatar :size="28" icon="UserFilled" />
            {{ auth.user?.realName || auth.user?.username || '用户' }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main style="background: #f5f7fa; padding: 16px; overflow-y: auto;">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()

function hasRole(...roles) {
  return roles.includes(auth.user?.role)
}

function handleCommand(command) {
  if (command === 'logout') {
    auth.doLogout()
  }
}
</script>

<style scoped>
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.el-menu {
  border-right: none;
}
.el-header {
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
</style>
