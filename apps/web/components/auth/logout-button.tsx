'use client';

import { Button } from '@collaborative-task-platform/ui-components';
import { Loader2, LogOut } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '../../lib/auth/auth-context';

interface LogoutButtonProps {
  variant?: 'default' | 'outline' | 'ghost';
  size?: 'sm' | 'lg';
  showIcon?: boolean;
  showText?: boolean;
  className?: string;
  onLogoutStart?: () => void;
  onLogoutComplete?: () => void;
}

export function LogoutButton({
  variant = 'outline',
  size = 'sm',
  showIcon = true,
  showText = true,
  className,
  onLogoutStart,
  onLogoutComplete,
}: LogoutButtonProps) {
  const { logout, isLoading } = useAuth();
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  const handleLogout = async () => {
    try {
      setIsLoggingOut(true);
      onLogoutStart?.();

      // Clear any pending requests or cleanup
      await new Promise((resolve) => setTimeout(resolve, 500));

      logout();

      onLogoutComplete?.();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      setIsLoggingOut(false);
    }
  };

  const isDisabled = isLoading || isLoggingOut;

  return (
    <Button
      variant={variant}
      size={size}
      onClick={handleLogout}
      disabled={isDisabled}
      className={className}
      aria-label="Sign out of your account"
    >
      {isLoggingOut ? (
        <>
          <Loader2 className="h-4 w-4 animate-spin" />
          {showText && <span className="ml-2">Signing out...</span>}
        </>
      ) : (
        <>
          {showIcon && <LogOut className="h-4 w-4" />}
          {showText && <span className={showIcon ? 'ml-2' : ''}>Sign Out</span>}
        </>
      )}
    </Button>
  );
}
