import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/auth";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/login", component: () => import("@/views/Login.vue"), meta: { public: true } },
    { path: "/register", component: () => import("@/views/Register.vue"), meta: { public: true } },
    {
      path: "/",
      component: () => import("@/components/AppLayout.vue"),
      children: [
        { path: "", redirect: "/dashboard" },
        { path: "dashboard", component: () => import("@/views/Dashboard.vue") },
        { path: "resumes", component: () => import("@/views/ResumeList.vue") },
        { path: "resumes/new", component: () => import("@/views/ResumeEditor.vue") },
        { path: "resumes/:id/edit", component: () => import("@/views/ResumeEditor.vue") },
        { path: "resumes/:id/optimize", component: () => import("@/views/ResumeOptimize.vue") },
        { path: "match", component: () => import("@/views/JobMatch.vue") },
        { path: "interview/start", component: () => import("@/views/InterviewStart.vue") },
        { path: "interview/:sessionId/chat", component: () => import("@/views/InterviewChat.vue") },
        { path: "interview/:sessionId/report", component: () => import("@/views/InterviewReport.vue") }
      ]
    }
  ]
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  if (!to.meta.public && !auth.isAuthenticated) {
    return "/login";
  }
  return true;
});

export default router;
