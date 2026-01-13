'use client';

import type { Project } from '@collaborative-task-platform/shared-types';
import {
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Input,
  Label,
} from '@collaborative-task-platform/ui-components';
import { AlertTriangle, Loader2 } from 'lucide-react';
import { useState } from 'react';
import { apiClient } from '../../lib/api/client';

interface DeleteProjectDialogProps {
  project: Project;
  onProjectDeleted: (projectId: string) => void;
  onClose: () => void;
}

export function DeleteProjectDialog({
  project,
  onProjectDeleted,
  onClose,
}: DeleteProjectDialogProps) {
  const [confirmationText, setConfirmationText] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const expectedText = project.name;
  const isConfirmationValid = confirmationText === expectedText;

  const handleDelete = async () => {
    if (!isConfirmationValid) {
      return;
    }

    setLoading(true);
    setError(null);

    try {
      await apiClient.deleteProject(project.id);
      onProjectDeleted(project.id);
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete project');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-red-600">
            <AlertTriangle className="h-5 w-5" />
            Delete Project
          </CardTitle>
        </CardHeader>

        <CardContent className="space-y-4">
          {/* Warning Message */}
          <div className="p-4 bg-red-50 border border-red-200 rounded-md">
            <div className="flex items-start gap-3">
              <AlertTriangle className="h-5 w-5 text-red-500 mt-0.5 flex-shrink-0" />
              <div className="text-sm">
                <p className="font-medium text-red-800 mb-1">This action cannot be undone</p>
                <p className="text-red-700">
                  This will permanently delete the project "{project.name}" and all of its data,
                  including tasks, comments, and member access. All team members will lose access to
                  this project immediately.
                </p>
              </div>
            </div>
          </div>

          {/* Error Message */}
          {error && (
            <div className="p-3 bg-red-50 border border-red-200 rounded-md">
              <p className="text-sm text-red-600">{error}</p>
            </div>
          )}

          {/* Confirmation Input */}
          <div className="space-y-2">
            <Label htmlFor="confirmation">
              Type <span className="font-mono font-bold">{expectedText}</span> to confirm deletion:
            </Label>
            <Input
              id="confirmation"
              type="text"
              placeholder={`Type "${expectedText}" here`}
              value={confirmationText}
              onChange={(e) => setConfirmationText(e.target.value)}
              disabled={loading}
              className={confirmationText && !isConfirmationValid ? 'border-red-500' : ''}
            />
            {confirmationText && !isConfirmationValid && (
              <p className="text-sm text-red-600">
                Text doesn't match. Please type "{expectedText}" exactly.
              </p>
            )}
          </div>

          {/* Project Info */}
          <div className="text-sm text-gray-600 space-y-1">
            <p>
              <strong>Project:</strong> {project.name}
            </p>
            <p>
              <strong>Members:</strong> {project.memberCount}
            </p>
            <p>
              <strong>Created:</strong> {new Date(project.createdAt).toLocaleDateString()}
            </p>
          </div>

          {/* Action Buttons */}
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
              onClick={handleDelete}
              disabled={loading || !isConfirmationValid}
              variant="destructive"
              className="flex-1"
            >
              {loading ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin mr-2" />
                  Deleting...
                </>
              ) : (
                'Delete Project'
              )}
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
