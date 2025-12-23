'use client';

import type { User } from '@collaborative-task-platform/shared-types';
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
import { Check, Loader2, Settings, User as UserIcon } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { useAuth } from '../../lib/auth/auth-context';

const profileSchema = z.object({
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
});

type ProfileFormData = z.infer<typeof profileSchema>;

interface UserProfileProps {
  className?: string;
}

import React from 'react';

export function UserProfile({ className }: UserProfileProps) {
  const { user } = useAuth();
  const [isEditing, setIsEditing] = useState(false);
  const [updateSuccess, setUpdateSuccess] = useState(false);
  const [updateError, setUpdateError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<ProfileFormData>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      fullName: user?.fullName || '',
      email: user?.email || '',
    },
    mode: 'onBlur',
  });

  const onSubmit = async (_data: ProfileFormData) => {
    try {
      setUpdateError(null);
      setUpdateSuccess(false);

      // TODO: Implement profile update API call
      // const response = await updateProfile(data);

      // Simulate API call for now
      await new Promise((resolve) => setTimeout(resolve, 1000));

      setUpdateSuccess(true);
      setIsEditing(false);

      // Hide success message after 3 seconds
      setTimeout(() => setUpdateSuccess(false), 3000);
    } catch (error) {
      setUpdateError(error instanceof Error ? error.message : 'Update failed');
    }
  };

  const handleCancel = () => {
    reset();
    setIsEditing(false);
    setUpdateError(null);
  };

  if (!user) {
    return null;
  }

  return (
    <Card className={className}>
      <CardHeader className="flex flex-row items-center space-y-0 pb-2">
        <div className="flex items-center space-x-2">
          <UserIcon className="h-5 w-5" />
          <CardTitle className="text-lg">Profile Information</CardTitle>
        </div>
        {!isEditing && (
          <Button
            variant="outline"
            size="sm"
            onClick={() => setIsEditing(true)}
            className="ml-auto"
          >
            <Settings className="h-4 w-4 mr-2" />
            Edit
          </Button>
        )}
      </CardHeader>
      <CardContent>
        {updateSuccess && (
          <div className="mb-4 p-3 text-sm text-green-600 bg-green-50 border border-green-200 rounded-md flex items-center">
            <Check className="h-4 w-4 mr-2" />
            Profile updated successfully!
          </div>
        )}

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          {/* Full Name Field */}
          <div className="space-y-2">
            <Label htmlFor="fullName">Full Name</Label>
            <Input
              id="fullName"
              type="text"
              disabled={!isEditing || isSubmitting}
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
              disabled={!isEditing || isSubmitting}
              {...register('email')}
              className={errors.email ? 'border-red-500 focus:border-red-500' : ''}
            />
            {errors.email && (
              <p className="text-sm text-red-600" role="alert">
                {errors.email.message}
              </p>
            )}
          </div>

          {/* Account Information */}
          <div className="space-y-2">
            <Label>Account Created</Label>
            <p className="text-sm text-gray-600">
              {new Date(user.createdAt).toLocaleDateString('en-US', {
                year: 'numeric',
                month: 'long',
                day: 'numeric',
              })}
            </p>
          </div>

          {/* Update Error */}
          {updateError && (
            <div
              className="p-3 text-sm text-red-600 bg-red-50 border border-red-200 rounded-md"
              role="alert"
            >
              {updateError}
            </div>
          )}

          {/* Action Buttons */}
          {isEditing && (
            <div className="flex space-x-2 pt-4">
              <Button type="submit" disabled={isSubmitting || !isDirty} className="flex-1">
                {isSubmitting ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Updating...
                  </>
                ) : (
                  'Save Changes'
                )}
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={handleCancel}
                disabled={isSubmitting}
                className="flex-1"
              >
                Cancel
              </Button>
            </div>
          )}
        </form>
      </CardContent>
    </Card>
  );
}

// User Avatar Component
interface UserAvatarProps {
  user?: User | null;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}

export function UserAvatar({ user, size = 'md', className }: UserAvatarProps) {
  const sizeClasses = {
    sm: 'h-8 w-8 text-sm',
    md: 'h-10 w-10 text-base',
    lg: 'h-12 w-12 text-lg',
  };

  const getInitials = (name: string) => {
    return name
      .split(' ')
      .map((part) => part.charAt(0))
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  if (!user) {
    return (
      <div
        className={`${sizeClasses[size]} ${className} rounded-full bg-gray-200 flex items-center justify-center`}
      >
        <UserIcon className="h-1/2 w-1/2 text-gray-400" />
      </div>
    );
  }

  if (user.avatarUrl) {
    return (
      <img
        src={user.avatarUrl}
        alt={`${user.fullName}'s avatar`}
        className={`${sizeClasses[size]} ${className} rounded-full object-cover`}
      />
    );
  }

  return (
    <div
      className={`${sizeClasses[size]} ${className} rounded-full bg-blue-600 text-white flex items-center justify-center font-medium`}
      title={user.fullName}
    >
      {getInitials(user.fullName)}
    </div>
  );
}
