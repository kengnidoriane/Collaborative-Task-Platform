import type { AuthResponse, User } from '@collaborative-task-platform/shared-types';
import { act, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider, useAuth } from '../auth-context';

// Mock the API client
const mockApiClient = {
  login: vi.fn(),
  register: vi.fn(),
  refreshToken: vi.fn(),
  getCurrentUser: vi.fn(),
};

vi.mock('../../api/client', () => ({
  apiClient: mockApiClient,
}));

// Mock Next.js router
const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
  }),
}));

// Mock localStorage
const localStorageMock = {
  getItem: vi.fn(),
  setItem: vi.fn(),
  removeItem: vi.fn(),
};
Object.defineProperty(window, 'localStorage', { value: localStorageMock });

// Mock Axios
vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() },
      },
      post: vi.fn(),
      get: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
      patch: vi.fn(),
    })),
  },
}));

// Test component to access auth context
function TestComponent() {
  const auth = useAuth();
  return (
    <div>
      <div data-testid="isAuthenticated">{auth.isAuthenticated.toString()}</div>
      <div data-testid="isLoading">{auth.isLoading.toString()}</div>
      <div data-testid="user">{auth.user?.fullName || 'null'}</div>
      <button onClick={() => auth.login('test@example.com', 'password')}>Login</button>
      <button onClick={() => auth.register('test@example.com', 'password', 'Test User')}>
        Register
      </button>
      <button onClick={() => auth.logout()}>Logout</button>
    </div>
  );
}

describe('AuthContext', () => {
  const mockUser: User = {
    id: '1',
    email: 'test@example.com',
    fullName: 'Test User',
    createdAt: new Date(),
    updatedAt: new Date(),
  };

  const mockAuthResponse: AuthResponse = {
    user: mockUser,
    accessToken: 'access-token',
    refreshToken: 'refresh-token',
  };

  beforeEach(() => {
    vi.clearAllMocks();
    localStorageMock.getItem.mockReturnValue(null);
    mockPush.mockClear();
  });

  it('provides initial unauthenticated state', async () => {
    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('false');
      expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
      expect(screen.getByTestId('user')).toHaveTextContent('null');
    });
  });

  it('initializes with existing token and validates user', async () => {
    localStorageMock.getItem.mockImplementation((key: string) => {
      if (key === 'accessToken') return 'existing-token';
      if (key === 'refreshToken') return 'existing-refresh-token';
      return null;
    });
    mockApiClient.getCurrentUser.mockResolvedValue(mockUser);

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true');
      expect(screen.getByTestId('user')).toHaveTextContent('Test User');
    });

    expect(mockApiClient.getCurrentUser).toHaveBeenCalled();
  });

  it('handles successful login', async () => {
    mockApiClient.login.mockResolvedValue(mockAuthResponse);

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
    });

    const loginButton = screen.getByText('Login');
    await act(async () => {
      loginButton.click();
    });

    await waitFor(() => {
      expect(mockApiClient.login).toHaveBeenCalledWith('test@example.com', 'password');
      expect(localStorageMock.setItem).toHaveBeenCalledWith('accessToken', 'access-token');
      expect(localStorageMock.setItem).toHaveBeenCalledWith('refreshToken', 'refresh-token');
      expect(mockPush).toHaveBeenCalledWith('/dashboard');
      expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true');
      expect(screen.getByTestId('user')).toHaveTextContent('Test User');
    });
  });

  it('handles successful registration', async () => {
    mockApiClient.register.mockResolvedValue(mockAuthResponse);

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('isLoading')).toHaveTextContent('false');
    });

    const registerButton = screen.getByText('Register');
    await act(async () => {
      registerButton.click();
    });

    await waitFor(() => {
      expect(mockApiClient.register).toHaveBeenCalledWith(
        'test@example.com',
        'password',
        'Test User'
      );
      expect(localStorageMock.setItem).toHaveBeenCalledWith('accessToken', 'access-token');
      expect(localStorageMock.setItem).toHaveBeenCalledWith('refreshToken', 'refresh-token');
      expect(mockPush).toHaveBeenCalledWith('/dashboard');
      expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true');
    });
  });

  it('handles logout', async () => {
    // Start with authenticated state
    localStorageMock.getItem.mockImplementation((key) => {
      if (key === 'accessToken') return 'existing-token';
      if (key === 'refreshToken') return 'existing-refresh-token';
      return null;
    });
    mockApiClient.getCurrentUser.mockResolvedValue(mockUser);

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true');
    });

    const logoutButton = screen.getByText('Logout');
    await act(async () => {
      logoutButton.click();
    });

    await waitFor(() => {
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('accessToken');
      expect(localStorageMock.removeItem).toHaveBeenCalledWith('refreshToken');
      expect(mockPush).toHaveBeenCalledWith('/login');
      expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('false');
      expect(screen.getByTestId('user')).toHaveTextContent('null');
    });
  });

  it('handles token refresh when initial validation fails', async () => {
    localStorageMock.getItem.mockImplementation((key) => {
      if (key === 'accessToken') return 'expired-token';
      if (key === 'refreshToken') return 'valid-refresh-token';
      return null;
    });
    mockApiClient.getCurrentUser.mockRejectedValue(new Error('Token expired'));
    mockApiClient.refreshToken.mockResolvedValue(mockAuthResponse);

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(mockApiClient.refreshToken).toHaveBeenCalledWith('valid-refresh-token');
      expect(screen.getByTestId('isAuthenticated')).toHaveTextContent('true');
    });
  });

  it('throws error when useAuth is used outside AuthProvider', () => {
    // Suppress console.error for this test
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

    expect(() => {
      render(<TestComponent />);
    }).toThrow('useAuth must be used within an AuthProvider');

    consoleSpy.mockRestore();
  });
});
