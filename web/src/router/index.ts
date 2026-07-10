// router/index.ts
import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import { authApi } from "@/api/auth.ts";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // ===== 公开页面（无需登录） =====
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
    {
      path: "/forgot-password",
      component: () => import("@/views/ForgotPassword.vue"),
      meta: { public: true }
    },

    // ===== 首页仪表盘（游客可浏览） =====
    {
      path: "/dashboard",
      component: () => import("@/views/Dashboard.vue"),
      meta: { public: true }
    },

    // 根路径重定向到首页
    {
      path: "/",
      redirect: "/dashboard"
    },

    // ===== 需要登录的独立页面（全部独立布局） =====
    // 简历管理
    {
      path: "/resumes",
      component: () => import("@/views/ResumeList.vue"),
      meta: { requiresAuth: true }
    },
    {
      path: "/resumes/import",
      component: () => import("@/views/ResumeImport.vue"),
      meta: { requiresAuth: true }
    },
    {
      path: "/resumes/new",
      component: () => import("@/views/ResumeCreate.vue"),
      meta: { requiresAuth: true }
    },
    {
      path: "/resumes/:id/edit",
      component: () => import("@/views/ResumeEditor.vue"),
      meta: { requiresAuth: true }
    },
    {
      path: "/resumes/:id/optimize",
      component: () => import("@/views/ResumeOptimize.vue"),
      meta: { requiresAuth: true }
    },

    // 职位匹配
    {
      path: "/match",
      component: () => import("@/views/JobMatch.vue"),
      meta: { requiresAuth: true }
    },

    // 面试相关
    {
      path: "/interview/start",
      component: () => import("@/views/InterviewStart.vue"),
      meta: { requiresAuth: true }
    },
    {
      path: "/interview/:sessionId/chat",
      component: () => import("@/views/InterviewChat.vue"),
      meta: { requiresAuth: true }
    },
    {
      path: "/interview/:sessionId/report",
      component: () => import("@/views/InterviewReport.vue"),
      meta: { requiresAuth: true }
    },

    // 个人中心
    {
      path: "/profile",
      component: () => import("@/views/Profile.vue"),
      meta: { requiresAuth: true }
    },

    // 404 重定向到首页
    {
      path: "/:pathMatch(.*)*",
      redirect: "/dashboard"
    }
  ]
});

// ===== 全局前置守卫 =====
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore();

  // 已登录禁止再进入登录/注册页
  const publicAuthPages = ['/login', '/register', '/forgot-password', '/reset-password'];
  if (publicAuthPages.includes(to.path) && authStore.isLoggedIn) {
    return next('/dashboard');
  }

  // 公开页面直接放行
  if (to.meta.public) {
    return next();
  }

  // 需要登录的页面
  if (!authStore.isLoggedIn) {
    // 未登录 - 跳登录并记录跳转地址
    return next({ path: '/login', query: { redirect: to.fullPath } });
  }

  // 已登录，校验token是否有效
  try {
    const isValid = await validateToken();
    if (!isValid) {
      authStore.clearAuth();
      return next({ path: '/login', query: { redirect: to.fullPath } });
    }
    next();
  } catch {
    authStore.clearAuth();
    return next({ path: '/login', query: { redirect: to.fullPath } });
  }
});

// ===== 验证 token 有效性 =====
async function validateToken(): Promise<boolean> {
  const authStore = useAuthStore();
  if (!authStore.accessToken) return false;
  try {
    const res = await authApi.refresh();
    if (res.code === 200) {
      authStore.setToken(res.data.accessToken);
      return true;
    }
    return false;
  } catch {
    return false;
  }
}

export default router;