// API endpoints
export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: '/auth/login',
    REGISTER: '/auth/register',
    REFRESH: '/auth/refresh',
    LOGOUT: '/auth/logout',
    PROFILE: '/auth/profile',
  },
  PROJECTS: {
    BASE: '/projects',
    MEMBERS: (projectId: string) => `/projects/${projectId}/members`,
    ANALYTICS: (projectId: string) => `/projects/${projectId}/analytics`,
  },
  TASKS: {
    BASE: '/tasks',
    BY_PROJECT: (projectId: string) => `/projects/${projectId}/tasks`,
    BY_ID: (taskId: string) => `/tasks/${taskId}`,
  },
  USERS: {
    BASE: '/users',
    BY_ID: (userId: string) => `/users/${userId}`,
    SEARCH: '/users/search',
  },
} as const;

// Task constants
export const TASK_STATUS = {
  TODO: 'TODO',
  IN_PROGRESS: 'IN_PROGRESS',
  IN_REVIEW: 'IN_REVIEW',
  DONE: 'DONE',
} as const;

export const TASK_PRIORITY = {
  LOW: 'LOW',
  MEDIUM: 'MEDIUM',
  HIGH: 'HIGH',
  URGENT: 'URGENT',
} as const;

export const TASK_STATUS_LABELS = {
  [TASK_STATUS.TODO]: 'To Do',
  [TASK_STATUS.IN_PROGRESS]: 'In Progress',
  [TASK_STATUS.IN_REVIEW]: 'In Review',
  [TASK_STATUS.DONE]: 'Done',
} as const;

export const TASK_PRIORITY_LABELS = {
  [TASK_PRIORITY.LOW]: 'Low',
  [TASK_PRIORITY.MEDIUM]: 'Medium',
  [TASK_PRIORITY.HIGH]: 'High',
  [TASK_PRIORITY.URGENT]: 'Urgent',
} as const;

// Project constants
export const PROJECT_ROLES = {
  OWNER: 'OWNER',
  ADMIN: 'ADMIN',
  MEMBER: 'MEMBER',
} as const;

export const PROJECT_ROLE_LABELS = {
  [PROJECT_ROLES.OWNER]: 'Owner',
  [PROJECT_ROLES.ADMIN]: 'Admin',
  [PROJECT_ROLES.MEMBER]: 'Member',
} as const;

// WebSocket events
export const WS_EVENTS = {
  CONNECTION: 'connection',
  DISCONNECT: 'disconnect',
  JOIN_PROJECT: 'join_project',
  LEAVE_PROJECT: 'leave_project',
  TASK_CREATED: 'task_created',
  TASK_UPDATED: 'task_updated',
  TASK_DELETED: 'task_deleted',
  USER_JOINED: 'user_joined',
  USER_LEFT: 'user_left',
  CURSOR_MOVED: 'cursor_moved',
  TYPING_STARTED: 'typing_started',
  TYPING_STOPPED: 'typing_stopped',
} as const;

// Validation constants
export const VALIDATION = {
  PASSWORD_MIN_LENGTH: 8,
  PROJECT_NAME_MAX_LENGTH: 100,
  PROJECT_DESCRIPTION_MAX_LENGTH: 500,
  TASK_TITLE_MAX_LENGTH: 200,
  TASK_DESCRIPTION_MAX_LENGTH: 1000,
  USER_NAME_MAX_LENGTH: 100,
  MAX_TAGS_PER_TASK: 10,
  TAG_MAX_LENGTH: 30,
} as const;

// UI constants
export const UI = {
  SIDEBAR_WIDTH: 280,
  HEADER_HEIGHT: 64,
  MOBILE_BREAKPOINT: 768,
  TABLET_BREAKPOINT: 1024,
  DESKTOP_BREAKPOINT: 1280,
} as const;

// Storage keys
export const STORAGE_KEYS = {
  AUTH_TOKEN: 'auth_token',
  REFRESH_TOKEN: 'refresh_token',
  USER_PREFERENCES: 'user_preferences',
  THEME: 'theme',
  SIDEBAR_STATE: 'sidebar_state',
  RECENT_PROJECTS: 'recent_projects',
} as const;

// Error codes
export const ERROR_CODES = {
  UNAUTHORIZED: 'UNAUTHORIZED',
  FORBIDDEN: 'FORBIDDEN',
  NOT_FOUND: 'NOT_FOUND',
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  INTERNAL_ERROR: 'INTERNAL_ERROR',
  NETWORK_ERROR: 'NETWORK_ERROR',
  TIMEOUT: 'TIMEOUT',
} as const;

// Date formats
export const DATE_FORMATS = {
  SHORT: 'MMM d, yyyy',
  LONG: 'MMMM d, yyyy',
  WITH_TIME: 'MMM d, yyyy h:mm a',
  ISO: 'yyyy-MM-dd',
  TIME_ONLY: 'h:mm a',
} as const;
