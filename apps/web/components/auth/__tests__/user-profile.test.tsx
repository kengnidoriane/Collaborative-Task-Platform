import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UserProfile, UserAvatar } from '../user-profile';
import type { User } from '@collaborative-task-platform/shared-types';

// Mock the auth context
const mockUser: User = {
  id: '1',
  email: 'john@example.com',
  fullName: 'John Doe',
  createdAt: new Date('2023-01-01'),
  updatedAt: new Date('2023-01-01'),
};

const mockAuthContext = {
  user: mockUser,
  isAuthenticated: true,
  isLoading: false,
  accessToken: 'token',
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  refreshToken: vi.fn(),
};

vi.mock('../../../lib/auth/auth-context', () => ({
  useAuth: () => mockAuthContext,
}));

describe('UserProfile', () => {
  const user = userEvent.setup();

  beforeEach(() => {
    vi.clearAllMocks();
    mockAuthContext.user = mockUser;
  });

  it('renders user profile information', () => {
    render(<UserProfile />);

    expect(screen.getByText(/profile information/i)).toBeInTheDocument();
    expect(screen.getByDisplayValue('John Doe')).toBeInTheDocument();
    expect(screen.getByDisplayValue('john@example.com')).toBeInTheDocument();
    expect(screen.getByText(/january 1, 2023/i)).toBeInTheDocument();
  });

  it('shows edit button when not in editing mode', () => {
    render(<UserProfile />);

    expect(screen.getByRole('button', { name: /edit/i })).toBeInTheDocument();
    expect(screen.getByDisplayValue('John Doe')).toBeDisabled();
    expect(screen.getByDisplayValue('john@example.com')).toBeDisabled();
  });

  it('enables editing mode when edit button is clicked', async () => {
    render(<UserProfile />);

    const editButton = screen.getByRole('button', { name: /edit/i });
    await user.click(editButton);

    expect(screen.getByDisplayValue('John Doe')).toBeEnabled();
    expect(screen.getByDisplayValue('john@example.com')).toBeEnabled();
    expect(screen.getByRole('button', { name: /save changes/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument();
  });

  it('cancels editing and resets form', async () => {
    render(<UserProfile />);

    const editButton = screen.getByRole('button', { name: /edit/i });
    await user.click(editButton);

    const fullNameInput = screen.getByDisplayValue('John Doe');
    await user.clear(fullNameInput);
    await user.type(fullNameInput, 'Jane Doe');

    const cancelButton = screen.getByRole('button', { name: /cancel/i });
    await user.click(cancelButton);

    expect(screen.getByDisplayValue('John Doe')).toBeInTheDocument();
    expect(screen.getByDisplayValue('John Doe')).toBeDisabled();
  });

  it('shows validation errors for invalid input', async () => {
    render(<UserProfile />);

    const editButton = screen.getByRole('button', { name: /edit/i });
    await user.click(editButton);

    const fullNameInput = screen.getByDisplayValue('John Doe');
    await user.clear(fullNameInput);
    await user.tab();

    await waitFor(() => {
      expect(screen.getByText(/full name is required/i)).toBeInTheDocument();
    });
  });

  it('does not render when user is null', () => {
    (mockAuthContext as any).user = null;
    const { container } = render(<UserProfile />);
    expect(container.firstChild).toBeNull();
  });
});

describe('UserAvatar', () => {
  it('renders user initials when no avatar URL is provided', () => {
    render(<UserAvatar user={mockUser} />);

    expect(screen.getByText('JD')).toBeInTheDocument();
  });

  it('renders avatar image when avatar URL is provided', () => {
    const userWithAvatar = { ...mockUser, avatarUrl: 'https://example.com/avatar.jpg' };
    render(<UserAvatar user={userWithAvatar} />);

    const avatarImage = screen.getByRole('img');
    expect(avatarImage).toHaveAttribute('src', 'https://example.com/avatar.jpg');
    expect(avatarImage).toHaveAttribute('alt', "John Doe's avatar");
  });

  it('renders default icon when user is null', () => {
    render(<UserAvatar user={null} />);

    // The default icon should be rendered (UserIcon from lucide-react)
    expect(screen.getByRole('generic')).toBeInTheDocument();
  });

  it('applies correct size classes', () => {
    const { rerender } = render(<UserAvatar user={mockUser} size="sm" />);
    expect(screen.getByText('JD')).toHaveClass('h-8', 'w-8');

    rerender(<UserAvatar user={mockUser} size="md" />);
    expect(screen.getByText('JD')).toHaveClass('h-10', 'w-10');

    rerender(<UserAvatar user={mockUser} size="lg" />);
    expect(screen.getByText('JD')).toHaveClass('h-12', 'w-12');
  });

  it('generates correct initials for different names', () => {
    const { rerender } = render(<UserAvatar user={{ ...mockUser, fullName: 'John Doe' }} />);
    expect(screen.getByText('JD')).toBeInTheDocument();

    rerender(<UserAvatar user={{ ...mockUser, fullName: 'Alice' }} />);
    expect(screen.getByText('A')).toBeInTheDocument();

    rerender(<UserAvatar user={{ ...mockUser, fullName: 'Bob Smith Johnson' }} />);
    expect(screen.getByText('BS')).toBeInTheDocument();
  });
});