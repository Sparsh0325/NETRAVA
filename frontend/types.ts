export type Role = 'Administrator' | 'Officer' | 'Analyst' | 'Read-Only';

export interface User {
  id: string;
  name: string;
  badgeNumber: string;
  agency: string;
  role: Role;
  email: string;
  department?: string | null;
  phone?: string | null;
  avatar?: string;
}

export type CaseStatus = 'Registered' | 'Under Investigation' | 'Closed' | 'Cold Case';

export interface Case {
  id: string;
  title: string;
  description: string;
  type: string;
  status: CaseStatus;
  date: string;
  location: {
    lat: number;
    lng: number;
    address: string;
    city: string;
  };
  priority: 'High' | 'Medium' | 'Low';
  assignedOfficer: string;
  entities: Entity[];
  evidence?: EvidenceRecord[];
  timeline?: TimelineRecord[];
  similarityScore?: number; // For search/analytics
}

export interface EvidenceRecord {
  id: number;
  title: string;
  description?: string | null;
  fileUrl?: string | null;
  fileType?: string | null;
  fileSizeBytes?: number | null;
  sha256Hash?: string | null;
  blockchainTxId?: string | null;
  uploadedBy?: string | null;
  createdAt: string;
}

export interface TimelineRecord {
  id: number;
  action: string;
  details?: string | null;
  performedBy?: string | null;
  timestamp: string;
}

export interface Entity {
  id: string;
  type: 'Person' | 'Location' | 'Vehicle' | 'Phone' | 'Organization';
  value: string;
  details?: string;
}

export interface ActivityLog {
  id: string;
  action: string;
  user: string;
  timestamp: string;
  type: 'info' | 'warning' | 'alert' | 'success';
}

export interface Notification {
  id: string;
  title: string;
  message: string;
  type: 'alert' | 'update' | 'share' | 'system';
  read: boolean;
  time: string;
}

export interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  token: string | null;
}
