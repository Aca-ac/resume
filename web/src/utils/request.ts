// src/utils/request.ts
import axios, { AxiosInstance, AxiosRequestConfig, InternalAxiosRequestConfig, AxiosError } from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

// Token刷新锁
let isRefreshing = false
let refreshSubscribers: ((token: string) => void)[] = []

// 订阅Token刷新
function subscribeTokenRefresh(callback: (token: string) => void) {
    refreshSubscribers.push(callback)
}

// 通知所有订阅者Token已刷新
function onTokenRefreshed(token: string) {
    refreshSubscribers.forEach(callback => callback(token))
    refreshSubscribers = []
}

class Request {
    private instance: AxiosInstance

    constructor() {
        this.instance = axios.create({
            baseURL: '/api',
            timeout: 1000000,
            headers: {
                'Content-Type': 'application/json'
            }
        })

        this.setupInterceptors()
    }

    private setupInterceptors() {
        // 请求拦截器 - 添加Token
        this.instance.interceptors.request.use(
            (config: InternalAxiosRequestConfig) => {
                const authStore = useAuthStore()
                const token = authStore.accessToken

                if (token && config.headers) {
                    config.headers.Authorization = `Bearer ${token}`
                }
                // baseURL is '/api' — strip leading /api from url to avoid /api/api/...
                if (typeof config.url === 'string') {
                    const u = config.url.trim()
                    if (u.startsWith('/api/')) {
                        config.url = u.slice(4)
                    } else if (u.startsWith('api/')) {
                        config.url = '/' + u.slice(4)
                    }
                }
                return config
            },
            (error) => {
                return Promise.reject(error)
            }
        )

        // 响应拦截器 - 处理Token过期
        this.instance.interceptors.response.use(
            (response) => {
                return response
            },
            async (error: AxiosError) => {
                const originalRequest = error.config as AxiosRequestConfig & { _retry?: boolean }

                // 如果是401错误且不是刷新接口且没有重试过
                if (error.response?.status === 401 &&
                    !originalRequest.url?.includes('/auth/refresh') &&
                    !originalRequest._retry) {

                    // 如果已经在刷新Token，将请求加入队列
                    if (isRefreshing) {
                        return new Promise((resolve) => {
                            subscribeTokenRefresh((token: string) => {
                                if (originalRequest.headers) {
                                    originalRequest.headers.Authorization = `Bearer ${token}`
                                }
                                resolve(this.instance(originalRequest))
                            })
                        })
                    }

                    originalRequest._retry = true
                    isRefreshing = true

                    try {
                        const authStore = useAuthStore()
                        const newToken = await authStore.refreshToken()

                        if (newToken) {
                            // 通知所有等待的请求使用新Token
                            onTokenRefreshed(newToken)

                            // 重试原始请求
                            if (originalRequest.headers) {
                                originalRequest.headers.Authorization = `Bearer ${newToken}`
                            }
                            return this.instance(originalRequest)
                        } else {
                            // 刷新失败，跳转到登录页
                            authStore.logout()
                            window.location.href = '/login'
                            return Promise.reject(error)
                        }
                    } catch (refreshError) {
                        // 刷新失败
                        const authStore = useAuthStore()
                        authStore.logout()
                        window.location.href = '/login'
                        return Promise.reject(refreshError)
                    } finally {
                        isRefreshing = false
                    }
                }

                // 业务错误处理
                if (error.response?.data) {
                    const data = error.response.data as { message?: string; code?: number }
                    if (data.message) {
                        ElMessage.error({
                            message: data.message,
                            duration: 1000  //  设置为 1秒，单位毫秒
                        })
                    }
                } else if (error.message) {
                    ElMessage.error(error.message)
                }

                return Promise.reject(error)
            }
        )
    }

    // 封装请求方法
    public get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
        return this.instance.get(url, config).then(res => res.data)
    }

    public post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
        return this.instance.post(url, data, config).then(res => res.data)
    }

    public put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
        return this.instance.put(url, data, config).then(res => res.data)
    }

    public delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
        return this.instance.delete(url, config).then(res => res.data)
    }
}

export default new Request()