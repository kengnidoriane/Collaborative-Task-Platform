'use client';

import { useState } from 'react';
import { LoginForm } from './login-form';
import { RegisterForm } from './register-form';

type AuthMode = 'login' | 'register';

interface AuthLayoutProps {
  initialMode?: AuthMode;
  className?: string;
}

export function AuthLayout({ initialMode = 'login', className }: AuthLayoutProps) {
  const [mode, setMode] = useState<AuthMode>(initialMode);

  return (
    <div className={`min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8 ${className}`}>
      <div className="max-w-md w-full space-y-8">
        {/* Header */}
        <div className="text-center">
          <h1 className="text-3xl font-bold text-gray-900">
            Collaborative Task Platform
          </h1>
          <p className="mt-2 text-sm text-gray-600">
            Real-time collaborative task management
          </p>
        </div>

        {/* Auth Forms */}
        {mode === 'login' ? (
          <LoginForm onSwitchToRegister={() => setMode('register')} />
        ) : (
          <RegisterForm onSwitchToLogin={() => setMode('login')} />
        )}

        {/* Footer */}
        <div className="text-center text-xs text-gray-500">
          <p>
            By signing in, you agree to our{' '}
            <a href="/terms" className="text-blue-600 hover:text-blue-500">
              Terms of Service
            </a>{' '}
            and{' '}
            <a href="/privacy" className="text-blue-600 hover:text-blue-500">
              Privacy Policy
            </a>
          </p>
        </div>
      </div>
    </div>
  );
}