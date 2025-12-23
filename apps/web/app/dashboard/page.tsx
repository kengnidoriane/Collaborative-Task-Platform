'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '../../lib/auth/auth-context';
import { UserProfile } from '../../components/auth/user-profile';
import { LogoutButton } from '../../components/auth/logout-button';
import { UserAvatar } from '../../components/auth/user-profile';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@collaborative-task-platform/ui-components';
import { Loader2 } from 'lucide-react';

export default function DashboardPage() {
  const { isAuthenticated, isLoading, user } = useAuth();
  const router = useRouter();

  useEffect(() => {
    // Only redirect if we're not loading and not authenticated
    if (!isLoading && !isAuthenticated) {
      router.push('/login');
    }
  }, [isAuthenticated, isLoading, router]);

  // Show loading state while checking authentication
  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="flex flex-col items-center space-y-4">
          <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
          <p className="text-sm text-gray-600">Checking authentication...</p>
        </div>
      </div>
    );
  }

  // Don't render children if not authenticated
  if (!isAuthenticated) {
    return null;
  }

  return <DashboardContent />;
}

function DashboardContent() {
  const { user } = useAuth();

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center space-x-4">
              <h1 className="text-xl font-semibold text-gray-900">
                Collaborative Task Platform
              </h1>
            </div>
            <div className="flex items-center space-x-4">
              <UserAvatar user={user} size="sm" />
              <span className="text-sm text-gray-700">
                Welcome, {user?.fullName}
              </span>
              <LogoutButton size="sm" />
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto py-6 px-4 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Welcome Card */}
          <div className="lg:col-span-2">
            <Card>
              <CardHeader>
                <CardTitle>Welcome to Your Dashboard</CardTitle>
                <CardDescription>
                  You have successfully authenticated and can now access protected content.
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-4">
                  <div className="p-4 bg-green-50 border border-green-200 rounded-md">
                    <h3 className="text-sm font-medium text-green-800">
                      Authentication Successful
                    </h3>
                    <p className="text-sm text-green-700 mt-1">
                      Your session is active and secure. You can now access all platform features.
                    </p>
                  </div>
                  
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div className="p-4 border rounded-md">
                      <h4 className="font-medium text-gray-900">Projects</h4>
                      <p className="text-sm text-gray-600 mt-1">
                        Create and manage your collaborative projects
                      </p>
                    </div>
                    <div className="p-4 border rounded-md">
                      <h4 className="font-medium text-gray-900">Tasks</h4>
                      <p className="text-sm text-gray-600 mt-1">
                        Track and organize your team's work
                      </p>
                    </div>
                    <div className="p-4 border rounded-md">
                      <h4 className="font-medium text-gray-900">Real-time Collaboration</h4>
                      <p className="text-sm text-gray-600 mt-1">
                        Work together with instant updates
                      </p>
                    </div>
                    <div className="p-4 border rounded-md">
                      <h4 className="font-medium text-gray-900">Analytics</h4>
                      <p className="text-sm text-gray-600 mt-1">
                        Monitor team performance and progress
                      </p>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* User Profile */}
          <div>
            <UserProfile />
          </div>
        </div>
      </main>
    </div>
  );
}