// router/index.ts
import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import { authApi } from "@/api/auth.ts";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // ===== 公开页面（无布局） =====
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

    // ===== 根路径重定向 =====
    {
      path: "/",
      redirect: "/dashboard"
    },

    // ===== 需要布局的页面（使用 AppLayout） =====
    {
      path: "/",
      component: () => import("@/components/AppLayout.vue"),
      children: [
        // Dashboard - 公开页面
        {
          path: "dashboard",
          component: () => import("@/views/Dashboard.vue"),
          meta: { public: true }
        },
        // 简历相关 - 需要登录
        {
          path: "resumes",
          component: () => import("@/views/ResumeList.vue"),
          meta: { requiresAuth: true }
        },
        {
          path: "resumes/import",
          component: () => import("@/views/ResumeImport.vue"),
          meta: { requiresAuth: true }
        },
        {
          path: "resumes/new",
          component: () => import("@/views/ResumeCreate.vue"),
          meta: { requiresAuth: true }
        },
        {
          path: "resumes/:id/edit",
          component: () => import("@/views/ResumeEditor.vue"),
          meta: { requiresAuth: true }
        },
        {
          path: "resumes/:id/optimize",
          component: () => import("@/views/ResumeOptimize.vue"),
          meta: { requiresAuth: true }
        },
        // 匹配
        {
          path: "match",
          component: () => import("@/views/JobMatch.vue"),
          meta: { requiresAuth: true }
        },
        // 面试
        {
          path: "interview/start",
          component: () => import("@/views/InterviewStart.vue"),
          meta: { requiresAuth: true }
        },
        {
          path: "interview/:sessionId/chat",
          component: () => import("@/views/InterviewChat.vue"),
          meta: { requiresAuth: true }
        },
        {
          path: "interview/:sessionId/report",
          component: () => import("@/views/InterviewReport.vue"),
          meta: { requiresAuth: true }
        },
        // 个人中心
        {
          path: "profile",
          component: () => import("@/views/Profile.vue"),
          meta: { requiresAuth: true }
        }
      ]
    },

    // 404 重定向
    {
      path: "/:pathMatch(.*)*",
      redirect: "/dashboard"
    }
  ]
});

// ===== Token 验证状态缓存 =====
let tokenValidationPromise: Promise<boolean> | null = null;
let lastValidationTime = 0;
const VALIDATION_INTERVAL = 5 * 60 * 1000;

// ===== 全局前置守卫 =====
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore();

  // 1. 已登录禁止再进入登录/注册页
  const publicAuthPages = ['/login', '/register', '/forgot-password', '/reset-password'];
  if (publicAuthPages.includes(to.path) && authStore.isLoggedIn) {
    return next('/dashboard');
  }

  // 2. 公开页面直接放行（包括 Dashboard）
  if (to.meta.public) {
    return next();
  }

  // 3. 检查是否需要登录
  if (to.meta.requiresAuth) {
    // 未登录跳转登录页，带上 redirect
    if (!authStore.isLoggedIn || !authStore.accessToken) {
      return next({ path: '/login', query: { redirect: to.fullPath } });
    }

    // 已登录，校验 token（缓存逻辑保持不变）
    if (Date.now() - lastValidationTime < VALIDATION_INTERVAL) {
      return next();
    }

    if (tokenValidationPromise) {
      try {
        const isValid = await tokenValidationPromise;
        if (!isValid) {
          authStore.clearAuth();
          return next({ path: '/login', query: { redirect: to.fullPath } });
        }
        return next();
      } catch {
        authStore.clearAuth();
        return next({ path: '/login', query: { redirect: to.fullPath } });
      }
    }

    tokenValidationPromise = validateToken();
    try {
      const isValid = await tokenValidationPromise;
      lastValidationTime = Date.now();
      tokenValidationPromise = null;

      if (!isValid) {
        authStore.clearAuth();
        return next({ path: '/login', query: { redirect: to.fullPath } });
      }
      return next();
    } catch (error) {
      console.error('Token validation error:', error);
      authStore.clearAuth();
      return next({ path: '/login', query: { redirect: to.fullPath } });
    }
  }

  // 默认放行
  next();
});

// ===== 验证 token 有效性 =====
async function validateToken(): Promise<boolean> {
  const authStore = useAuthStore();

  if (!authStore.accessToken) return false;

  try {
    const res = await authApi.refresh();
    if (res.code === 200 && res.data?.accessToken) {
      authStore.setToken(res.data.accessToken);
      return true;
    }
    return false;
  } catch {
    return false;
  }
}

// ===== 路由跳转错误处理 =====
router.onError((error) => {
  console.error('Router error:', error);
  if (error.message?.includes('Failed to fetch dynamically imported module')) {
    window.location.reload();
  }
});

export default router;

