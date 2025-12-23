import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios';
import type { User, AuthResponse } from '@collaborative-task-platform/shared-types';

const API_BASE_URL = process.env['NEXT_PUBLIC_API_URL'] || 'http://localhost:8080';

export class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public code?: string
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export class ApiClient {
  private axiosInstance: AxiosInstance;

  constructor(baseUrl: string = API_BASE_URL) {
    this.axiosInstance = axios.create({
      baseURL: baseUrl,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Request interceptor to add auth token
    this.axiosInstance.interceptors.request.use(
      (config: any) => {
        const token = this.getAuthToken();
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error: any) => Promise.reject(error)
    );

    // Response interceptor to handle errors
    this.axiosInstance.interceptors.response.use(
      (response: any) => response,
      (error: any) => {
        if (error.response) {
          // Server responded with error status
          const { status, data } = error.response;
          throw new ApiError(
            data?.message || `HTTP ${status}`,
            status,
            data?.code
          );
        } else if (error.request) {
          // Request was made but no response received
          throw new ApiError('Network error', 0);
        } else {
          // Something else happened
          throw new ApiError(error.message, 0);
        }
      }
    );
  }

  private getAuthToken(): string | null {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem('accessToken');
  }

  // Auth endpoints
  async login(email: string, password: string): Promise<AuthResponse> {
    const response = await this.axiosInstance.post<AuthResponse>('/api/v1/auth/login', {
      email,
      password,
    });
    return response.data;
  }

  async register(email: string, password: string, fullName: string): Promise<AuthResponse> {
    const response = await this.axiosInstance.post<AuthResponse>('/api/v1/auth/register', {
      email,
      password,
      fullName,
    });
    return response.data;
  }

  async refreshToken(refreshToken: string): Promise<AuthResponse> {
    const response = await this.axiosInstance.post<AuthResponse>('/api/v1/auth/refresh', {
      refreshToken,
    });
    return response.data;
  }

  async getCurrentUser(): Promise<User> {
    const response = await this.axiosInstance.get<User>('/api/v1/auth/me');
    return response.data;
  }

  async logout(): Promise<void> {
    await this.axiosInstance.post('/api/v1/auth/logout');
  }

  // Generic CRUD methods
  async get<T>(endpoint: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.axiosInstance.get<T>(endpoint, config);
    return response.data;
  }

  async post<T>(endpoint: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.axiosInstance.post<T>(endpoint, data, config);
    return response.data;
  }

  async put<T>(endpoint: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.axiosInstance.put<T>(endpoint, data, config);
    return response.data;
  }

  async patch<T>(endpoint: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.axiosInstance.patch<T>(endpoint, data, config);
    return response.data;
  }

  async delete<T>(endpoint: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.axiosInstance.delete<T>(endpoint, config);
    return response.data;
  }

  // File upload method
  async uploadFile<T>(
    endpoint: string,
    file: File,
    onUploadProgress?: (progressEvent: any) => void
  ): Promise<T> {
    const formData = new FormData();
    formData.append('file', file);

    const response = await this.axiosInstance.post<T>(endpoint, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress,
    });
    return response.data;
  }

  // Method to update base URL if needed
  updateBaseURL(baseUrl: string): void {
    this.axiosInstance.defaults.baseURL = baseUrl;
  }

  // Method to set default headers
  setDefaultHeader(key: string, value: string): void {
    this.axiosInstance.defaults.headers.common[key] = value;
  }

  // Method to remove default headers
  removeDefaultHeader(key: string): void {
    delete this.axiosInstance.defaults.headers.common[key];
  }
}

// Export a default instance
export const apiClient = new ApiClient();