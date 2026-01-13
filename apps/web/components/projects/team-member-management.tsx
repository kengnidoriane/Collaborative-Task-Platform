'use client';

import type React from 'react';

import type {
  InviteMemberDto,
  Project,
  ProjectMember,
  UpdateMemberRoleDto,
} from '@collaborative-task-platform/shared-types';
import { Button, Input, Label } from '@collaborative-task-platform/ui-components';
import {
  Crown,
  Loader2,
  Mail,
  MoreVertical,
  Plus,
  Shield,
  Trash2,
  User,
  UserCheck,
  Users,
} from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { apiClient } from '../../lib/api/client';

interface TeamMemberManagementProps {
  project: Project;
}

interface InviteFormData {
  email: string;
  role: 'ADMIN' | 'MEMBER';
}

interface InviteFormErrors {
  email?: string;
  general?: string;
}

export function TeamMemberManagement({ project }: TeamMemberManagementProps) {
  const [members, setMembers] = useState<ProjectMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [inviteLoading, setInviteLoading] = useState(false);
  const [showInviteForm, setShowInviteForm] = useState(false);
  const [inviteForm, setInviteForm] = useState<InviteFormData>({
    email: '',
    role: 'MEMBER',
  });
  const [inviteErrors, setInviteErrors] = useState<InviteFormErrors>({});
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  const loadMembers = useCallback(async () => {
    try {
      setLoading(true);
      const membersData = await apiClient.getProjectMembers(project.id);
      setMembers(membersData);
    } catch (error) {
      console.error('Failed to load members:', error);
    } finally {
      setLoading(false);
    }
  }, [project.id]);

  useEffect(() => {
    loadMembers();
  }, [loadMembers]);

  const validateInviteForm = (): boolean => {
    const errors: InviteFormErrors = {};

    if (!inviteForm.email.trim()) {
      errors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(inviteForm.email)) {
      errors.email = 'Please enter a valid email address';
    }

    // Check if user is already a member
    const existingMember = members.find(
      (member) => member.userEmail.toLowerCase() === inviteForm.email.toLowerCase()
    );
    if (existingMember) {
      errors.email = 'This user is already a member of the project';
    }

    setInviteErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleInviteMember = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateInviteForm()) {
      return;
    }

    setInviteLoading(true);
    setInviteErrors({});

    try {
      const inviteData: InviteMemberDto = {
        email: inviteForm.email.trim(),
        role: inviteForm.role,
      };

      const newMember = await apiClient.inviteMember(project.id, inviteData);
      setMembers((prev) => [...prev, newMember]);
      setInviteForm({ email: '', role: 'MEMBER' });
      setShowInviteForm(false);
    } catch (error) {
      setInviteErrors({
        general: error instanceof Error ? error.message : 'Failed to invite member',
      });
    } finally {
      setInviteLoading(false);
    }
  };

  const handleUpdateRole = async (memberId: string, newRole: 'ADMIN' | 'MEMBER') => {
    setActionLoading(memberId);

    try {
      const updateData: UpdateMemberRoleDto = { role: newRole };
      const updatedMember = await apiClient.updateMemberRole(project.id, memberId, updateData);

      setMembers((prev) => prev.map((member) => (member.id === memberId ? updatedMember : member)));
    } catch (error) {
      console.error('Failed to update member role:', error);
    } finally {
      setActionLoading(null);
    }
  };

  const handleRemoveMember = async (memberId: string) => {
    if (!confirm('Are you sure you want to remove this member from the project?')) {
      return;
    }

    setActionLoading(memberId);

    try {
      await apiClient.removeMember(project.id, memberId);
      setMembers((prev) => prev.filter((member) => member.id !== memberId));
    } catch (error) {
      console.error('Failed to remove member:', error);
    } finally {
      setActionLoading(null);
    }
  };

  const getRoleIcon = (role: string) => {
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

  const getRoleLabel = (role: string) => {
    switch (role) {
      case 'OWNER':
        return 'Owner';
      case 'ADMIN':
        return 'Admin';
      case 'MEMBER':
        return 'Member';
      default:
        return role;
    }
  };

  const canManageMember = (member: ProjectMember) => {
    // Owner can manage everyone except themselves
    if (project.userRole === 'OWNER') {
      return member.role !== 'OWNER';
    }
    // Admin can only manage members (not other admins or owner)
    if (project.userRole === 'ADMIN') {
      return member.role === 'MEMBER';
    }
    return false;
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-8">
        <Loader2 className="h-6 w-6 animate-spin text-blue-600" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Invite Member Section */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-medium">Team Members</h3>
          <Button
            onClick={() => setShowInviteForm(!showInviteForm)}
            size="sm"
            className="flex items-center gap-2"
          >
            <Plus className="h-4 w-4" />
            Invite Member
          </Button>
        </div>

        {/* Invite Form */}
        {showInviteForm && (
          <div className="p-4 border rounded-md bg-gray-50 mb-4">
            <form onSubmit={handleInviteMember} className="space-y-4">
              {inviteErrors.general && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-md">
                  <p className="text-sm text-red-600">{inviteErrors.general}</p>
                </div>
              )}

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="invite-email">Email Address</Label>
                  <Input
                    id="invite-email"
                    type="email"
                    placeholder="Enter email address"
                    value={inviteForm.email}
                    onChange={(e) => {
                      setInviteForm((prev) => ({ ...prev, email: e.target.value }));
                      if (inviteErrors.email) {
                        setInviteErrors((prev) => {
                          const newErrors = { ...prev };
                          newErrors.email = undefined;
                          return newErrors;
                        });
                      }
                    }}
                    disabled={inviteLoading}
                    className={inviteErrors.email ? 'border-red-500' : ''}
                  />
                  {inviteErrors.email && (
                    <p className="text-sm text-red-600">{inviteErrors.email}</p>
                  )}
                </div>

                <div className="space-y-2">
                  <Label htmlFor="invite-role">Role</Label>
                  <select
                    id="invite-role"
                    value={inviteForm.role}
                    onChange={(e) =>
                      setInviteForm((prev) => ({
                        ...prev,
                        role: e.target.value as 'ADMIN' | 'MEMBER',
                      }))
                    }
                    disabled={inviteLoading}
                    className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    <option value="MEMBER">Member</option>
                    <option value="ADMIN">Admin</option>
                  </select>
                </div>
              </div>

              <div className="flex gap-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => {
                    setShowInviteForm(false);
                    setInviteForm({ email: '', role: 'MEMBER' });
                    setInviteErrors({});
                  }}
                  disabled={inviteLoading}
                  size="sm"
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  disabled={inviteLoading || !inviteForm.email.trim()}
                  size="sm"
                >
                  {inviteLoading ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin mr-2" />
                      Inviting...
                    </>
                  ) : (
                    <>
                      <Mail className="h-4 w-4 mr-2" />
                      Send Invitation
                    </>
                  )}
                </Button>
              </div>
            </form>
          </div>
        )}
      </div>

      {/* Members List */}
      <div className="space-y-3">
        {members.map((member) => (
          <div
            key={member.id}
            className="flex items-center justify-between p-3 border rounded-md bg-white"
          >
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 bg-gray-200 rounded-full flex items-center justify-center">
                <User className="h-4 w-4 text-gray-600" />
              </div>
              <div>
                <div className="font-medium text-sm">{member.userFullName}</div>
                <div className="text-xs text-gray-500">{member.userEmail}</div>
              </div>
            </div>

            <div className="flex items-center gap-3">
              <div className="flex items-center gap-1 text-sm">
                {getRoleIcon(member.role)}
                {getRoleLabel(member.role)}
              </div>

              {canManageMember(member) && (
                <div className="flex items-center gap-1">
                  {member.role === 'MEMBER' && (
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => handleUpdateRole(member.id, 'ADMIN')}
                      disabled={actionLoading === member.id}
                      className="text-xs"
                    >
                      {actionLoading === member.id ? (
                        <Loader2 className="h-3 w-3 animate-spin" />
                      ) : (
                        <>
                          <UserCheck className="h-3 w-3 mr-1" />
                          Promote
                        </>
                      )}
                    </Button>
                  )}

                  {member.role === 'ADMIN' && project.userRole === 'OWNER' && (
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => handleUpdateRole(member.id, 'MEMBER')}
                      disabled={actionLoading === member.id}
                      className="text-xs"
                    >
                      {actionLoading === member.id ? (
                        <Loader2 className="h-3 w-3 animate-spin" />
                      ) : (
                        'Demote'
                      )}
                    </Button>
                  )}

                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => handleRemoveMember(member.id)}
                    disabled={actionLoading === member.id}
                    className="text-xs text-red-600 hover:text-red-700 hover:bg-red-50"
                  >
                    {actionLoading === member.id ? (
                      <Loader2 className="h-3 w-3 animate-spin" />
                    ) : (
                      <Trash2 className="h-3 w-3" />
                    )}
                  </Button>
                </div>
              )}
            </div>
          </div>
        ))}

        {members.length === 0 && (
          <div className="text-center py-8 text-gray-500">
            <Users className="h-8 w-8 mx-auto mb-2 text-gray-400" />
            <p>No members found</p>
          </div>
        )}
      </div>
    </div>
  );
}
