'use client';

import { Button } from '@collaborative-task-platform/ui-components';
import { Input } from '@collaborative-task-platform/ui-components';
import { Label } from '@collaborative-task-platform/ui-components';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@collaborative-task-platform/ui-components';
import { zodResolver } from '@hookform/resolvers/zod';
import { Check, Eye, EyeOff, Loader2, X } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { useAuth } from '../../lib/auth/auth-context';

const registerSchema = z
  .object({
    fullName: z
      .string()
      .min(1, 'Full name is required')
      .min(2, 'Full name must be at least 2 characters')
      .max(100, 'Full name must be less than 100 characters')
      .regex(/^[a-zA-Z\s]+$/, 'Full name can only contain letters and spaces'),
    email: z
      .string()
      .min(1, 'Email is required')
      .email('Please enter a valid email address')
      .max(255, 'Email must be less than 255 characters'),
    password: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .max(128, 'Password must be less than 128 characters')
      .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
      .regex(/[a-z]/, 'Password must contain at least one lowercase letter')
      .regex(/[0-9]/, 'Password must contain at least one number')
      .regex(/[^A-Za-z0-9]/, 'Password must contain at least one special character'),
    confirmPassword: z.string().min(1, 'Please confirm your password'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords don't match",
    path: ['confirmPassword'],
  });

type RegisterFormData = z.infer<typeof registerSchema>;

interface RegisterFormProps {
  onSwitchToLogin?: () => void;
}

export function RegisterForm({ onSwitchToLogin }: RegisterFormProps) {
  const { register: registerUser, isLoading } = useAuth();
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    mode: 'onBlur',
  });

  const password = watch('password');

  const onSubmit = async (data: RegisterFormData) => {
    try {
      setSubmitError(null);
      await registerUser(data.email, data.password, data.fullName);
    } catch (error) {
      setSubmitError(error instanceof Error ? error.message : 'Registration failed');
    }
  };

  const isFormLoading = isLoading || isSubmitting;

  // Password strength indicators
  const passwordRequirements = [
    { label: 'At least 8 characters', test: (pwd: string) => pwd.length >= 8 },
    { label: 'One uppercase letter', test: (pwd: string) => /[A-Z]/.test(pwd) },
    { label: 'One lowercase letter', test: (pwd: string) => /[a-z]/.test(pwd) },
    { label: 'One number', test: (pwd: string) => /[0-9]/.test(pwd) },
    { label: 'One special character', test: (pwd: string) => /[^A-Za-z0-9]/.test(pwd) },
  ];

  return (
    <Card className="w-full max-w-md mx-auto">
      <CardHeader className="space-y-1">
        <CardTitle className="text-2xl font-bold text-center">Create Account</CardTitle>
        <CardDescription className="text-center">
          Enter your information to create a new account
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {/* Full Name Field */}
          <div className="space-y-2">
            <Label htmlFor="fullName">Full Name</Label>
            <Input
              id="fullName"
              type="text"
              placeholder="Enter your full name"
              autoComplete="name"
              disabled={isFormLoading}
              {...register('fullName')}
              className={errors.fullName ? 'border-red-500 focus:border-red-500' : ''}
            />
            {errors.fullName && (
              <p className="text-sm text-red-600" role="alert">
                {errors.fullName.message}
              </p>
            )}
          </div>

          {/* Email Field */}
          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              placeholder="Enter your email"
              autoComplete="email"
              disabled={isFormLoading}
              {...register('email')}
              className={errors.email ? 'border-red-500 focus:border-red-500' : ''}
            />
            {errors.email && (
              <p className="text-sm text-red-600" role="alert">
                {errors.email.message}
              </p>
            )}
          </div>

          {/* Password Field */}
          <div className="space-y-2">
            <Label htmlFor="password">Password</Label>
            <div className="relative">
              <Input
                id="password"
                type={showPassword ? 'text' : 'password'}
                placeholder="Create a password"
                autoComplete="new-password"
                disabled={isFormLoading}
                {...register('password')}
                className={errors.password ? 'border-red-500 focus:border-red-500 pr-10' : 'pr-10'}
              />
              <button
                type="button"
                className="absolute inset-y-0 right-0 pr-3 flex items-center"
                onClick={() => setShowPassword(!showPassword)}
                disabled={isFormLoading}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? (
                  <EyeOff className="h-4 w-4 text-gray-400" />
                ) : (
                  <Eye className="h-4 w-4 text-gray-400" />
                )}
              </button>
            </div>

            {/* Password Requirements */}
            {password && (
              <div className="space-y-1">
                {passwordRequirements.map((req, index) => {
                  const isValid = req.test(password);
                  return (
                    <div key={index} className="flex items-center space-x-2 text-xs">
                      {isValid ? (
                        <Check className="h-3 w-3 text-green-500" />
                      ) : (
                        <X className="h-3 w-3 text-red-500" />
                      )}
                      <span className={isValid ? 'text-green-600' : 'text-red-600'}>
                        {req.label}
                      </span>
                    </div>
                  );
                })}
              </div>
            )}

            {errors.password && (
              <p className="text-sm text-red-600" role="alert">
                {errors.password.message}
              </p>
            )}
          </div>

          {/* Confirm Password Field */}
          <div className="space-y-2">
            <Label htmlFor="confirmPassword">Confirm Password</Label>
            <div className="relative">
              <Input
                id="confirmPassword"
                type={showConfirmPassword ? 'text' : 'password'}
                placeholder="Confirm your password"
                autoComplete="new-password"
                disabled={isFormLoading}
                {...register('confirmPassword')}
                className={
                  errors.confirmPassword ? 'border-red-500 focus:border-red-500 pr-10' : 'pr-10'
                }
              />
              <button
                type="button"
                className="absolute inset-y-0 right-0 pr-3 flex items-center"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                disabled={isFormLoading}
                aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
              >
                {showConfirmPassword ? (
                  <EyeOff className="h-4 w-4 text-gray-400" />
                ) : (
                  <Eye className="h-4 w-4 text-gray-400" />
                )}
              </button>
            </div>
            {errors.confirmPassword && (
              <p className="text-sm text-red-600" role="alert">
                {errors.confirmPassword.message}
              </p>
            )}
          </div>

          {/* Submit Error */}
          {submitError && (
            <div
              className="p-3 text-sm text-red-600 bg-red-50 border border-red-200 rounded-md"
              role="alert"
            >
              {submitError}
            </div>
          )}

          {/* Submit Button */}
          <Button type="submit" className="w-full" disabled={isFormLoading}>
            {isFormLoading ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Creating Account...
              </>
            ) : (
              'Create Account'
            )}
          </Button>

          {/* Switch to Login */}
          {onSwitchToLogin && (
            <div className="text-center">
              <p className="text-sm text-gray-600">
                Already have an account?{' '}
                <button
                  type="button"
                  onClick={onSwitchToLogin}
                  className="font-medium text-blue-600 hover:text-blue-500 focus:outline-none focus:underline"
                  disabled={isFormLoading}
                >
                  Sign in
                </button>
              </p>
            </div>
          )}
        </form>
      </CardContent>
    </Card>
  );
}
