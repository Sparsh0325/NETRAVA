import { Case } from '../types';

export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8085/api').replace(/\/$/, '');

export class ApiError extends Error {
  constructor(message: string, public readonly status: number, public readonly payload?: unknown) {
    super(message);
    this.name = 'ApiError';
  }
}

export interface ApiUser {
  id: number | string;
  name: string;
  email: string;
  badgeNumber: string;
  agency: string;
  role: string;
  department?: string | null;
  phone?: string | null;
  avatarUrl?: string | null;
}

export interface AuthResult {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: ApiUser;
}

export interface CasePage {
  content: ApiCase[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

interface ApiCase extends Omit<Case, 'date' | 'location' | 'assignedOfficer'> {
  date: string | null;
  location: { lat: number | null; lng: number | null; address: string | null; city: string | null };
  assignedOfficer: string | null;
}

function normalizeCase(value: ApiCase): Case {
  return { ...value, date: value.date || '', assignedOfficer: value.assignedOfficer || '',
    location: { lat: value.location.lat ?? 0, lng: value.location.lng ?? 0,
      address: value.location.address || '', city: value.location.city || '' } };
}

export interface DashboardStats {
  totalCases: number;
  activeCases: number;
  highPriorityCases: number;
  closedCases: number;
}
export interface CaseAnalytics {
  totalCases: number;
  byType: Array<{ label: string; count: number }>;
  byStatus: Array<{ label: string; count: number }>;
  byPriority: Array<{ label: string; count: number }>;
  byMonth: Array<{ label: string; count: number }>;
}
export interface IngestionResult { jobId: string; status: 'SUCCEEDED' | 'FAILED'; processedRows: number; errors: string[]; }
export interface CaseShare {
  id: number; caseNumber: string; caseTitle: string; ownerAgency: string; recipientAgency: string;
  permission: 'READ' | 'EDIT'; status: 'ACTIVE' | 'REVOKED'; createdAt: string; revokedAt?: string | null;
}
export interface IntelligenceResult {
  text: string; provider: string; webGrounded: boolean; notice: string; sources: Array<{ title: string; uri: string }>;
}
export function analyzeIntelligence(prompt: string, webSearch = true) {
  return apiRequest<IntelligenceResult>('/intelligence/analyze', { method: 'POST', body: JSON.stringify({ prompt, webSearch }) });
}

export interface AuditEvent {
  id: number; chainIndex: number; timestamp: string; actorUsername: string; actorRole: string;
  agency: string | null; action: string; resource: string; details: string | null; hash: string;
}
export interface AuditPage { content: AuditEvent[]; page: number; size: number; totalElements: number; totalPages: number; }

export function fetchAuditLogs(params: { page?: number; size?: number; q?: string } = {}) {
  const query = new URLSearchParams({ page: String(params.page ?? 0), size: String(params.size ?? 10) });
  if (params.q?.trim()) query.set('q', params.q.trim());
  return apiRequest<AuditPage>(`/audit-logs?${query}`);
}

export function fetchMyAuditLogs(params: { page?: number; size?: number } = {}) {
  const query = new URLSearchParams({ page: String(params.page ?? 0), size: String(params.size ?? 20) });
  return apiRequest<AuditPage>(`/audit-logs/me?${query}`);
}

export async function downloadAuditCsv() {
  const token = localStorage.getItem('intellishare_token');
  const response = await fetch(`${API_BASE_URL}/audit-logs/export.csv`, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
  if (!response.ok) throw new ApiError(`Audit export failed (${response.status})`, response.status);
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a'); anchor.href = url; anchor.download = 'netrava-audit.csv'; anchor.click(); URL.revokeObjectURL(url);
}

export async function downloadMyAuditCsv() {
  const token = localStorage.getItem('intellishare_token');
  const response = await fetch(`${API_BASE_URL}/audit-logs/me/export.csv`, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
  if (!response.ok) throw new ApiError(`Personal audit export failed (${response.status})`, response.status);
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a'); anchor.href = url; anchor.download = 'netrava-my-audit.csv'; anchor.click(); URL.revokeObjectURL(url);
}

export interface CriminalRecord {
  id: number;
  recordNumber: string;
  name: string;
  alias?: string | null;
  dateOfBirth?: string | null;
  age?: number | null;
  gender?: string | null;
  status: 'Wanted' | 'In Custody' | 'Surveillance' | 'Released';
  riskLevel: 'High' | 'Medium' | 'Low';
  crimeType?: string | null;
  lastSeenLocation?: string | null;
  notes?: string | null;
  syntheticDemo: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CriminalRecordPage {
  content: CriminalRecord[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export function saveSession(token: string) {
  localStorage.setItem('intellishare_token', token);
}

export function clearSession() {
  localStorage.removeItem('intellishare_token');
  localStorage.removeItem('intellishare_user');
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');
  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  const token = ['/auth/login', '/auth/register'].includes(path) ? null : localStorage.getItem('intellishare_token');
  if (token) headers.set('Authorization', `Bearer ${token}`);

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers });
  } catch {
    throw new ApiError(`Cannot connect to the NETRAVA API at ${API_BASE_URL}.`, 0);
  }

  const payload = response.status === 204 ? undefined : await response.json().catch(() => undefined);
  if (!response.ok) {
    if (response.status === 401 && path !== '/auth/login' && path !== '/auth/register') {
      clearSession();
      window.dispatchEvent(new Event('intellishare:session-expired'));
    }
    const message = payload && typeof payload === 'object' && 'message' in payload
      ? String((payload as { message: unknown }).message)
      : `Request failed (${response.status})`;
    throw new ApiError(message, response.status, payload);
  }
  return payload as T;
}

export function mapUser(user: ApiUser) {
  return {
    id: String(user.id), name: user.name, email: user.email, badgeNumber: user.badgeNumber,
    agency: user.agency, role: user.role as import('../types').Role,
    department: user.department, phone: user.phone, avatar: user.avatarUrl || undefined,
  };
}

export async function fetchCases(params: { q?: string; status?: string; type?: string; priority?: string; city?: string; page?: number; size?: number } = {}) {
  const query = new URLSearchParams();
  query.set('page', String(params.page ?? 0));
  query.set('size', String(params.size ?? 100));
  query.set('sort', 'createdAt');
  query.set('direction', 'desc');
  if (params.q?.trim()) query.set('q', params.q.trim());
  if (params.status && params.status !== 'All') query.set('status', params.status);
  if (params.type) query.set('type', params.type);
  if (params.priority) query.set('priority', params.priority);
  if (params.city) query.set('city', params.city);
  const page = await apiRequest<CasePage>(`/cases?${query.toString()}`);
  return { ...page, content: page.content.map(normalizeCase) };
}

export async function fetchCase(id: string) {
  return normalizeCase(await apiRequest<ApiCase>(`/cases/${encodeURIComponent(id)}`));
}

export function createCase(data: {
  caseNumber: string; title: string; description?: string; type: string; priority: string;
  incidentDate?: string; address?: string; city?: string; latitude?: number; longitude?: number;
  entities?: Array<{ type: string; value: string; details?: string }>;
}) {
  return apiRequest<ApiCase>('/cases', { method: 'POST', body: JSON.stringify(data) }).then(normalizeCase);
}

export function updateCase(id: string, data: {
  title: string; description?: string; type: string; status: Case['status']; priority: Case['priority'];
  incidentDate?: string; address?: string; city?: string; latitude?: number; longitude?: number;
}) {
  return apiRequest<ApiCase>(`/cases/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(data) }).then(normalizeCase);
}

export function deleteCase(id: string) {
  return apiRequest<void>(`/cases/${encodeURIComponent(id)}`, { method: 'DELETE' });
}

export function fetchDashboardStats() {
  return apiRequest<DashboardStats>('/dashboard/stats');
}

export function fetchCaseAnalytics() { return apiRequest<CaseAnalytics>('/dashboard/analytics'); }

export function uploadCaseCsv(file: File) {
  const form = new FormData(); form.append('file', file);
  return apiRequest<IngestionResult>('/ingestion/cases.csv', { method: 'POST', body: form });
}

export function fetchShares() { return apiRequest<CaseShare[]>('/shares'); }
export function createCaseShare(caseKey: string, recipientAgency: string, permission: 'READ' | 'EDIT') {
  return apiRequest<CaseShare>(`/shares/cases/${encodeURIComponent(caseKey)}`, { method: 'POST', body: JSON.stringify({ recipientAgency, permission }) });
}
export function revokeCaseShare(id: number) { return apiRequest<void>(`/shares/${id}`, { method: 'DELETE' }); }

export function fetchCriminalRecords(params: { q?: string; status?: string; riskLevel?: string; page?: number; size?: number } = {}) {
  const query = new URLSearchParams({
    page: String(params.page ?? 0), size: String(params.size ?? 20), sort: 'createdAt', direction: 'desc',
  });
  if (params.q?.trim()) query.set('q', params.q.trim());
  if (params.status && params.status !== 'All') query.set('status', params.status);
  if (params.riskLevel && params.riskLevel !== 'All') query.set('riskLevel', params.riskLevel);
  return apiRequest<CriminalRecordPage>(`/criminal-records?${query.toString()}`);
}

export function createCriminalRecord(data: {
  name: string; alias?: string; dateOfBirth?: string; age?: number; gender?: string;
  status: CriminalRecord['status']; riskLevel: CriminalRecord['riskLevel']; crimeType?: string;
  lastSeenLocation?: string; notes?: string;
}) {
  return apiRequest<CriminalRecord>('/criminal-records', { method: 'POST', body: JSON.stringify(data) });
}

export function deleteCriminalRecord(recordNumber: string) {
  return apiRequest<void>(`/criminal-records/${encodeURIComponent(recordNumber)}`, { method: 'DELETE' });
}
