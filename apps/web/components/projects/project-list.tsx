'use client';

import type { Project } from '@collaborative-task-platform/shared-types';
import { ProjectCard } from './project-card';

interface ProjectListProps {
  projects: Project[];
  viewMode: 'grid' | 'list';
  onProjectUpdated: (project: Project) => void;
  onProjectDeleted: (projectId: string) => void;
}

export function ProjectList({
  projects,
  viewMode,
  onProjectUpdated,
  onProjectDeleted,
}: ProjectListProps) {
  if (viewMode === 'grid') {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {projects.map((project) => (
          <ProjectCard
            key={project.id}
            project={project}
            onProjectUpdated={onProjectUpdated}
            onProjectDeleted={onProjectDeleted}
          />
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {projects.map((project) => (
        <ProjectCard
          key={project.id}
          project={project}
          variant="list"
          onProjectUpdated={onProjectUpdated}
          onProjectDeleted={onProjectDeleted}
        />
      ))}
    </div>
  );
}
