import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import {authApi} from "@/api/auth.ts";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // 公开页面（无需登录）
    {
      path: "/login",
      component: () => import("@/views/Login.vue"),
      meta: { public: true }
    },
    {
      path: "/register",
      component: () => import("@/views/Register.vue"),
      meta: { public: true }
    },
    {
      path: "/reset-password",
      component: () => import("@/views/ResetPassword.vue"),
      meta: { public: true }
    },
    // 如果忘记密码和重置密码是同一个页面，可以保留一个
    // 如果不同，可以保留两个
    {
      path: "/forgot-password",
      component: () => import("@/views/ForgotPassword.vue"),
      meta: { public: true }
    },
    // 根路径重定向
    {
      path: "/",
      redirect: "/dashboard"
    },
    // 需要登录的页面（使用AppLayout布局）
    {
      path: "/",
      component: () => import("@/components/AppLayout.vue"),
      meta: { requiresAuth: true },
      children: [
        // 仪表板
        {
          path: "dashboard",
          component: () => import("@/views/Dashboard.vue")
        },
        // 简历相关
        {
          path: "resumes",
          component: () => import("@/views/ResumeList.vue")
        },
        {
          path: "resumes/new",
          component: () => import("@/views/ResumeEditor.vue")
        },
        {
          path: "resumes/:id/edit",
          component: () => import("@/views/ResumeEditor.vue")
        },
        {
          path: "resumes/:id/optimize",
          component: () => import("@/views/ResumeOptimize.vue")
        },
        // 职位匹配
        {
          path: "match",
          component: () => import("@/views/JobMatch.vue")
        },
        // 面试相关
        {
          path: "interview/start",
          component: () => import("@/views/InterviewStart.vue")
        },
        {
          path: "interview/:sessionId/chat",
          component: () => import("@/views/InterviewChat.vue")
        },
        {
          path: "interview/:sessionId/report",
          component: () => import("@/views/InterviewReport.vue")
        },
        // 个人中心
        {
          path: "profile",
          component: () => import("@/views/Profile.vue"),
          meta: { requiresAuth: true }
        }
      ]
    },
    // 404重定向 - 所有未匹配的路由都重定向到登录页
    {
      path: "/:pathMatch(.*)*",
      redirect: "/login"
    }
  ]
});

// 全局前置守卫
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  const requiresAuth = to.meta.requiresAuth // 路由元信息标记是否需要登录

  if (requiresAuth && authStore.isLoggedIn) {
    // 用户已登录，需要验证 token 是否有效
    try {
      // 调用后端验证接口或尝试刷新 token
      const isValid = await validateToken()
      if (!isValid) {
        authStore.clearAuth()
        next('/login')
        return
      }
    } catch {
      authStore.clearAuth()
      next('/login')
      return
    }
  }

  next()
})

// 验证 token 有效性的函数
async function validateToken(): Promise<boolean> {
  const authStore = useAuthStore()
  if (!authStore.accessToken) return false

  try {
    // 可以调用后端的验证接口，或者尝试刷新 token
    const res = await authApi.refresh() // 尝试刷新 token
    if (res.code === 200) {
      authStore.setToken(res.data.accessToken)
      return true
    }
    return false
  } catch {
    return false
  }
}

export default router;