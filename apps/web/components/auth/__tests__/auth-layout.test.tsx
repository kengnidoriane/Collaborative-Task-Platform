import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { AuthLayout } from '../auth-layout';

// Mock the form components
vi.mock('../login-form', () => ({
  LoginForm: ({ onSwitchToRegister }: { onSwitchToRegister?: () => void }) => (
    <div data-testid="login-form">
      <button onClick={onSwitchToRegister}>Switch to Register</button>
    </div>
  ),
}));

vi.mock('../register-form', () => ({
  RegisterForm: ({ onSwitchToLogin }: { onSwitchToLogin?: () => void }) => (
    <div data-testid="register-form">
      <button onClick={onSwitchToLogin}>Switch to Login</button>
    </div>
  ),
}));

describe('AuthLayout', () => {
  const user = userEvent.setup();

  it('renders with login form by default', () => {
    render(<AuthLayout />);

    expect(screen.getByText(/collaborative task platform/i)).toBeInTheDocument();
    expect(screen.getByText(/real-time collaborative task management/i)).toBeInTheDocument();
    expect(screen.getByTestId('login-form')).toBeInTheDocument();
    expect(screen.queryByTestId('register-form')).not.toBeInTheDocument();
  });

  it('renders with register form when initialMode is register', () => {
    render(<AuthLayout initialMode="register" />);

    expect(screen.getByTestId('register-form')).toBeInTheDocument();
    expect(screen.queryByTestId('login-form')).not.toBeInTheDocument();
  });

  it('switches from login to register form', async () => {
    render(<AuthLayout />);

    expect(screen.getByTestId('login-form')).toBeInTheDocument();

    const switchButton = screen.getByText('Switch to Register');
    await user.click(switchButton);

    expect(screen.getByTestId('register-form')).toBeInTheDocument();
    expect(screen.queryByTestId('login-form')).not.toBeInTheDocument();
  });

  it('switches from register to login form', async () => {
    render(<AuthLayout initialMode="register" />);

    expect(screen.getByTestId('register-form')).toBeInTheDocument();

    const switchButton = screen.getByText('Switch to Login');
    await user.click(switchButton);

    expect(screen.getByTestId('login-form')).toBeInTheDocument();
    expect(screen.queryByTestId('register-form')).not.toBeInTheDocument();
  });

  it('renders footer with terms and privacy links', () => {
    render(<AuthLayout />);

    expect(screen.getByText(/by signing in, you agree to our/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /terms of service/i })).toHaveAttribute(
      'href',
      '/terms'
    );
    expect(screen.getByRole('link', { name: /privacy policy/i })).toHaveAttribute(
      'href',
      '/privacy'
    );
  });

  it('applies custom className', () => {
    const { container } = render(<AuthLayout className="custom-class" />);
    expect(container.firstChild).toHaveClass('custom-class');
  });
});
