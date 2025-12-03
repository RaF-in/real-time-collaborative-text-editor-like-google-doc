// src/app/core/services/load-balancer.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ServerAssignment } from '../models/server-assignment.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class LoadBalancerService {
  private apiUrl = environment.apiUrl;
  
  constructor(private http: HttpClient) {}
  
  /**
   * Get server assignment using consistent hashing
   */
  getServerAssignment(key: string): Observable<ServerAssignment> {
    return this.http.get<ServerAssignment>(
      `${this.apiUrl}/api/loadbalancer/server?key=${key}`
    );
  }
  
  /**
   * Get all available servers
   */
  getAvailableServers(): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/loadbalancer/servers`);
  }
  
  /**
   * Get distribution statistics
   */
  getDistribution(sampleSize: number = 1000): Observable<any> {
    return this.http.get(
      `${this.apiUrl}/api/loadbalancer/distribution?sampleSize=${sampleSize}`
    );
  }
}