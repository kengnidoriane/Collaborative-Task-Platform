import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { RegisterForm } from '../register-form';

// Mock the auth context
const mockRegister = vi.fn();
const mockAuthContext = {
  user: null,
  isAuthenticated: false,
  isLoading: false,
  accessToken: null,
  login: vi.fn(),
  register: mockRegister,
  logout: vi.fn(),
  refreshToken: vi.fn(),
};

vi.mock('../../../lib/auth/auth-context', () => ({
  useAuth: () => mockAuthContext,
  AuthProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

describe('RegisterForm', () => {
  const user = userEvent.setup();

  beforeEach(() => {
    vi.clearAllMocks();
    mockAuthContext.isLoading = false;
  });

  it('renders registration form with all required fields', () => {
    render(<RegisterForm />);

    expect(screen.getByRole('heading', { name: /create account/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/full name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
    expect(screen.getByLabelText('Password')).toBeInTheDocument();
    expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create account/i })).toBeInTheDocument();
  });

  it('shows validation errors for empty fields', async () => {
    render(<RegisterForm />);

    const submitButton = screen.getByRole('button', { name: /create account/i });
    await user.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText(/full name is required/i)).toBeInTheDocument();
      expect(screen.getByText(/email is required/i)).toBeInTheDocument();
    });
  });

  it('shows validation error for invalid full name', async () => {
    render(<RegisterForm />);

    const fullNameInput = screen.getByLabelText(/full name/i);
    await user.type(fullNameInput, 'A');
    await user.tab();

    await waitFor(() => {
      expect(screen.getByText(/full name must be at least 2 characters/i)).toBeInTheDocument();
    });
  });

  it('shows validation error for full name with numbers', async () => {
    render(<RegisterForm />);

    const fullNameInput = screen.getByLabelText(/full name/i);
    await user.type(fullNameInput, 'John123');
    await user.tab();

    await waitFor(() => {
      expect(
        screen.getByText(/full name can only contain letters and spaces/i)
      ).toBeInTheDocument();
    });
  });

  it('shows password requirements when typing password', async () => {
    render(<RegisterForm />);

    const passwordInput = screen.getByLabelText('Password');
    await user.type(passwordInput, 'test');

    await waitFor(() => {
      expect(screen.getByText(/at least 8 characters/i)).toBeInTheDocument();
      expect(screen.getByText(/one uppercase letter/i)).toBeInTheDocument();
      expect(screen.getByText(/one lowercase letter/i)).toBeInTheDocument();
      expect(screen.getByText(/one number/i)).toBeInTheDocument();
      expect(screen.getByText(/one special character/i)).toBeInTheDocument();
    });
  });

  it('shows password mismatch error', async () => {
    render(<RegisterForm />);

    const passwordInput = screen.getByLabelText('Password');
    const confirmPasswordInput = screen.getByLabelText(/confirm password/i);

    await user.type(passwordInput, 'Password123!');
    await user.type(confirmPasswordInput, 'DifferentPassword123!');
    await user.tab();

    await waitFor(() => {
      expect(screen.getByText(/passwords don't match/i)).toBeInTheDocument();
    });
  });

  it('toggles password visibility for both password fields', async () => {
    render(<RegisterForm />);

    const passwordInput = screen.getByLabelText('Password');
    const confirmPasswordInput = screen.getByLabelText(/confirm password/i);
    const passwordToggle = screen.getAllByRole('button', { name: /show password/i })[0];
    const confirmPasswordToggle = screen.getAllByRole('button', { name: /show password/i })[1];

    expect(passwordInput).toHaveAttribute('type', 'password');
    expect(confirmPasswordInput).toHaveAttribute('type', 'password');

    if (passwordToggle) {
      await user.click(passwordToggle);
      expect(passwordInput).toHaveAttribute('type', 'text');
    }

    if (confirmPasswordToggle) {
      await user.click(confirmPasswordToggle);
      expect(confirmPasswordInput).toHaveAttribute('type', 'text');
    }
  });

  it('calls register function with correct data on form submission', async () => {
    render(<RegisterForm />);

    const fullNameInput = screen.getByLabelText(/full name/i);
    const emailInput = screen.getByLabelText(/email/i);
    const passwordInput = screen.getByLabelText('Password');
    const confirmPasswordInput = screen.getByLabelText(/confirm password/i);
    const submitButton = screen.getByRole('button', { name: /create account/i });

    await user.type(fullNameInput, 'John Doe');
    await user.type(emailInput, 'john@example.com');
    await user.type(passwordInput, 'Password123!');
    await user.type(confirmPasswordInput, 'Password123!');
    await user.click(submitButton);

    await waitFor(() => {
      expect(mockRegister).toHaveBeenCalledWith('john@example.com', 'Password123!', 'John Doe');
    });
  });

  it('displays error message when registration fails', async () => {
    const errorMessage = 'Email already exists';
    mockRegister.mockRejectedValueOnce(new Error(errorMessage));

    render(<RegisterForm />);

    const fullNameInput = screen.getByLabelText(/full name/i);
    const emailInput = screen.getByLabelText(/email/i);
    const passwordInput = screen.getByLabelText('Password');
    const confirmPasswordInput = screen.getByLabelText(/confirm password/i);
    const submitButton = screen.getByRole('button', { name: /create account/i });

    await user.type(fullNameInput, 'John Doe');
    await user.type(emailInput, 'john@example.com');
    await user.type(passwordInput, 'Password123!');
    await user.type(confirmPasswordInput, 'Password123!');
    await user.click(submitButton);

    await waitFor(() => {
      expect(screen.getByText(errorMessage)).toBeInTheDocument();
    });
  });

  it('calls onSwitchToLogin when login link is clicked', async () => {
    const mockSwitchToLogin = vi.fn();
    render(<RegisterForm onSwitchToLogin={mockSwitchToLogin} />);

    const loginLink = screen.getByRole('button', { name: /sign in/i });
    await user.click(loginLink);

    expect(mockSwitchToLogin).toHaveBeenCalled();
  });
});
