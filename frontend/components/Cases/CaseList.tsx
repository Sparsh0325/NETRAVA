import React, { useState, useEffect } from 'react';
import { ApiError, deleteCase, fetchCases } from '../../lib/api';
import { Eye, Edit, Share2, Filter, Download, Briefcase, BarChart3, Trash2 } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { useTheme } from '../../contexts/ThemeContext';

const CaseList = () => {
  const [cases, setCases] = useState<import('../../types').Case[]>([]);
  const [filter, setFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('All');
  const [page, setPage] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { preferences } = useTheme();
  const navigate = useNavigate();

  useEffect(() => {
    let active = true;
    setLoading(true);
    fetchCases({ q: filter, status: statusFilter, page, size: 20 })
      .then(result => { if (active) { setCases(result.content); setTotalElements(result.totalElements); setTotalPages(result.totalPages); setError(''); } })
      .catch(err => { if (active) setError(err instanceof ApiError ? err.message : 'Unable to load cases.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [filter, statusFilter, page]);

  const handleDelete = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (window.confirm(`Are you sure you want to permanently delete Case ${id}?`)) {
      try {
        await deleteCase(id);
        setCases(prev => prev.filter(c => c.id !== id));
        setTotalElements(value => Math.max(0, value - 1));
      } catch (err) {
        setError(err instanceof ApiError ? err.message : 'Unable to delete the case.');
      }
    }
  };

  const filteredCases = cases;

  const exportCsv = () => {
    const headers = ['Case ID', 'Title', 'Type', 'Status', 'Priority', 'Date', 'City', 'Agency Officer'];
    const rows = cases.map(c => [c.id, c.title, c.type, c.status, c.priority, c.date, c.location.city, c.assignedOfficer]);
    const csv = [headers, ...rows].map(row => row.map(value => `"${String(value ?? '').replaceAll('"', '""')}"`).join(',')).join('\r\n');
    const link = document.createElement('a');
    link.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    link.download = 'intellishare-cases.csv'; link.click(); URL.revokeObjectURL(link.href);
  };

  const formatDate = (dateString: string) => {
    try {
      const date = new Date(dateString);
      if (preferences.dateFormat.startsWith('DD/MM/YYYY')) {
        return date.toLocaleDateString('en-GB');
      } else if (preferences.dateFormat.startsWith('MM/DD/YYYY')) {
        return date.toLocaleDateString('en-US');
      }
      return dateString; // Default YYYY-MM-DD
    } catch (e) {
      return dateString;
    }
  };

  const tablePadding = preferences.compactTables ? 'py-2' : 'py-4';

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-end border-b border-slate-200 dark:border-slate-700 pb-4">
        <div>
          <h2 className="text-2xl font-bold text-navy-900 dark:text-white tracking-tight">Case Management</h2>
          <p className="text-slate-500 dark:text-slate-400 text-sm mt-1">Manage, track and update ongoing investigations</p>
        </div>
        <button 
          onClick={() => navigate('/ingest', { state: { tab: 'manual' } })}
          className="bg-navy-800 text-white px-5 py-2.5 rounded-md hover:bg-navy-700 transition shadow-sm font-medium text-sm flex items-center gap-2"
        >
          <Briefcase size={16} />
          Create New Case
        </button>
      </div>

      <div className="bg-white dark:bg-slate-800 rounded-lg shadow-sm border border-slate-200 dark:border-slate-700 overflow-hidden">
        {/* Filters */}
        <div className="p-4 border-b border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 flex flex-wrap gap-4 items-center justify-between">
          <div className="flex items-center gap-2 flex-1 min-w-[300px]">
            <input 
              type="text" 
              placeholder="Filter by Case ID, Title, or Officer..." 
              className="w-full border border-slate-300 dark:border-slate-600 rounded-md px-4 py-2 focus:ring-2 focus:ring-navy-600 focus:outline-none text-sm text-slate-800 bg-white"
              value={filter}
                onChange={(e) => { setFilter(e.target.value); setPage(0); }}
            />
          </div>
          <div className="flex items-center gap-3">
            <div className="relative">
              <select 
                className="appearance-none border border-slate-300 dark:border-slate-600 rounded-md pl-4 pr-10 py-2 bg-white dark:bg-slate-800 text-sm text-slate-700 dark:text-slate-300 font-medium focus:outline-none focus:ring-1 focus:ring-navy-600"
                value={statusFilter}
                onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
              >
                <option value="All">All Statuses</option>
                <option value="Registered">Registered</option>
                <option value="Under Investigation">Under Investigation</option>
                <option value="Closed">Closed</option>
                <option value="Cold Case">Cold Case</option>
              </select>
              <div className="pointer-events-none absolute inset-y-0 right-0 flex items-center px-2 text-slate-500">
                <Filter size={14} />
              </div>
            </div>
            
            <button onClick={exportCsv} title="Export the currently displayed page" disabled={!cases.length} className="flex items-center gap-2 px-3 py-2 border border-slate-300 dark:border-slate-600 rounded-md hover:bg-white dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 text-sm font-medium transition bg-slate-50 dark:bg-slate-800 disabled:opacity-50">
              <Download size={16} />
              Export CSV
            </button>
          </div>
        </div>

        {/* Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-100 dark:bg-slate-900 text-navy-900 dark:text-white border-b border-slate-200 dark:border-slate-700 uppercase tracking-wider text-xs font-bold">
              <tr>
                <th className="px-6 py-4">Case ID</th>
                <th className="px-6 py-4">Details</th>
                <th className="px-6 py-4">Date</th>
                <th className="px-6 py-4">Classification</th>
                <th className="px-6 py-4">Jurisdiction</th>
                <th className="px-6 py-4">Current Status</th>
                <th className="px-6 py-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-700">
              {loading && <tr><td colSpan={7} className="px-6 py-12 text-center text-slate-500">Loading cases…</td></tr>}
              {!loading && error && <tr><td colSpan={7} className="px-6 py-12 text-center text-red-600">{error}</td></tr>}
              {!loading && !error && filteredCases.length === 0 && <tr><td colSpan={7} className="px-6 py-12 text-center text-slate-500">No cases found for this search.</td></tr>}
              {filteredCases.map((c, idx) => (
                <tr key={c.id} className={`hover:bg-slate-50 dark:hover:bg-slate-700/50 transition-colors ${idx % 2 === 0 ? 'bg-white dark:bg-slate-800' : 'bg-slate-50/30 dark:bg-slate-800/50'}`}>
                  <td className={`px-6 ${tablePadding}`}>
                    <Link to={`/cases/${c.id}`} className="font-mono text-navy-700 dark:text-blue-400 font-bold hover:underline hover:text-navy-900 dark:hover:text-blue-300">
                      {c.id}
                    </Link>
                  </td>
                  <td className={`px-6 ${tablePadding}`}>
                    <div className="font-bold text-slate-800 dark:text-slate-200">{c.title}</div>
                    <div className="text-xs text-slate-500 dark:text-slate-400 truncate max-w-[200px] mt-0.5">{c.description}</div>
                  </td>
                  <td className={`px-6 ${tablePadding} text-slate-600 dark:text-slate-400`}>
                    {formatDate(c.date)}
                  </td>
                  <td className={`px-6 ${tablePadding}`}>
                    <span className="inline-flex items-center px-2.5 py-0.5 rounded text-xs font-medium border border-slate-200 dark:border-slate-600 bg-white dark:bg-slate-700 text-slate-700 dark:text-slate-300">
                      {c.type}
                    </span>
                  </td>
                  <td className={`px-6 ${tablePadding} text-slate-600 dark:text-slate-400 font-medium`}>{c.location.city}</td>
                  <td className={`px-6 ${tablePadding}`}>
                    <span className={`inline-flex items-center px-2.5 py-1 rounded text-xs font-bold border ${
                      c.status === 'Closed' ? 'bg-emerald-50 dark:bg-emerald-900/20 text-emerald-700 dark:text-emerald-400 border-emerald-200 dark:border-emerald-800' :
                      c.status === 'Under Investigation' ? 'bg-amber-50 dark:bg-amber-900/20 text-amber-700 dark:text-amber-400 border-amber-200 dark:border-amber-800' :
                      'bg-slate-100 dark:bg-slate-700 text-slate-700 dark:text-slate-300 border-slate-200 dark:border-slate-600'
                    }`}>
                      {c.status}
                    </span>
                  </td>
                  <td className={`px-6 ${tablePadding} text-right`}>
                    <div className="flex justify-end gap-1">
                      <button 
                        onClick={() => navigate(`/analytics/${c.id}`)}
                        className="p-1.5 text-slate-400 hover:text-purple-700 dark:hover:text-purple-400 hover:bg-purple-50 dark:hover:bg-slate-700 rounded transition" 
                        title="AI Analytics"
                      >
                         <BarChart3 size={16} />
                      </button>
                      <button 
                        onClick={() => navigate(`/cases/${c.id}`)}
                        className="p-1.5 text-slate-400 hover:text-navy-700 dark:hover:text-blue-400 hover:bg-navy-50 dark:hover:bg-slate-700 rounded transition" 
                        title="View Details"
                      >
                         <Eye size={16} />
                      </button>
                      <button disabled title="Case editing is not connected yet" className="p-1.5 text-slate-400 rounded opacity-50 cursor-not-allowed">
                         <Edit size={16} />
                      </button>
                      <button 
                        onClick={(e) => handleDelete(c.id, e)}
                        className="p-1.5 text-slate-400 hover:text-red-700 dark:hover:text-red-400 hover:bg-red-50 dark:hover:bg-slate-700 rounded transition" 
                        title="Delete Case"
                      >
                         <Trash2 size={16} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        
        {/* Pagination */}
        <div className="p-4 border-t border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900 flex justify-between items-center text-xs text-slate-600 dark:text-slate-400 font-medium">
           <span>Page {totalPages ? page + 1 : 0} of {totalPages} · {totalElements} records</span>
           <div className="flex gap-2">
             <button onClick={() => setPage(p => Math.max(0,p-1))} disabled={page===0||loading} className="px-3 py-1.5 border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 rounded hover:bg-slate-50 dark:hover:bg-slate-700 disabled:opacity-50">Previous</button>
             <button onClick={() => setPage(p => Math.min(totalPages-1,p+1))} disabled={page>=totalPages-1||loading} className="px-3 py-1.5 border border-slate-300 dark:border-slate-600 bg-white dark:bg-slate-800 rounded hover:bg-slate-50 dark:hover:bg-slate-700 disabled:opacity-50">Next</button>
           </div>
        </div>
      </div>
    </div>
  );
};

export default CaseList;
