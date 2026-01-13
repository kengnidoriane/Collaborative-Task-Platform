'use client';

import type { Project } from '@collaborative-task-platform/shared-types';
import {
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from '@collaborative-task-platform/ui-components';
import {
  Calendar,
  Crown,
  Globe,
  Lock,
  MoreVertical,
  Settings,
  Shield,
  Trash2,
  User,
  Users,
} from 'lucide-react';
import { useState } from 'react';
import { DeleteProjectDialog } from './delete-project-dialog';
import { ProjectSettings } from './project-settings';

interface ProjectCardProps {
  project: Project;
  variant?: 'card' | 'list';
  onProjectUpdated: (project: Project) => void;
  onProjectDeleted: (projectId: string) => void;
}

export function ProjectCard({
  project,
  variant = 'card',
  onProjectUpdated,
  onProjectDeleted,
}: ProjectCardProps) {
  const [showMenu, setShowMenu] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);

  const formatDate = (date: Date) => {
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    }).format(new Date(date));
  };

  const getRoleIcon = (role?: string) => {
    switch (role) {
      case 'OWNER':
        return <Crown className="h-4 w-4 text-yellow-500" />;
      case 'ADMIN':
        return <Shield className="h-4 w-4 text-blue-500" />;
      case 'MEMBER':
        return <User className="h-4 w-4 text-gray-500" />;
      default:
        return null;
    }
  };

  const canManageProject = project.userRole === 'OWNER' || project.userRole === 'ADMIN';
  const canDeleteProject = project.userRole === 'OWNER';

  if (variant === 'list') {
    return (
      <Card className="hover:shadow-md transition-shadow">
        <CardContent className="p-6">
          <div className="flex items-center justify-between">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-3 mb-2">
                <h3 className="text-lg font-semibold text-gray-900 truncate">{project.name}</h3>
                <div className="flex items-center gap-2">
                  {getRoleIcon(project.userRole)}
                  {project.isPrivate ? (
                    <Lock className="h-4 w-4 text-gray-400" />
                  ) : (
                    <Globe className="h-4 w-4 text-green-500" />
                  )}
                </div>
              </div>

              {project.description && (
                <p className="text-gray-600 text-sm mb-2 line-clamp-2">{project.description}</p>
              )}

              <div className="flex items-center gap-4 text-sm text-gray-500">
                <div className="flex items-center gap-1">
                  <Users className="h-4 w-4" />
                  {project.memberCount} member{project.memberCount !== 1 ? 's' : ''}
                </div>
                <div className="flex items-center gap-1">
                  <Calendar className="h-4 w-4" />
                  Created {formatDate(project.createdAt)}
                </div>
                <span>by {project.ownerName}</span>
              </div>
            </div>

            {canManageProject && (
              <div className="relative ml-4">
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => setShowMenu(!showMenu)}
                >
                  <MoreVertical className="h-4 w-4" />
                </Button>

                {showMenu && (
                  <div className="absolute right-0 top-full mt-1 w-48 bg-white border rounded-md shadow-lg z-10">
                    <div className="py-1">
                      <button
                        type="button"
                        onClick={() => {
                          setShowSettings(true);
                          setShowMenu(false);
                        }}
                        className="flex items-center gap-2 w-full px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                      >
                        <Settings className="h-4 w-4" />
                        Project Settings
                      </button>
                      {canDeleteProject && (
                        <button
                          type="button"
                          onClick={() => {
                            setShowDeleteDialog(true);
                            setShowMenu(false);
                          }}
                          className="flex items-center gap-2 w-full px-4 py-2 text-sm text-red-600 hover:bg-red-50"
                        >
                          <Trash2 className="h-4 w-4" />
                          Delete Project
                        </button>
                      )}
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <>
      <Card className="hover:shadow-md transition-shadow cursor-pointer">
        <CardHeader className="pb-3">
          <div className="flex items-start justify-between">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-1">
                <CardTitle className="text-lg truncate">{project.name}</CardTitle>
                {getRoleIcon(project.userRole)}
              </div>
              <div className="flex items-center gap-2">
                {project.isPrivate ? (
                  <div className="flex items-center gap-1 text-xs text-gray-500">
                    <Lock className="h-3 w-3" />
                    Private
                  </div>
                ) : (
                  <div className="flex items-center gap-1 text-xs text-green-600">
                    <Globe className="h-3 w-3" />
                    Public
                  </div>
                )}
              </div>
            </div>

            {canManageProject && (
              <div className="relative">
                <Button variant="ghost" size="sm" onClick={() => setShowMenu(!showMenu)}>
                  <MoreVertical className="h-4 w-4" />
                </Button>

                {showMenu && (
                  <div className="absolute right-0 top-full mt-1 w-48 bg-white border rounded-md shadow-lg z-10">
                    <div className="py-1">
                      <button
                        type="button"
                        onClick={() => {
                          setShowSettings(true);
                          setShowMenu(false);
                        }}
                        className="flex items-center gap-2 w-full px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                      >
                        <Settings className="h-4 w-4" />
                        Project Settings
                      </button>
                      {canDeleteProject && (
                        <button
                          type="button"
                          onClick={() => {
                            setShowDeleteDialog(true);
                            setShowMenu(false);
                          }}
                          className="flex items-center gap-2 w-full px-4 py-2 text-sm text-red-600 hover:bg-red-50"
                        >
                          <Trash2 className="h-4 w-4" />
                          Delete Project
                        </button>
                      )}
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        </CardHeader>

        <CardContent>
          {project.description && (
            <p className="text-gray-600 text-sm mb-4 line-clamp-3">{project.description}</p>
          )}

          <div className="space-y-2">
            <div className="flex items-center justify-between text-sm">
              <div className="flex items-center gap-1 text-gray-500">
                <Users className="h-4 w-4" />
                {project.memberCount} member{project.memberCount !== 1 ? 's' : ''}
              </div>
              <span className="text-gray-400">{formatDate(project.createdAt)}</span>
            </div>

            <div className="text-xs text-gray-500">Created by {project.ownerName}</div>
          </div>
        </CardContent>
      </Card>

      {/* Project Settings Modal */}
      {showSettings && (
        <ProjectSettings
          project={project}
          onProjectUpdated={onProjectUpdated}
          onClose={() => setShowSettings(false)}
        />
      )}

      {/* Delete Confirmation Dialog */}
      {showDeleteDialog && (
        <DeleteProjectDialog
          project={project}
          onProjectDeleted={onProjectDeleted}
          onClose={() => setShowDeleteDialog(false)}
        />
      )}
    </>
  );
}
