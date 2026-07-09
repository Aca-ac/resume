import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/auth";

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
        // 个人中心（已实现）
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

// 全局路由守卫
router.beforeEach((to, from, next) => {
  const auth = useAuthStore();

  // 如果是公开页面，直接放行
  if (to.meta.public) {
    next();
    return;
  }

  // 检查是否已登录
  const isAuthenticated = auth.isLoggedIn;

  // 如果未登录，跳转到登录页
  if (!isAuthenticated) {
    // 保存用户想要访问的页面，登录后跳转回来
    next({
      path: "/login",
      query: { redirect: to.fullPath }
    });
    return;
  }

  // 已登录，正常访问
  next();
});

export default router;