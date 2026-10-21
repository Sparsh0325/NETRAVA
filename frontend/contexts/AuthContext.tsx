import React, { createContext, useContext, useEffect, useState } from 'react';
import { AuthState, User } from '../types';
import { apiRequest, AuthResult, clearSession, mapUser, saveSession } from '../lib/api';

interface RegisterData {
  name: string;
  email: string;
  badgeNumber: string;
  agency: string;
  department?: string;
  phone?: string;
  password: string;
}

interface AuthContextType extends AuthState {
  isLoading: boolean;
  login: (email: string, password: string) => Promise<boolean>;
  register: (data: RegisterData) => Promise<boolean>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children?: React.ReactNode }) => {
  const [auth, setAuth] = useState<AuthState>({ user: null, isAuthenticated: false, token: null });
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let active = true;
    const token = localStorage.getItem('intellishare_token');
    if (!token) { setIsLoading(false); return; }
    apiRequest<{ user: Parameters<typeof mapUser>[0] }>('/auth/me')
      .then(result => { if (active) setAuth({ user: mapUser(result.user), isAuthenticated: true, token }); })
      .catch(() => { clearSession(); if (active) setAuth({ user: null, isAuthenticated: false, token: null }); })
      .finally(() => { if (active) setIsLoading(false); });
    const expired = () => { if (active) setAuth({ user: null, isAuthenticated: false, token: null }); };
    window.addEventListener('intellishare:session-expired', expired);
    return () => { active = false; window.removeEventListener('intellishare:session-expired', expired); };
  }, []);

  const acceptSession = (result: AuthResult) => {
    saveSession(result.token);
    setAuth({ user: mapUser(result.user), isAuthenticated: true, token: result.token });
  };

  const login = async (email: string, password: string) => {
    const result = await apiRequest<AuthResult>('/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) });
    acceptSession(result);
    return true;
  };

  const register = async (data: RegisterData) => {
    const result = await apiRequest<AuthResult>('/auth/register', {
      method: 'POST', body: JSON.stringify({ name: data.name, email: data.email, password: data.password,
        badgeNumber: data.badgeNumber, agency: data.agency, department: data.department, phone: data.phone }),
    });
    acceptSession(result);
    return true;
  };

  const logout = () => {
    clearSession();
    setAuth({ user: null, isAuthenticated: false, token: null });
  };

  return <AuthContext.Provider value={{ ...auth, isLoading, login, register, logout }}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within an AuthProvider');
  return context;
};
