import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LogoutButton } from '../logout-button';

// Mock the auth context
const mockLogout = vi.fn();
const mockAuthContext = {
  user: { id: '1', email: 'test@example.com', fullName: 'Test User', createdAt: new Date(), updatedAt: new Date() },
  isAuthenticated: true,
  isLoading: false,
  accessToken: 'token',
  login: vi.fn(),
  register: vi.fn(),
  logout: mockLogout,
  refreshToken: vi.fn(),
};

vi.mock('../../../lib/auth/auth-context', () => ({
  useAuth: () => mockAuthContext,
}));

describe('LogoutButton', () => {
  const user = userEvent.setup();

  beforeEach(() => {
    vi.clearAllMocks();
    mockAuthContext.isLoading = false;
  });

  it('renders logout button with default props', () => {
    render(<LogoutButton />);

    const button = screen.getByRole('button', { name: /sign out of your account/i });
    expect(button).toBeInTheDocument();
    expect(screen.getByText(/sign out/i)).toBeInTheDocument();
  });

  it('renders with custom text and icon settings', () => {
    render(<LogoutButton showIcon={false} showText={false} />);

    const button = screen.getByRole('button', { name: /sign out of your account/i });
    expect(button).toBeInTheDocument();
    expect(screen.queryByText(/sign out/i)).not.toBeInTheDocument();
  });

  it('calls logout function when clicked', async () => {
    render(<LogoutButton />);

    const button = screen.getByRole('button', { name: /sign out of your account/i });
    await user.click(button);

    await waitFor(() => {
      expect(mockLogout).toHaveBeenCalled();
    });
  });

  it('shows loading state during logout', async () => {
    render(<LogoutButton />);

    const button = screen.getByRole('button', { name: /sign out of your account/i });
    await user.click(button);

    // During the logout process, it should show loading state
    expect(screen.getByText(/signing out/i)).toBeInTheDocument();
  });

  it('is disabled when auth is loading', () => {
    mockAuthContext.isLoading = true;
    render(<LogoutButton />);

    const button = screen.getByRole('button', { name: /sign out of your account/i });
    expect(button).toBeDisabled();
  });

  it('calls onLogoutStart and onLogoutComplete callbacks', async () => {
    const onLogoutStart = vi.fn();
    const onLogoutComplete = vi.fn();

    render(
      <LogoutButton 
        onLogoutStart={onLogoutStart} 
        onLogoutComplete={onLogoutComplete} 
      />
    );

    const button = screen.getByRole('button', { name: /sign out of your account/i });
    await user.click(button);

    await waitFor(() => {
      expect(onLogoutStart).toHaveBeenCalled();
      expect(onLogoutComplete).toHaveBeenCalled();
    });
  });

  it('applies custom className', () => {
    render(<LogoutButton className="custom-class" />);

    const button = screen.getByRole('button', { name: /sign out of your account/i });
    expect(button).toHaveClass('custom-class');
  });

  it('renders with different variants', () => {
    const { rerender } = render(<LogoutButton variant="default" />);
    let button = screen.getByRole('button', { name: /sign out of your account/i });
    expect(button).toBeInTheDocument();

    rerender(<LogoutButton variant="ghost" />);
    button = screen.getByRole('button', { name: /sign out of your account/i });
    expect(button).toBeInTheDocument();
  });
});