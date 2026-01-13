'use client';

import type React from 'react';

import type { Project, UpdateProjectDto } from '@collaborative-task-platform/shared-types';
import {
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Input,
  Label,
} from '@collaborative-task-platform/ui-components';
import { Loader2, Settings as SettingsIcon, Users, X } from 'lucide-react';
import { useEffect, useState } from 'react';
import { apiClient } from '../../lib/api/client';
import { TeamMemberManagement } from './team-member-management';

interface ProjectSettingsProps {
  project: Project;
  onProjectUpdated: (project: Project) => void;
  onClose: () => void;
}

interface FormData {
  name: string;
  description: string;
  isPrivate: boolean;
}

interface FormErrors {
  name?: string;
  description?: string;
  general?: string;
}

type TabType = 'general' | 'members';

export function ProjectSettings({ project, onProjectUpdated, onClose }: ProjectSettingsProps) {
  const [activeTab, setActiveTab] = useState<TabType>('general');
  const [formData, setFormData] = useState<FormData>({
    name: project.name,
    description: project.description || '',
    isPrivate: project.isPrivate,
  });
  const [errors, setErrors] = useState<FormErrors>({});
  const [loading, setLoading] = useState(false);
  const [hasChanges, setHasChanges] = useState(false);

  // Track changes
  useEffect(() => {
    const changed =
      formData.name !== project.name ||
      formData.description !== (project.description || '') ||
      formData.isPrivate !== project.isPrivate;
    setHasChanges(changed);
  }, [formData, project]);

  const validateForm = (): boolean => {
    const newErrors: FormErrors = {};

    // Validate name
    if (!formData.name.trim()) {
      newErrors.name = 'Project name is required';
    } else if (formData.name.trim().length < 1) {
      newErrors.name = 'Project name must be at least 1 character';
    } else if (formData.name.trim().length > 100) {
      newErrors.name = 'Project name must not exceed 100 characters';
    }

    // Validate description
    if (formData.description.length > 500) {
      newErrors.description = 'Description must not exceed 500 characters';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm() || !hasChanges) {
      return;
    }

    setLoading(true);
    setErrors({});

    try {
      const updateData: UpdateProjectDto = {
        name: formData.name.trim(),
        isPrivate: formData.isPrivate,
      };

      if (formData.description.trim()) {
        updateData.description = formData.description.trim();
      }

      const updatedProject = await apiClient.updateProject(project.id, updateData);
      onProjectUpdated(updatedProject);
      setHasChanges(false);
    } catch (error) {
      setErrors({
        general: error instanceof Error ? error.message : 'Failed to update project',
      });
    } finally {
      setLoading(false);
    }
  };

  const handleInputChange = (field: keyof FormData, value: string | boolean) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    // Clear field-specific error when user starts typing
    if (errors[field as keyof FormErrors]) {
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[field as keyof FormErrors];
        return newErrors;
      });
    }
  };

  const canManageMembers = project.userRole === 'OWNER' || project.userRole === 'ADMIN';

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <Card className="w-full max-w-2xl max-h-[90vh] overflow-hidden">
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2">
              <SettingsIcon className="h-5 w-5" />
              Project Settings
            </CardTitle>
            <Button variant="ghost" size="sm" onClick={onClose} disabled={loading}>
              <X className="h-4 w-4" />
            </Button>
          </div>

          {/* Tabs */}
          <div className="flex border-b">
            <button
              type="button"
              onClick={() => setActiveTab('general')}
              className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
                activeTab === 'general'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              General
            </button>
            {canManageMembers && (
              <button
                type="button"
                onClick={() => setActiveTab('members')}
                className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
                  activeTab === 'members'
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700'
                }`}
              >
                <Users className="h-4 w-4 inline mr-1" />
                Members
              </button>
            )}
          </div>
        </CardHeader>

        <CardContent className="overflow-y-auto max-h-[60vh]">
          {activeTab === 'general' && (
            <form onSubmit={handleSubmit} className="space-y-4">
              {/* General Error */}
              {errors.general && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-md">
                  <p className="text-sm text-red-600">{errors.general}</p>
                </div>
              )}

              {/* Project Name */}
              <div className="space-y-2">
                <Label htmlFor="name">
                  Project Name <span className="text-red-500">*</span>
                </Label>
                <Input
                  id="name"
                  type="text"
                  placeholder="Enter project name"
                  value={formData.name}
                  onChange={(e) => handleInputChange('name', e.target.value)}
                  disabled={loading}
                  className={errors.name ? 'border-red-500' : ''}
                  maxLength={100}
                />
                {errors.name && <p className="text-sm text-red-600">{errors.name}</p>}
                <p className="text-xs text-gray-500">{formData.name.length}/100 characters</p>
              </div>

              {/* Description */}
              <div className="space-y-2">
                <Label htmlFor="description">Description</Label>
                <textarea
                  id="description"
                  placeholder="Enter project description (optional)"
                  value={formData.description}
                  onChange={(e) => handleInputChange('description', e.target.value)}
                  disabled={loading}
                  className={`flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 resize-none ${
                    errors.description ? 'border-red-500' : ''
                  }`}
                  maxLength={500}
                  rows={3}
                />
                {errors.description && <p className="text-sm text-red-600">{errors.description}</p>}
                <p className="text-xs text-gray-500">
                  {formData.description.length}/500 characters
                </p>
              </div>

              {/* Privacy Setting */}
              <div className="space-y-2">
                <Label>Project Visibility</Label>
                <div className="space-y-2">
                  <label className="flex items-center space-x-2 cursor-pointer">
                    <input
                      type="radio"
                      name="privacy"
                      checked={!formData.isPrivate}
                      onChange={() => handleInputChange('isPrivate', false)}
                      disabled={loading}
                      className="text-blue-600"
                    />
                    <div>
                      <div className="font-medium text-sm">Public</div>
                      <div className="text-xs text-gray-500">
                        Anyone can discover and join this project
                      </div>
                    </div>
                  </label>

                  <label className="flex items-center space-x-2 cursor-pointer">
                    <input
                      type="radio"
                      name="privacy"
                      checked={formData.isPrivate}
                      onChange={() => handleInputChange('isPrivate', true)}
                      disabled={loading}
                      className="text-blue-600"
                    />
                    <div>
                      <div className="font-medium text-sm">Private</div>
                      <div className="text-xs text-gray-500">
                        Only invited members can access this project
                      </div>
                    </div>
                  </label>
                </div>
              </div>

              {/* Form Actions */}
              <div className="flex gap-3 pt-4">
                <Button
                  type="button"
                  variant="outline"
                  onClick={onClose}
                  disabled={loading}
                  className="flex-1"
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  disabled={loading || !hasChanges || !formData.name.trim()}
                  className="flex-1"
                >
                  {loading ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin mr-2" />
                      Saving...
                    </>
                  ) : (
                    'Save Changes'
                  )}
                </Button>
              </div>
            </form>
          )}

          {activeTab === 'members' && canManageMembers && (
            <TeamMemberManagement project={project} />
          )}
        </CardContent>
      </Card>
    </div>
  );
}
