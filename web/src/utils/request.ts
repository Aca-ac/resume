import axios, { type AxiosRequestConfig } from "axios";
import { useAuthStore } from "@/stores/auth";
import router from "@/router";

const request = axios.create({
  baseURL: "/api/v1",
  timeout: 30000
});

let refreshPromise: Promise<void> | null = null;

function refreshAccessToken(): Promise<void> {
  if (!refreshPromise) {
    const auth = useAuthStore();
    refreshPromise = auth
      .refresh()
      .then(() => undefined)
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

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
    const config = error.config as AxiosRequestConfig & { _retry?: boolean };
    if (error.response?.status === 401 && auth.refreshToken && config && !config._retry) {
      config._retry = true;
      try {
        await refreshAccessToken();
        config.headers = config.headers ?? {};
        config.headers.Authorization = `Bearer ${auth.accessToken}`;
        return request(config);
      } catch {
        auth.logout();
        router.push("/login");
      }
    }
    return Promise.reject(error);
  }
);

export default request;