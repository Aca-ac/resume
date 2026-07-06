import axios from "axios";
import { useAuthStore } from "@/stores/auth";
import router from "@/router";

const request = axios.create({
  baseURL: "/api/v1",
  timeout: 30000
});

request.interceptors.request.use((config) => {
  const auth = useAuthStore();
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => {
    const body = response.data;
    if (body && typeof body.code === "number" && body.code !== 0) {
      return Promise.reject(new Error(body.message || "request failed"));
    }
    return body?.data !== undefined ? body.data : body;
  },
  async (error) => {
    const auth = useAuthStore();
    if (error.response?.status === 401 && auth.refreshToken) {
      try {
        await auth.refresh();
        error.config.headers.Authorization = `Bearer ${auth.accessToken}`;
        return request(error.config);
      } catch {
        auth.logout();
        router.push("/login");
      }
    }
    return Promise.reject(error);
  }
);

export default request;
