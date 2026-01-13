import type { Project } from '@collaborative-task-platform/shared-types';
import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ProjectCard } from '../project-card';

// Mock the API client
vi.mock('../../../lib/api/client', () => ({
  apiClient: {
    updateProject: vi.fn(),
    deleteProject: vi.fn(),
    getProjectMembers: vi.fn(),
  },
}));

const mockProject: Project = {
  id: '1',
  name: 'Test Project',
  description: 'A test project description',
  ownerId: 'owner-1',
  ownerName: 'John Doe',
  ownerEmail: 'john@example.com',
  isPrivate: false,
  archived: false,
  memberCount: 3,
  userRole: 'OWNER',
  createdAt: new Date('2024-01-01'),
  updatedAt: new Date('2024-01-01'),
};

describe('ProjectCard', () => {
  it('renders project information correctly', () => {
    const mockOnUpdate = vi.fn();
    const mockOnDelete = vi.fn();

    render(
      <ProjectCard
        project={mockProject}
        onProjectUpdated={mockOnUpdate}
        onProjectDeleted={mockOnDelete}
      />
    );

    expect(screen.getByText('Test Project')).toBeInTheDocument();
    expect(screen.getByText('A test project description')).toBeInTheDocument();
    expect(screen.getByText('3 members')).toBeInTheDocument();
    expect(screen.getByText('Created by John Doe')).toBeInTheDocument();
  });

  it('shows public indicator for public projects', () => {
    const mockOnUpdate = vi.fn();
    const mockOnDelete = vi.fn();

    render(
      <ProjectCard
        project={mockProject}
        onProjectUpdated={mockOnUpdate}
        onProjectDeleted={mockOnDelete}
      />
    );

    expect(screen.getByText('Public')).toBeInTheDocument();
  });

  it('shows private indicator for private projects', () => {
    const privateProject = { ...mockProject, isPrivate: true };
    const mockOnUpdate = vi.fn();
    const mockOnDelete = vi.fn();

    render(
      <ProjectCard
        project={privateProject}
        onProjectUpdated={mockOnUpdate}
        onProjectDeleted={mockOnDelete}
      />
    );

    expect(screen.getByText('Private')).toBeInTheDocument();
  });

  it('renders in list variant correctly', () => {
    const mockOnUpdate = vi.fn();
    const mockOnDelete = vi.fn();

    render(
      <ProjectCard
        project={mockProject}
        variant="list"
        onProjectUpdated={mockOnUpdate}
        onProjectDeleted={mockOnDelete}
      />
    );

    expect(screen.getByText('Test Project')).toBeInTheDocument();
    expect(screen.getByText('by John Doe')).toBeInTheDocument();
  });
});
