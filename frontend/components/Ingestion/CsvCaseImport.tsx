import React, { useRef, useState } from 'react';
import { AlertCircle, CheckCircle, FileText, Loader, UploadCloud } from 'lucide-react';
import { ApiError, uploadCaseCsv } from '../../lib/api';

const CsvCaseImport: React.FC = () => {
  const input = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [errors, setErrors] = useState<string[]>([]);
  const [job, setJob] = useState('');

  const submit = async () => {
    if (!file) return;
    setBusy(true); setMessage(''); setErrors([]); setJob('');
    try {
      const result = await uploadCaseCsv(file);
      setJob(result.jobId); setMessage(`${result.processedRows} cases imported from ${file.name}.`); setFile(null);
    } catch (cause) {
      if (cause instanceof ApiError && cause.payload && typeof cause.payload === 'object') {
        const payload = cause.payload as { jobId?: string; errors?: string[]; message?: string };
        setJob(payload.jobId || ''); setErrors(payload.errors || []); setMessage(payload.message || cause.message);
      } else setMessage(cause instanceof Error ? cause.message : 'Import failed. The file was not accepted.');
    } finally { setBusy(false); }
  };

  return <div className="grid gap-8 lg:grid-cols-3">
    <div className="space-y-5 lg:col-span-2">
      <button type="button" onClick={() => input.current?.click()} onDragOver={e => e.preventDefault()} onDrop={e => { e.preventDefault(); setFile(e.dataTransfer.files[0] || null); }}
        className="w-full rounded-xl border-2 border-dashed border-slate-300 p-10 text-center hover:border-navy-500 hover:bg-slate-50">
        <UploadCloud size={34} className="mx-auto mb-4 text-navy-600" />
        <span className="block font-bold text-navy-900">Choose a case CSV file</span>
        <span className="mt-2 block text-sm text-slate-500">Only CSV imports are supported. Maximum file size: 2 MB.</span>
      </button>
      <input ref={input} type="file" accept=".csv,text/csv" className="hidden" onChange={e => setFile(e.target.files?.[0] || null)} />
      {file && <div className="flex items-center gap-3 rounded border border-slate-200 p-3"><FileText size={18}/><span className="flex-1 text-sm">{file.name} · {(file.size/1024).toFixed(1)} KB</span><button onClick={() => setFile(null)} className="text-sm text-red-600">Remove</button></div>}
      <div className="rounded-lg border border-blue-100 bg-blue-50 p-4 text-sm text-blue-900"><strong>Required columns in this order:</strong><br/><code>caseNumber,title,type,status,priority,incidentDate,city,address,description</code><br/><span>Dates use YYYY-MM-DD. Status: Registered, Under Investigation, Closed, Cold Case. Priority: High, Medium, Low. The import uses your account’s agency.</span></div>
      {message && <div role="status" className="rounded border border-slate-200 p-3 text-sm">{job && <span className="mr-2 font-mono text-xs">Job {job}</span>}{message}</div>}
      {errors.length > 0 && <div role="alert" className="rounded border border-red-200 bg-red-50 p-3 text-sm text-red-800"><p className="mb-2 font-bold">Import rejected. No cases were created.</p><ul className="list-inside list-disc">{errors.map((error,index)=><li key={index}>{error}</li>)}</ul></div>}
    </div>
    <aside className="h-fit rounded-xl border border-slate-200 bg-white p-6"><h3 className="mb-3 font-bold text-navy-900">Ingestion status</h3><p className="mb-4 text-sm text-slate-500">Files are parsed in memory. The original file is not saved. A job record is saved for successful and validation-failed imports.</p><button onClick={submit} disabled={!file || busy} className="w-full rounded bg-navy-800 py-3 font-bold text-white disabled:opacity-50">{busy ? <><Loader className="mr-2 inline animate-spin" size={16}/>Processing CSV…</> : 'Validate and import'}</button></aside>
  </div>;
};

export default CsvCaseImport;
