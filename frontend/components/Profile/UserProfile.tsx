import React, { useEffect, useState } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { useTheme } from '../../contexts/ThemeContext';
import {
  User, 
  Shield, 
  Settings, 
  Activity, 
  Save, 
  Globe, 
  LogOut,
} from 'lucide-react';
import { ApiError, AuditEvent, downloadMyAuditCsv, fetchMyAuditLogs } from '../../lib/api';

const UserProfile = () => {
  const { user, logout, updateProfile } = useAuth();
  const { preferences, updatePreference } = useTheme();
  const [activeTab, setActiveTab] = useState<'profile' | 'security' | 'preferences' | 'activity'>('profile');
  const [isLoading, setIsLoading] = useState(false);
  const [profileMessage, setProfileMessage] = useState('');
  const [auditEvents, setAuditEvents] = useState<AuditEvent[]>([]);
  const [auditPage, setAuditPage] = useState(0);
  const [auditPages, setAuditPages] = useState(0);
  const [auditLoading, setAuditLoading] = useState(false);
  const [auditError, setAuditError] = useState('');

  const [formData, setFormData] = useState({
    name: user?.name || '',
    email: user?.email || '',
    phone: user?.phone || '',
    department: user?.department || '',
  });

  useEffect(() => {
    setFormData({ name: user?.name || '', email: user?.email || '', phone: user?.phone || '', department: user?.department || '' });
  }, [user?.name, user?.email, user?.phone, user?.department]);

  useEffect(() => {
    if (activeTab !== 'activity') return;
    let active = true;
    setAuditLoading(true);
    setAuditError('');
    fetchMyAuditLogs({ page: auditPage, size: 20 })
      .then((page) => {
        if (!active) return;
        setAuditEvents(page.content);
        setAuditPages(page.totalPages);
      })
      .catch((cause) => { if (active) setAuditError(cause instanceof ApiError ? cause.message : 'Could not load your saved audit history.'); })
      .finally(() => { if (active) setAuditLoading(false); });
    return () => { active = false; };
  }, [activeTab, auditPage]);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setProfileMessage('');
    try {
      await updateProfile({ name: formData.name, phone: formData.phone, department: formData.department });
      setProfileMessage('Profile saved.');
    } catch (cause) {
      setProfileMessage(cause instanceof ApiError ? cause.message : 'Profile could not be saved.');
    } finally { setIsLoading(false); }
  };

  const TabButton = ({ id, label, icon: Icon }: { id: typeof activeTab, label: string, icon: any }) => (
    <button
      onClick={() => setActiveTab(id)}
      className={`w-full flex items-center gap-3 px-4 py-3 text-sm font-medium transition-all duration-200 border-l-[3px] ${
        activeTab === id
          ? 'bg-navy-50 border-navy-800 text-navy-900 shadow-sm dark:bg-navy-900 dark:border-blue-400 dark:text-blue-100'
          : 'border-transparent text-slate-500 hover:bg-slate-50 hover:text-navy-700 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-200'
      }`}
    >
      <Icon size={18} className={activeTab === id ? 'text-navy-700 dark:text-blue-400' : 'text-slate-400'} />
      {label}
    </button>
  );

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      <div className="border-b border-slate-200 dark:border-slate-700 pb-4 flex justify-between items-end">
        <div>
          <h2 className="text-2xl font-bold text-navy-900 dark:text-white tracking-tight">User Profile & Settings</h2>
          <p className="text-slate-500 dark:text-slate-400 text-sm mt-1">Manage your account credentials and system preferences.</p>
        </div>
        <div className="text-xs font-mono text-slate-400 bg-slate-50 dark:bg-slate-800 px-2 py-1 rounded border border-slate-200 dark:border-slate-700">
          ID: {user?.badgeNumber}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
        {/* Left Sidebar Navigation */}
        <div className="lg:col-span-1">
          <div className="bg-white dark:bg-slate-800 rounded-lg border border-slate-200 dark:border-slate-700 shadow-sm overflow-hidden mb-6">
            <div className="p-6 flex flex-col items-center border-b border-slate-100 dark:border-slate-700 bg-slate-50 dark:bg-slate-900">
              <div aria-hidden="true" className="flex h-24 w-24 items-center justify-center rounded-full border-4 border-white bg-navy-800 text-2xl font-bold text-white shadow-md dark:border-slate-700">
                {(user?.name || '?').split(/\s+/).map(part => part[0]).slice(0, 2).join('').toUpperCase()}
              </div>
              <h3 className="mt-4 font-bold text-navy-900 dark:text-white text-lg">{user?.name}</h3>
              <p className="text-slate-500 dark:text-slate-400 text-sm">{user?.role}</p>
              <div className="mt-2 text-xs font-medium px-2 py-1 bg-navy-100 dark:bg-navy-900 text-navy-800 dark:text-blue-200 rounded-full border border-navy-200 dark:border-navy-700">
                {user?.agency}
              </div>
            </div>
            <nav className="py-2">
              <TabButton id="profile" label="Profile Information" icon={User} />
              <TabButton id="security" label="Security & Login" icon={Shield} />
              <TabButton id="preferences" label="System Preferences" icon={Settings} />
              <TabButton id="activity" label="My Activity Log" icon={Activity} />
            </nav>
            <div className="p-4 border-t border-slate-100 dark:border-slate-700">
              <button 
                onClick={logout}
                className="flex items-center gap-2 text-red-600 dark:text-red-400 text-sm font-medium hover:text-red-700 w-full px-2 py-1 rounded hover:bg-red-50 dark:hover:bg-red-900/20 transition"
              >
                <LogOut size={16} /> Sign Out
              </button>
            </div>
          </div>
        </div>

        {/* Right Content Area */}
        <div className="lg:col-span-3">
          <div className="bg-white dark:bg-slate-800 rounded-lg border border-slate-200 dark:border-slate-700 shadow-sm min-h-[600px]">
            
            {/* PROFILE TAB */}
            {activeTab === 'profile' && (
              <div className="p-8 animate-fade-in">
                <div className="flex justify-between items-center mb-6">
                  <h3 className="text-lg font-bold text-navy-900 dark:text-white border-l-4 border-navy-600 pl-3">Personal Information</h3>
                  <span className="text-xs text-slate-400">* Required fields for official records</span>
                </div>
                
                <form onSubmit={handleSave} className="space-y-6 max-w-2xl">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                      <label className="block text-sm font-bold text-slate-700 dark:text-slate-300 mb-1">Full Name</label>
                      <input 
                        type="text"
                        required
                        maxLength={100}
                        value={formData.name}
                        onChange={e => setFormData({...formData, name: e.target.value})}
                        className="w-full border border-slate-300 dark:border-slate-600 rounded-md px-4 py-2 text-sm focus:ring-2 focus:ring-navy-600 focus:outline-none dark:bg-slate-700 dark:text-white"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-bold text-slate-700 dark:text-slate-300 mb-1">Official Email</label>
                      <input 
                        type="email" 
                        value={formData.email}
                        disabled
                        className="w-full border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 text-slate-500 dark:text-slate-400 rounded-md px-4 py-2 text-sm cursor-not-allowed"
                      />
                      <p className="text-[10px] text-slate-400 mt-1">Contact IT admin to change email</p>
                    </div>
                    <div>
                      <label className="block text-sm font-bold text-slate-700 dark:text-slate-300 mb-1">Badge Number</label>
                      <input 
                        type="text" 
                        value={user?.badgeNumber} 
                        disabled 
                        className="w-full border border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 text-slate-500 dark:text-slate-400 rounded-md px-4 py-2 text-sm font-mono cursor-not-allowed"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-bold text-slate-700 dark:text-slate-300 mb-1">Contact Number</label>
                      <input 
                        type="tel"
                        maxLength={50}
                        value={formData.phone}
                        onChange={e => setFormData({...formData, phone: e.target.value})}
                        className="w-full border border-slate-300 dark:border-slate-600 rounded-md px-4 py-2 text-sm focus:ring-2 focus:ring-navy-600 focus:outline-none dark:bg-slate-700 dark:text-white"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-bold text-slate-700 dark:text-slate-300 mb-1">Department</label>
                      <input 
                        type="text" 
                        maxLength={100}
                        value={formData.department}
                        onChange={e => setFormData({...formData, department: e.target.value})}
                        className="w-full border border-slate-300 dark:border-slate-600 rounded-md px-4 py-2 text-sm focus:ring-2 focus:ring-navy-600 focus:outline-none dark:bg-slate-700 dark:text-white"
                      />
                    </div>
                  </div>

                  {profileMessage && <p role="status" className={`text-sm ${profileMessage === 'Profile saved.' ? 'text-emerald-700' : 'text-red-700'}`}>{profileMessage}</p>}

                  <div className="pt-4 border-t border-slate-100 dark:border-slate-700 flex justify-end">
                    <button 
                      type="submit" 
                      disabled={isLoading}
                      className="bg-navy-800 text-white px-6 py-2 rounded-md hover:bg-navy-700 transition shadow-sm font-medium text-sm flex items-center gap-2"
                    >
                      {isLoading ? (
                        <>Saving...</>
                      ) : (
                        <>
                          <Save size={16} /> Save Changes
                        </>
                      )}
                    </button>
                  </div>
                </form>
              </div>
            )}

            {/* SECURITY TAB */}
            {activeTab === 'security' && (
              <div className="p-8 animate-fade-in">
                <div className="flex justify-between items-center mb-6">
                  <h3 className="text-lg font-bold text-navy-900 dark:text-white border-l-4 border-navy-600 pl-3">Security Settings</h3>
                </div>
                <div role="note" className="max-w-3xl space-y-4 rounded-lg border border-amber-300 bg-amber-50 p-5 text-sm text-amber-950 dark:border-amber-800 dark:bg-amber-950/30 dark:text-amber-100">
                  <p><strong>Authentication:</strong> NETRAVA uses password hashing with BCrypt and signed JWT bearer tokens.</p>
                  <p>Password changes, two-factor authentication, and active-session management are not available in this version. The old screen showed sample devices and falsely marked 2FA as enabled; those controls have been removed.</p>
                  <p>Signing out clears the token from this browser. It does not revoke tokens already issued to other sessions.</p>
                </div>
              </div>
            )}

            {/* PREFERENCES TAB */}
            {activeTab === 'preferences' && (
              <div className="p-8 animate-fade-in">
                <div className="flex justify-between items-center mb-6">
                  <h3 className="text-lg font-bold text-navy-900 dark:text-white border-l-4 border-navy-600 pl-3">System Preferences</h3>
                </div>

                <div className="space-y-6 max-w-2xl">
                  <div className="border-b border-slate-100 dark:border-slate-700 pb-6">
                    <label className="block text-sm font-bold text-slate-700 dark:text-slate-300 mb-3">Interface Theme</label>
                    <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                      <div 
                        onClick={() => updatePreference('theme', 'light')}
                        className={`border-2 rounded-lg p-3 cursor-pointer transition-all ${preferences.theme === 'light' ? 'border-navy-600 bg-slate-50 dark:bg-slate-700' : 'border-slate-200 dark:border-slate-700 hover:border-slate-300'}`}
                      >
                        <div className="h-20 bg-white border border-slate-200 rounded mb-2 shadow-sm"></div>
                        <div className="text-center font-bold text-navy-900 dark:text-white text-sm">Light (Gov Standard)</div>
                      </div>
                      <div 
                        onClick={() => updatePreference('theme', 'dark')}
                        className={`border-2 rounded-lg p-3 cursor-pointer transition-all ${preferences.theme === 'dark' ? 'border-navy-600 bg-slate-50 dark:bg-slate-700' : 'border-slate-200 dark:border-slate-700 hover:border-slate-300'}`}
                      >
                        <div className="h-20 bg-navy-900 rounded mb-2 shadow-sm"></div>
                        <div className="text-center font-bold text-slate-600 dark:text-slate-300 text-sm">Dark Mode</div>
                      </div>
                      <div 
                        onClick={() => updatePreference('theme', 'contrast')}
                        className={`border-2 rounded-lg p-3 cursor-pointer transition-all ${preferences.theme === 'contrast' ? 'border-navy-600 bg-slate-50 dark:bg-slate-700' : 'border-slate-200 dark:border-slate-700 hover:border-slate-300'}`}
                      >
                        <div className="h-20 bg-slate-200 rounded mb-2 shadow-sm border border-black"></div>
                        <div className="text-center font-bold text-slate-600 dark:text-slate-300 text-sm">High Contrast</div>
                      </div>
                    </div>
                  </div>

                  <div className="border-b border-slate-100 dark:border-slate-700 pb-6">
                    <label className="block text-sm font-bold text-slate-700 dark:text-slate-300 mb-3">Language & Locale</label>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <span className="text-xs font-bold text-slate-500 dark:text-slate-400 mb-1 block">System Language</span>
                        <div className="relative">
                          <Globe size={16} className="absolute left-3 top-2.5 text-slate-400" />
                          <select 
                            value={preferences.language}
                            onChange={(e) => updatePreference('language', e.target.value)}
                            className="w-full border border-slate-300 dark:border-slate-600 rounded-md pl-10 pr-4 py-2 text-sm focus:ring-2 focus:ring-navy-600 dark:bg-slate-700 dark:text-white"
                          >
                            <option>English (UK)</option>
                            <option>Hindi</option>
                            <option>Marathi</option>
                          </select>
                        </div>
                      </div>
                      <div>
                        <span className="text-xs font-bold text-slate-500 dark:text-slate-400 mb-1 block">Date Format</span>
                         <select 
                           value={preferences.dateFormat}
                           onChange={(e) => updatePreference('dateFormat', e.target.value)}
                           className="w-full border border-slate-300 dark:border-slate-600 rounded-md px-4 py-2 text-sm focus:ring-2 focus:ring-navy-600 dark:bg-slate-700 dark:text-white"
                         >
                            <option>DD/MM/YYYY (Indian Standard)</option>
                            <option>MM/DD/YYYY</option>
                            <option>YYYY-MM-DD</option>
                          </select>
                      </div>
                    </div>
                  </div>

                  <div>
                     <label className="block text-sm font-bold text-slate-700 dark:text-slate-300 mb-3">Dashboard Customization</label>
                     <div className="space-y-2">
                       <label className="flex items-center gap-2 text-sm text-slate-700 dark:text-slate-300 cursor-pointer select-none">
                         <input 
                           type="checkbox" 
                           checked={preferences.showQuickStats} 
                           onChange={(e) => updatePreference('showQuickStats', e.target.checked)}
                           className="rounded text-navy-600 focus:ring-navy-600" 
                         />
                         Show Quick Stats Widget
                       </label>
                       <label className="flex items-center gap-2 text-sm text-slate-700 dark:text-slate-300 cursor-pointer select-none">
                         <input 
                           type="checkbox" 
                           checked={preferences.realTimeMap} 
                           onChange={(e) => updatePreference('realTimeMap', e.target.checked)}
                           className="rounded text-navy-600 focus:ring-navy-600" 
                         />
                         Show saved case locations on dashboard
                       </label>
                       <label className="flex items-center gap-2 text-sm text-slate-700 dark:text-slate-300 cursor-pointer select-none">
                         <input 
                           type="checkbox" 
                           checked={preferences.compactTables} 
                           onChange={(e) => updatePreference('compactTables', e.target.checked)}
                           className="rounded text-navy-600 focus:ring-navy-600" 
                         />
                         Compact View for Tables
                       </label>
                     </div>
                  </div>
                </div>
              </div>
            )}

            {/* ACTIVITY TAB */}
            {activeTab === 'activity' && (
              <div className="p-8 animate-fade-in">
                <div className="flex justify-between items-center mb-6">
                  <h3 className="text-lg font-bold text-navy-900 dark:text-white border-l-4 border-navy-600 pl-3">Recent Activity History</h3>
                  <button onClick={() => { setAuditError(''); void downloadMyAuditCsv().catch(cause => setAuditError(cause instanceof ApiError ? cause.message : 'Audit export failed.')); }} disabled={auditLoading} className="text-xs font-bold text-navy-600 hover:underline disabled:opacity-50 dark:text-blue-400">Export My Audit</button>
                </div>

                <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">Persisted actions recorded for your badge number. Search history and page views are not recorded.</p>
                {auditError && <p role="alert" className="mb-4 text-sm text-red-700">{auditError}</p>}
                {auditLoading ? <p className="py-8 text-center text-sm text-slate-500">Loading your audit history…</p>
                  : auditEvents.length === 0 ? <p className="py-8 text-center text-sm text-slate-500">No audit events are saved for your account yet.</p>
                    : <div className="relative ml-3 space-y-5 border-l-2 border-slate-200 py-2 pl-6 dark:border-slate-700">{auditEvents.map(event => <article key={event.id} className="relative">
                      <div className="absolute -left-[31px] top-1 h-4 w-4 rounded-full border-2 border-white bg-navy-500 shadow-sm dark:border-slate-800" />
                      <p className="text-sm font-bold text-slate-800 dark:text-slate-200">{event.action.replaceAll('_', ' ')}</p>
                      <p className="mt-0.5 text-xs text-slate-500 dark:text-slate-400">{event.resource} · {new Date(event.timestamp).toLocaleString()}</p>
                      {event.details && <p className="mt-1 text-xs text-slate-600 dark:text-slate-300">{event.details}</p>}
                    </article>)}</div>}
                <div className="mt-5 flex items-center justify-between border-t border-slate-100 pt-4 dark:border-slate-700">
                  <button onClick={() => setAuditPage(page => Math.max(0, page - 1))} disabled={auditPage === 0 || auditLoading} className="text-xs font-bold text-navy-600 disabled:opacity-40 dark:text-blue-400">Newer</button>
                  <span className="text-xs text-slate-500">{auditPages === 0 ? 'No pages' : `Page ${auditPage + 1} of ${auditPages}`}</span>
                  <button onClick={() => setAuditPage(page => Math.min(Math.max(0, auditPages - 1), page + 1))} disabled={auditPage + 1 >= auditPages || auditLoading} className="text-xs font-bold text-navy-600 disabled:opacity-40 dark:text-blue-400">Older</button>
                </div>
              </div>
            )}
            
          </div>
        </div>
      </div>
    </div>
  );
};

export default UserProfile;
