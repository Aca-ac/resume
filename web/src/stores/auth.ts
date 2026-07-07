import { defineStore } from "pinia";
import { login as loginApi, register as registerApi, refresh as refreshApi } from "@/api/auth";

interface AuthState {
  accessToken: string;
  refreshToken: string;
  userId: number | null;
  username: string;
}

const STORAGE_KEY = "resume-assistant-auth";

export const useAuthStore = defineStore("auth", {
  state: (): AuthState => ({
    accessToken: "",
    refreshToken: "",
    userId: null,
    username: ""
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken)
  },
  actions: {
    load() {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return;
      try {
        Object.assign(this.$state, JSON.parse(raw));
      } catch {
        localStorage.removeItem(STORAGE_KEY);
      }
    },
    persist() {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.$state));
    },
    setSession(payload: { accessToken: string; refreshToken: string; userId: number; username: string }) {
      this.accessToken = payload.accessToken;
      this.refreshToken = payload.refreshToken;
      this.userId = payload.userId;
      this.username = payload.username;
      this.persist();
    },
    async login(username: string, password: string) {
      const data = await loginApi({ username, password });
      this.setSession(data);
    },
    async register(username: string, email: string, password: string) {
      const data = await registerApi({ username, email, password });
      this.setSession(data);
    },
    async refresh() {
      const data = await refreshApi({ refreshToken: this.refreshToken });
      this.setSession(data);
    },
    logout() {
      this.$reset();
      localStorage.removeItem(STORAGE_KEY);
    }
  }
});
