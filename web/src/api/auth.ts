import request from "@/utils/request";

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  userId: number;
  username: string;
}

export function register(data: { username: string; email: string; password: string }) {
  return request.post<AuthResponse>("/auth/register", data);
}

export function login(data: { username: string; password: string }) {
  return request.post<AuthResponse>("/auth/login", data);
}

export function refresh(data: { refreshToken: string }) {
  return request.post<AuthResponse>("/auth/refresh", data);
}