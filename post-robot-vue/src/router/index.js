import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/LoginView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      redirect: '/dashboard',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: '首页看板', roles: ['ADMIN', 'OPERATOR', 'MAINTAINER'] }
        },
        {
          path: 'products',
          name: 'ProductList',
          component: () => import('@/views/product/ProductList.vue'),
          meta: { title: '商品管理', roles: ['ADMIN', 'OPERATOR'] }
        },
        {
          path: 'products/add',
          name: 'ProductAdd',
          component: () => import('@/views/product/ProductForm.vue'),
          meta: { title: '新增商品', roles: ['ADMIN', 'OPERATOR'] }
        },
        {
          path: 'products/edit/:id',
          name: 'ProductEdit',
          component: () => import('@/views/product/ProductForm.vue'),
          meta: { title: '编辑商品', roles: ['ADMIN', 'OPERATOR'] }
        },
        {
          path: 'inventory',
          name: 'Inventory',
          component: () => import('@/views/inventory/InventoryView.vue'),
          meta: { title: '库存管理', roles: ['ADMIN', 'OPERATOR'] }
        },
        {
          path: 'tasks',
          name: 'TaskMonitor',
          component: () => import('@/views/task/TaskMonitor.vue'),
          meta: { title: '任务监控', roles: ['ADMIN', 'MAINTAINER'] }
        },
        {
          path: 'users',
          name: 'UserManage',
          component: () => import('@/views/user/UserManage.vue'),
          meta: { title: '用户管理', roles: ['ADMIN'] }
        },
        {
          path: 'orders',
          name: 'OrderList',
          component: () => import('@/views/order/OrderList.vue'),
          meta: { title: '订单管理', roles: ['ADMIN', 'OPERATOR'] }
        },
        {
          path: 'alerts',
          name: 'AlertManage',
          component: () => import('@/views/alert/AlertManage.vue'),
          meta: { title: '告警管理', roles: ['ADMIN', 'MAINTAINER'] }
        },
        {
          path: 'audit-logs',
          name: 'AuditLogView',
          component: () => import('@/views/audit/AuditLogView.vue'),
          meta: { title: '审计日志', roles: ['ADMIN'] }
        },
        {
          path: 'postal',
          name: 'PostalManage',
          component: () => import('@/views/postal/PostalManage.vue'),
          meta: { title: '邮政对接', roles: ['ADMIN'] }
        }
      ]
    }
  ]
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth !== false && !auth.token) {
    next('/login')
  } else if (to.path === '/login' && auth.token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
