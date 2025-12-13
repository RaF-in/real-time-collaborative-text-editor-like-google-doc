export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
  timestamp?: string;
}

export interface ErrorResponse {
  message: string;
  status: number;
  error: string;
  path: string;
  timestamp: string;
  validationErrors?: Record<string, string>;
}