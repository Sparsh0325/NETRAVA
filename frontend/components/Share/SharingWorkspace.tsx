import React, { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { AlertCircle, Loader, LockKeyhole, Plus, RefreshCw, Share2 } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { ApiError, CaseShare, createCaseShare, fetchShares, revokeCaseShare } from '../../lib/api';

const SharingWorkspace: React.FC = () => {
  const { user } = useAuth();
  const [searchParams] = useSearchParams();
  const [shares,setShares]=useState<CaseShare[]>([]);
  const [caseKey,setCaseKey]=useState(() => searchParams.get('case') || '');
  const [recipient,setRecipient]=useState('');
  const [permission,setPermission]=useState<'READ'|'EDIT'>('READ');
  const [loading,setLoading]=useState(true);
  const [busy,setBusy]=useState(false);
  const [error,setError]=useState('');
  const [message,setMessage]=useState('');

  const load=useCallback(async()=>{setLoading(true);setError('');try{setShares(await fetchShares());}catch(e){setError(e instanceof ApiError?e.message:'Could not load shared cases.');}finally{setLoading(false);}},[]);
  useEffect(()=>{void load();},[load]);

  const create=async(e:React.FormEvent)=>{e.preventDefault();setBusy(true);setError('');setMessage('');try{await createCaseShare(caseKey.trim(),recipient.trim(),permission);setMessage('Case access granted.');setCaseKey('');setRecipient('');await load();}catch(err){setError(err instanceof ApiError?err.message:'Could not share this case.');}finally{setBusy(false);}};
  const revoke=async(id:number)=>{setBusy(true);setError('');setMessage('');try{await revokeCaseShare(id);setMessage('Access revoked.');await load();}catch(err){setError(err instanceof ApiError?err.message:'Could not revoke access.');}finally{setBusy(false);}};
  const canManage=user?.role==='Administrator'||user?.role==='Officer';

  return <div className="space-y-6">
    <header className="border-b border-slate-200 pb-4 dark:border-slate-700"><h2 className="flex items-center gap-2 text-2xl font-bold text-navy-900 dark:text-white"><Share2/> Inter-Agency Case Sharing</h2><p className="mt-1 text-sm text-slate-500">Give another agency time-unlimited read or edit access. Revoke it at any time.</p></header>
    {canManage&&<form onSubmit={create} className="grid gap-4 rounded-xl border border-slate-200 bg-white p-5 md:grid-cols-4 dark:border-slate-700 dark:bg-slate-800">
      <label className="text-xs font-bold text-slate-600 dark:text-slate-300">Case number or ID<input required value={caseKey} onChange={e=>setCaseKey(e.target.value)} className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm font-normal dark:border-slate-600 dark:bg-slate-900" placeholder="CASE-2026-001"/></label>
      <label className="text-xs font-bold text-slate-600 dark:text-slate-300">Recipient agency<input required maxLength={100} value={recipient} onChange={e=>setRecipient(e.target.value)} className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm font-normal dark:border-slate-600 dark:bg-slate-900" placeholder="Agency name"/></label>
      <label className="text-xs font-bold text-slate-600 dark:text-slate-300">Access<select value={permission} onChange={e=>setPermission(e.target.value as 'READ'|'EDIT')} className="mt-1 w-full rounded border border-slate-300 px-3 py-2 text-sm font-normal dark:border-slate-600 dark:bg-slate-900"><option value="READ">Read only</option><option value="EDIT">Edit case</option></select></label>
      <button disabled={busy} className="self-end rounded bg-navy-800 px-4 py-2 text-sm font-bold text-white disabled:opacity-50"><Plus className="mr-1 inline" size={16}/>{busy?'Saving…':'Grant access'}</button>
    </form>}
    <div className="flex items-center justify-between"><h3 className="font-bold text-navy-900 dark:text-white">Shared case access</h3><button onClick={()=>void load()} disabled={loading} className="flex items-center gap-2 text-sm text-navy-700 dark:text-blue-400"><RefreshCw size={15}/>Refresh</button></div>
    {error&&<p role="alert" className="rounded border border-red-200 bg-red-50 p-3 text-sm text-red-700"><AlertCircle className="mr-2 inline" size={16}/>{error}</p>}
    {message&&<p role="status" className="rounded border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-800">{message}</p>}
    {loading?<div className="p-8 text-center text-slate-500"><Loader className="mr-2 inline animate-spin"/>Loading shares…</div>:<div className="overflow-x-auto rounded-lg border border-slate-200 bg-white dark:border-slate-700 dark:bg-slate-800"><table className="w-full text-left text-sm"><thead className="bg-slate-50 text-xs uppercase text-slate-500 dark:bg-slate-900"><tr><th className="p-3">Case</th><th className="p-3">From</th><th className="p-3">To</th><th className="p-3">Permission</th><th className="p-3">Status</th><th className="p-3">Started</th><th className="p-3">Action</th></tr></thead><tbody className="divide-y divide-slate-100 dark:divide-slate-700">{shares.map(s=><tr key={s.id}><td className="p-3"><strong>{s.caseNumber}</strong><span className="block text-xs text-slate-500">{s.caseTitle}</span></td><td className="p-3">{s.ownerAgency}</td><td className="p-3">{s.recipientAgency}</td><td className="p-3">{s.permission==='READ'?'Read only':'Edit'}</td><td className="p-3"><span className={s.status==='ACTIVE'?'text-emerald-700':'text-slate-500'}>{s.status}</span></td><td className="p-3">{new Date(s.createdAt).toLocaleDateString()}</td><td className="p-3">{s.status==='ACTIVE'&&s.ownerAgency===user?.agency&&canManage?<button disabled={busy} onClick={()=>void revoke(s.id)} className="text-xs font-bold text-red-600 disabled:opacity-50">Revoke</button>:<LockKeyhole size={15} className="text-slate-400"/>}</td></tr>)}{shares.length===0&&<tr><td colSpan={7} className="p-8 text-center text-slate-500">No case shares have been created.</td></tr>}</tbody></table></div>}
    <p className="text-xs text-slate-500">Shared cases are limited to the recipient agency. Edit access still requires an Officer or Administrator account. Case reads, grants, and revocations are recorded in the audit history. This screen does not send files or messages.</p>
  </div>;
};

export default SharingWorkspace;
