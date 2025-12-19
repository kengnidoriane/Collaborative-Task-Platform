// User types
export interface User {
  id: string;
  email: string;
  fullName: string;
  avatarUrl?: string;
  createdAt: Date;
  updatedAt: Date;
}

export interface UserRegistration {
  email: string;
  password: string;
  fullName: string;
}

export interface UserLogin {
  email: string;
  password: string;
}

export interface AuthResponse {
  user: User;
  accessToken: string;
  refreshToken: string;
}

// Project types
export interface Project {
  id: string;
  name: string;
  description?: string;
  ownerId: string;
  isPrivate: boolean;
  createdAt: Date;
  updatedAt: Date;
}

export interface ProjectMember {
  projectId: string;
  userId: string;
  role: 'OWNER' | 'ADMIN' | 'MEMBER';
  joinedAt: Date;
}

export interface CreateProjectDto {
  name: string;
  description?: string;
  isPrivate?: boolean;
  teamMembers?: string[];
}

// Task types
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

export interface Task {
  id: string;
  projectId: string;
  title: string;
  description?: string;
  status: TaskStatus;
  priority: TaskPriority;
  assigneeId?: string;
  createdBy: string;
  dueDate?: Date;
  tags: string[];
  createdAt: Date;
  updatedAt: Date;
}

export interface CreateTaskDto {
  projectId: string;
  title: string;
  description?: string;
  priority?: TaskPriority;
  assigneeId?: string;
  dueDate?: Date;
  tags?: string[];
}

export interface UpdateTaskDto {
  title?: string;
  description?: string;
  status?: TaskStatus;
  priority?: TaskPriority;
  assigneeId?: string;
  dueDate?: Date;
  tags?: string[];
}

// WebSocket message types
export enum WebSocketMessageType {
  TASK_CREATED = 'TASK_CREATED',
  TASK_UPDATED = 'TASK_UPDATED',
  TASK_DELETED = 'TASK_DELETED',
  USER_JOINED = 'USER_JOINED',
  USER_LEFT = 'USER_LEFT',
  CURSOR_MOVED = 'CURSOR_MOVED',
  TYPING_STARTED = 'TYPING_STARTED',
  TYPING_STOPPED = 'TYPING_STOPPED',
}

export interface WebSocketMessage<T = unknown> {
  type: WebSocketMessageType;
  payload: T;
  userId: string;
  projectId: string;
  timestamp: Date;
}

// Analytics types
export interface ProjectAnalytics {
  projectId: string;
  date: Date;
  tasksCreated: number;
  tasksCompleted: number;
  activeUsers: number;
  avgCompletionTime: number;
}

export interface TeamPerformance {
  userId: string;
  tasksCompleted: number;
  avgResponseTime: number;
}

// API Response types
export interface ApiResponse<T = unknown> {
  success: boolean;
  data?: T;
  error?: ApiError;
  timestamp: Date;
}

export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, unknown>;
}

// Pagination types
export interface PaginationParams {
  page: number;
  limit: number;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
}

export interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}
