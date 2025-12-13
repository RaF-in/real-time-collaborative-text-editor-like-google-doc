import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { User } from '../models/user.model';
import { ApiResponse } from '../models/api-response.model';
import { ChangePasswordRequest } from '../models/auth.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);
  private readonly API_URL = environment.apiUrl;

  /**
   * Get user by ID
   */
  getUserById(userId: string): Observable<ApiResponse<User>> {
    return this.http.get<ApiResponse<User>>(`${this.API_URL}/api/users/${userId}`);
  }

  /**
   * Update user
   */
  updateUser(userId: string, data: Partial<User>): Observable<ApiResponse<User>> {
    return this.http.put<ApiResponse<User>>(`${this.API_URL}/api/users/${userId}`, data);
  }

  /**
   * Change password
   */
  changePassword(userId: string, request: ChangePasswordRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `${this.API_URL}/api/users/${userId}/change-password`,
      request
    );
  }

  /**
   * Delete user account
   */
  deleteUser(userId: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.API_URL}/api/users/${userId}`);
  }
}