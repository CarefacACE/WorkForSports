import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';

export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
}

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
});

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('access_token');

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }

    try {
      const userInfo = JSON.parse(localStorage.getItem('user_info') || '{}');
      if (userInfo.id) config.headers['X-User-Id'] = String(userInfo.id);
      if (userInfo.username) config.headers['X-Username'] = userInfo.username;
      if (userInfo.role) config.headers['X-Role'] = userInfo.role;
    } catch {}

    return config;
  },
  (error: AxiosError) => Promise.reject(error),
);

service.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResponse;

    if (result.code !== 200) {
      return Promise.reject(new Error(result.message || '请求失败'));
    }

    return result.data as never;
  },
  (error: AxiosError<ApiResponse>) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('access_token');
    }

    return Promise.reject(new Error(error.response?.data?.message || error.message || '请求失败'));
  },
);

const request = {
  get<T = unknown>(url: string, params?: unknown): Promise<T> {
    return service.get(url, { params });
  },
  post<T = unknown>(url: string, data?: unknown): Promise<T> {
    return service.post(url, data);
  },
  put<T = unknown>(url: string, data?: unknown): Promise<T> {
    return service.put(url, data);
  },
  delete<T = unknown>(url: string, data?: unknown): Promise<T> {
    return service.delete(url, { data });
  },
};

export default request;
