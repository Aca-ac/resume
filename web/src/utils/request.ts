import axios, { type AxiosRequestConfig, type AxiosInstance } from "axios";
import { useAuthStore } from "@/stores/auth";
import router from "@/router";

// ── Underlying axios instance ──────────────────────
const instance: AxiosInstance = axios.create({
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

instance.interceptors.request.use((config) => {
  const auth = useAuthStore();
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`;
  }
  return config;
});

// The interceptor unwraps { code, data } envelopes:
//   { code:0, data: T } → T
//   otherwise           → body as-is
instance.interceptors.response.use(
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
        return instance(config);
      } catch {
        auth.logout();
        router.push("/login");
      }
    }
    return Promise.reject(error);
  }
);

// ── Typed wrapper: reflects that the interceptor ──
//     already unwraps the envelope so callers get T
//     directly, not AxiosResponse<T>.
// ────────────────────────────────────────────────────
interface UnwrappedRequest {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>;
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>;
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>;
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>;
}

const request: UnwrappedRequest = {
  get: (url, config) => instance.get(url, config) as any,
  post: (url, data, config) => instance.post(url, data, config) as any,
  put: (url, data, config) => instance.put(url, data, config) as any,
  delete: (url, config) => instance.delete(url, config) as any,
};

export default request;