'use client';

import type { User, AuthResponse } from '@collaborative-task-platform/shared-types';
import { createContext, useContext, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { apiClient } from '../api/client';

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  accessToken: string | null;
}

interface AuthContextType extends AuthState {
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, fullName: string) => Promise<void>;
  logout: () => void;
  refreshToken: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const API_BASE_URL = process.env['NEXT_PUBLIC_API_URL'] || 'http://localhost:8080';

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>({
    user: null,
    isAuthenticated: false,
    isLoading: true,
    accessToken: null,
  });
  const router = useRouter();

  // Initialize auth state from localStorage
  useEffect(() => {
    const initializeAuth = async () => {
      try {
        const token = localStorage.getItem('accessToken');
        const refreshTokenValue = localStorage.getItem('refreshToken');
        
        if (token && refreshTokenValue) {
          // Validate token and get user info
          try {
            const user = await apiClient.getCurrentUser();
            setState({
              user,
              isAuthenticated: true,
              isLoading: false,
              accessToken: token,
            });
          } catch (error) {
            // Token is invalid, try to refresh
            await refreshTokenInternal();
          }
        } else {
          setState(prev => ({ ...prev, isLoading: false }));
        }
      } catch (error) {
        console.error('Auth initialization error:', error);
        setState(prev => ({ ...prev, isLoading: false }));
      }
    };

    initializeAuth();
  }, []);

  const refreshTokenInternal = async () => {
    try {
      const refreshTokenValue = localStorage.getItem('refreshToken');
      if (!refreshTokenValue) {
        throw new Error('No refresh token available');
      }

      const authResponse = await apiClient.refreshToken(refreshTokenValue);
      localStorage.setItem('accessToken', authResponse.accessToken);
      localStorage.setItem('refreshToken', authResponse.refreshToken);
      
      setState({
        user: authResponse.user,
        isAuthenticated: true,
        isLoading: false,
        accessToken: authResponse.accessToken,
      });
    } catch (error) {
      console.error('Token refresh error:', error);
      logout();
    }
  };

  const login = async (email: string, password: string) => {
    try {
      setState(prev => ({ ...prev, isLoading: true }));
      
      const authResponse = await apiClient.login(email, password);
      
      // Store tokens
      localStorage.setItem('accessToken', authResponse.accessToken);
      localStorage.setItem('refreshToken', authResponse.refreshToken);
      
      setState({
        user: authResponse.user,
        isAuthenticated: true,
        isLoading: false,
        accessToken: authResponse.accessToken,
      });

      router.push('/dashboard');
    } catch (error) {
      setState(prev => ({ ...prev, isLoading: false }));
      throw error;
    }
  };

  const register = async (email: string, password: string, fullName: string) => {
    try {
      setState(prev => ({ ...prev, isLoading: true }));
      
      const authResponse = await apiClient.register(email, password, fullName);
      
      // Store tokens
      localStorage.setItem('accessToken', authResponse.accessToken);
      localStorage.setItem('refreshToken', authResponse.refreshToken);
      
      setState({
        user: authResponse.user,
        isAuthenticated: true,
        isLoading: false,
        accessToken: authResponse.accessToken,
      });

      router.push('/dashboard');
    } catch (error) {
      setState(prev => ({ ...prev, isLoading: false }));
      throw error;
    }
  };

  const logout = () => {
    // Clear tokens
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    
    setState({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      accessToken: null,
    });

    router.push('/login');
  };

  const refreshToken = async () => {
    await refreshTokenInternal();
  };

  const value: AuthContextType = {
    ...state,
    login,
    register,
    logout,
    refreshToken,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}