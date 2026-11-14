import React, { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate, useSearchParams, Link } from 'react-router-dom';
import { 
  ArrowLeft, 
  Share2, 
  ArrowRight,
  GitMerge,
  BrainCircuit,
  FileText,
  Image as ImageIcon,
  Film,
  Download,
  Eye,
  XCircle,
  ShieldAlert,
  Sparkles,
  History,
  Edit3,
  Link2
} from 'lucide-react';
import { Case } from '../../types';
import { ApiError, fetchCase, fetchCases, updateCase } from '../../lib/api';
import { useAuth } from '../../contexts/AuthContext';
import * as L from 'leaflet';

interface CaseEditValues {
  title: string;
  description: string;
  type: string;
  status: Case['status'];
  priority: Case['priority'];
  incidentDate: string;
  address: string;
  city: string;
  latitude: string;
  longitude: string;
}

const caseEditValues = (record: Case): CaseEditValues => ({
  title: record.title,
  description: record.description || '',
  type: record.type,
  status: record.status,
  priority: record.priority,
  incidentDate: record.date || '',
  address: record.location.address || '',
  city: record.location.city || '',
  latitude: record.location.lat ? String(record.location.lat) : '',
  longitude: record.location.lng ? String(record.location.lng) : '',
});

const CaseDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const canEditCase = user?.role === 'Administrator' || user?.role === 'Officer';
  const [searchParams, setSearchParams] = useSearchParams();
  const editRequested = searchParams.get('edit') === '1';
  const [caseData, setCaseData] = useState<Case | null>(null);
  const [relatedCases, setRelatedCases] = useState<Case[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [activeTab, setActiveTab] = useState('overview');
  const [editOpen, setEditOpen] = useState(false);
  const [editValues, setEditValues] = useState<CaseEditValues | null>(null);
  const [editError, setEditError] = useState('');
  const [editSaving, setEditSaving] = useState(false);

  const mapRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let active = true;
    if (!id) { setLoading(false); return; }
    setLoading(true); setLoadError('');
    fetchCase(id).then(found => {
      if (!active) return;
      setCaseData(found);
      return fetchCases({ type: found.type, size: 10 }).then(page => {
        if (active) setRelatedCases(page.content.filter(item => item.id !== found.id).slice(0, 3));
      });
    }).catch(error => { if (active) setLoadError(error instanceof ApiError ? error.message : 'Unable to load this case.'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [id]);

  useEffect(() => {
    if (!caseData || !editRequested || !canEditCase) return;
    setEditValues(caseEditValues(caseData));
    setEditError('');
    setEditOpen(true);
    setSearchParams(params => {
      params.delete('edit');
      return params;
    }, { replace: true });
  }, [caseData, editRequested, canEditCase, setSearchParams]);

  const openEdit = () => {
    if (!caseData) return;
    setEditValues(caseEditValues(caseData));
    setEditError('');
    setEditOpen(true);
  };

  const saveCase = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!caseData || !editValues) return;
    setEditSaving(true);
    setEditError('');
    const coordinate = (value: string) => value.trim() ? Number(value) : undefined;
    try {
      const saved = await updateCase(caseData.id, {
        ...editValues,
        incidentDate: editValues.incidentDate || undefined,
        latitude: coordinate(editValues.latitude),
        longitude: coordinate(editValues.longitude),
      });
      setCaseData(saved);
      setEditOpen(false);
    } catch (error) {
      setEditError(error instanceof ApiError ? error.message : 'Could not save case changes.');
    } finally {
      setEditSaving(false);
    }
  };

  // Leaflet Map Init for Overview
  useEffect(() => {
    if (activeTab === 'overview' && caseData && mapRef.current && (caseData.location.lat !== 0 || caseData.location.lng !== 0)) {
        const container = L.DomUtil.get(mapRef.current);
        if(container != null){
          // @ts-ignore
          container._leaflet_id = null; 
        }

        const map = L.map(mapRef.current).setView([caseData.location.lat, caseData.location.lng], 13);
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);
        
        const icon = L.divIcon({
            className: 'bg-transparent',
            html: `<div class="w-8 h-8 bg-red-500/30 rounded-full flex items-center justify-center animate-pulse"><div class="w-4 h-4 bg-red-600 rounded-full border-2 border-white shadow-lg"></div></div>`
        });

        const popupContent = document.createElement('div');
        const caseLabel = document.createElement('strong');
        caseLabel.textContent = caseData.id;
        const address = document.createElement('div');
        address.textContent = caseData.location.address;
        popupContent.append(caseLabel, document.createElement('br'), address);

        L.marker([caseData.location.lat, caseData.location.lng], { icon }).addTo(map)
         .bindPopup(popupContent).openPopup();
         
        return () => { map.remove(); }
    }
  }, [activeTab, caseData]);

  if (loading) {
    return <div className="p-10 text-center text-slate-500">Loading case…</div>;
  }

  if (!caseData) {
    return (
      <div className="flex flex-col items-center justify-center h-96 text-slate-400">
        <p>{loadError || 'Case not found or access denied.'}</p>
        <button onClick={() => navigate('/cases')} className="mt-4 text-navy-600 font-bold hover:underline">Return to Case List</button>
      </div>
    );
  }

  const timelineEvents = (caseData.timeline || []).map(event => ({
    date: new Date(event.timestamp).toLocaleString(), time: '', title: event.action,
    type: 'event', user: event.performedBy || 'System'
  }));
  const evidenceFiles = caseData.evidence || [];
  const personCount = caseData.entities.filter(entity => entity.type === 'Person').length;

  return (
    <div className="space-y-6 animate-fade-in relative">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-200 dark:border-slate-700 pb-4">
        <div>
          <div className="flex items-center gap-2 mb-2">
            <Link to="/cases" className="text-slate-400 hover:text-navy-600 dark:hover:text-blue-400 transition flex items-center gap-1 text-xs font-bold uppercase">
              <ArrowLeft size={14} /> Back to Registry
            </Link>
            <span className="text-slate-300 dark:text-slate-600">|</span>
            <span className="text-xs font-mono font-bold text-slate-500 dark:text-slate-400">{caseData.id}</span>
          </div>
          <h2 className="text-2xl font-bold text-navy-900 dark:text-white tracking-tight flex items-center gap-2">
            {caseData.title}
            <span className={`text-xs px-2 py-1 rounded-full border ${
               caseData.status === 'Closed' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-amber-50 text-amber-700 border-amber-200'
            }`}>
              {caseData.status}
            </span>
          </h2>
        </div>
        
        <div className="flex gap-2">
          <button 
             onClick={() => navigate(`/analytics/${caseData.id}`)}
             className="bg-purple-600 text-white px-4 py-2 rounded-md font-bold text-sm hover:bg-purple-700 transition shadow-sm flex items-center gap-2"
          >
            <BrainCircuit size={16} /> AI Analysis
          </button>
          {canEditCase && <button onClick={openEdit} className="bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-600 text-slate-700 dark:text-slate-200 px-4 py-2 rounded-md font-bold text-sm hover:bg-slate-50 dark:hover:bg-slate-700 transition shadow-sm flex items-center gap-2">
            <Edit3 size={16} /> Edit Case
          </button>}
          {canEditCase && <button
            onClick={() => navigate(`/share?case=${encodeURIComponent(caseData.id)}`)}
            className="bg-navy-800 text-white px-4 py-2 rounded-md font-bold text-sm hover:bg-navy-700 transition shadow-sm flex items-center gap-2"
            title="Continue in the Inter-Agency workspace"
          >
            <Share2 size={16} /> Share Case
          </button>}
        </div>
      </div>

      {editOpen && editValues && <div className="fixed inset-0 z-[1000] flex items-center justify-center bg-slate-950/60 p-4" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget && !editSaving) setEditOpen(false); }}>
        <section role="dialog" aria-modal="true" aria-labelledby="edit-case-title" className="w-full max-w-2xl max-h-[90vh] overflow-y-auto rounded-xl bg-white p-6 shadow-2xl dark:bg-slate-800">
          <div className="mb-5 flex items-start justify-between gap-4">
            <div><h3 id="edit-case-title" className="text-lg font-bold text-navy-900 dark:text-white">Edit case</h3><p className="text-sm text-slate-500">Changes are saved to the case record and audit history.</p></div>
            <button type="button" aria-label="Close edit case" disabled={editSaving} onClick={() => setEditOpen(false)} className="rounded p-1 text-slate-500 hover:bg-slate-100 disabled:opacity-50 dark:hover:bg-slate-700"><XCircle size={20}/></button>
          </div>
          <form onSubmit={saveCase} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="space-y-1 text-sm font-medium text-slate-700 dark:text-slate-200">Title<input required maxLength={255} value={editValues.title} onChange={event => setEditValues({...editValues,title:event.target.value})} className="w-full rounded border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900"/></label>
              <label className="space-y-1 text-sm font-medium text-slate-700 dark:text-slate-200">Case type<input required maxLength={100} value={editValues.type} onChange={event => setEditValues({...editValues,type:event.target.value})} className="w-full rounded border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900"/></label>
              <label className="space-y-1 text-sm font-medium text-slate-700 dark:text-slate-200">Status<select value={editValues.status} onChange={event => setEditValues({...editValues,status:event.target.value as Case['status']})} className="w-full rounded border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900"><option>Registered</option><option>Under Investigation</option><option>Closed</option><option>Cold Case</option></select></label>
              <label className="space-y-1 text-sm font-medium text-slate-700 dark:text-slate-200">Priority<select value={editValues.priority} onChange={event => setEditValues({...editValues,priority:event.target.value as Case['priority']})} className="w-full rounded border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900"><option>High</option><option>Medium</option><option>Low</option></select></label>
              <label className="space-y-1 text-sm font-medium text-slate-700 dark:text-slate-200">Incident date<input type="date" value={editValues.incidentDate} onChange={event => setEditValues({...editValues,incidentDate:event.target.value})} className="w-full rounded border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900"/></label>
              <label className="space-y-1 text-sm font-medium text-slate-700 dark:text-slate-200">City<input maxLength={100} value={editValues.city} onChange={event => setEditValues({...editValues,city:event.target.value})} className="w-full rounded border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900"/></label>
              <label className="space-y-1 text-sm font-medium text-slate-700 dark:text-slate-200 sm:col-span-2">Address<input maxLength={255} value={editValues.address} onChange={event => setEditValues({...editValues,address:event.target.value})} className="w-full rounded border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900"/></label>
              <label className="space-y-1 text-sm font-medium text-slate-700 dark:text-slate-200 sm:col-span-2">Description<textarea maxLength={20000} rows={4} value={editValues.description} onChange={event => setEditValues({...editValues,description:event.target.value})} className="w-full rounded border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900"/></label>
              <label className="space-y-1 text-sm font-medium text-slate-700 dark:text-slate-200">Latitude<input type="number" min={-90} max={90} step="any" value={editValues.latitude} onChange={event => setEditValues({...editValues,latitude:event.target.value})} className="w-full rounded border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900"/></label>
              <label className="space-y-1 text-sm font-medium text-slate-700 dark:text-slate-200">Longitude<input type="number" min={-180} max={180} step="any" value={editValues.longitude} onChange={event => setEditValues({...editValues,longitude:event.target.value})} className="w-full rounded border border-slate-300 bg-white px-3 py-2 dark:border-slate-600 dark:bg-slate-900"/></label>
            </div>
            {editError && <p role="alert" className="rounded bg-red-50 p-3 text-sm text-red-700 dark:bg-red-950/40 dark:text-red-300">{editError}</p>}
            <div className="flex justify-end gap-3 border-t border-slate-200 pt-4 dark:border-slate-700"><button type="button" disabled={editSaving} onClick={() => setEditOpen(false)} className="rounded border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-600 disabled:opacity-50 dark:border-slate-600 dark:text-slate-200">Cancel</button><button type="submit" disabled={editSaving} className="rounded bg-navy-800 px-4 py-2 text-sm font-bold text-white disabled:opacity-50">{editSaving ? 'Saving…' : 'Save changes'}</button></div>
          </form>
        </section>
      </div>}

      {/* Tabs */}
      <div className="flex border-b border-slate-200 dark:border-slate-700 overflow-x-auto">
        {/* REMOVED CONTINUITY TAB as requested */}
        {['Overview', 'Evidence', 'Timeline', 'Related'].map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab.toLowerCase())}
            className={`px-6 py-3 text-sm font-bold border-b-2 transition-colors whitespace-nowrap ${
              activeTab === tab.toLowerCase() 
                ? 'border-navy-600 text-navy-900 dark:text-white dark:border-blue-400' 
                : 'border-transparent text-slate-500 hover:text-navy-700 dark:hover:text-slate-300'
            }`}
          >
            {tab}
          </button>
        ))}
      </div>

      {/* Content */}
      <div className="min-h-[500px]">
        
        {activeTab === 'overview' && (
          <div className="space-y-6">
             {/* 1. AI Brief Banner */}
             <div className="bg-gradient-to-r from-slate-50 to-white dark:from-slate-800 dark:to-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg p-4 transition-all">
                <div className="flex justify-between items-start">
                    <div className="flex items-center gap-3">
                        <div className="p-2 bg-purple-100 dark:bg-purple-900/30 text-purple-600 dark:text-purple-300 rounded-lg">
                            <Sparkles size={20} />
                        </div>
                        <div>
                            <h3 className="font-bold text-navy-900 dark:text-white text-sm">AI Case Intelligent Brief</h3>
                            <p className="text-xs text-slate-500 dark:text-slate-400">
                                AI brief generation will be available after a server-side AI provider is configured.
                            </p>
                        </div>
                    </div>
                </div>
             </div>

             {/* 2. Investigation Summary Card */}
             <div className="bg-white dark:bg-slate-800 rounded-lg border border-slate-200 dark:border-slate-700 p-6 shadow-sm">
                <h3 className="text-sm font-bold text-navy-900 dark:text-white flex items-center gap-2 mb-3">
                   <FileText size={16} className="text-blue-500" /> Investigation Summary
                </h3>
                <p className="text-slate-600 dark:text-slate-300 text-sm leading-relaxed mb-8">
                   {caseData.description || 'No investigation description has been recorded.'}
                </p>
                <div className="grid grid-cols-3 gap-4 border-t border-slate-100 dark:border-slate-700 pt-6">
                   <div className="text-center">
                      <div className="text-2xl font-bold text-navy-900 dark:text-white">{personCount}</div>
                      <div className="text-[10px] text-slate-500 uppercase font-bold tracking-wider mt-1">Person entities</div>
                   </div>
                   <div className="text-center border-l border-slate-100 dark:border-slate-700">
                      <div className="text-2xl font-bold text-navy-900 dark:text-white">{evidenceFiles.length}</div>
                      <div className="text-[10px] text-slate-500 uppercase font-bold tracking-wider mt-1">Evidence Files</div>
                   </div>
                   <div className="text-center border-l border-slate-100 dark:border-slate-700">
                      <div className="text-2xl font-bold text-navy-900 dark:text-white">{caseData.status}</div>
                      <div className="text-[10px] text-slate-500 uppercase font-bold tracking-wider mt-1">Current status</div>
                   </div>
                </div>
             </div>

             {/* 3. Map & Report Grid */}
             <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
                {/* Map */}
                <div className="lg:col-span-3 bg-white dark:bg-slate-800 rounded-lg border border-slate-200 dark:border-slate-700 overflow-hidden h-[400px] relative">
                   <div className="absolute top-4 left-4 z-[400] bg-white dark:bg-slate-800 px-3 py-1.5 rounded shadow-md border border-slate-200 dark:border-slate-600 flex items-center gap-2">
                      <span className="w-2 h-2 bg-red-500 rounded-full animate-pulse"></span>
                      <span className="text-xs font-bold text-navy-900 dark:text-white">Case Location</span>
                   </div>
                   {caseData.location.lat !== 0 || caseData.location.lng !== 0 ? <div ref={mapRef} className="w-full h-full bg-slate-100 dark:bg-slate-900"></div> : <div className="w-full h-full flex items-center justify-center text-sm text-slate-500">No coordinates recorded for this case.</div>}
                </div>

                {/* Report Editor & History */}
                <div className="lg:col-span-2 flex flex-col gap-4">
                   {/* Editor */}
                   <div className="bg-white dark:bg-slate-800 rounded-lg border border-slate-200 dark:border-slate-700 flex-1 flex flex-col">
                      <div className="p-3 border-b border-slate-100 dark:border-slate-700 flex justify-between items-center bg-slate-50 dark:bg-slate-900/50">
                         <div className="flex gap-2">
                            <button className="p-1 hover:bg-slate-200 rounded text-slate-500"><Edit3 size={14} /></button>
                            <button className="p-1 hover:bg-slate-200 rounded text-slate-500 font-serif font-bold">B</button>
                            <button className="p-1 hover:bg-slate-200 rounded text-slate-500 italic">I</button>
                         </div>
                         <span className="text-[10px] text-slate-500 font-bold">Saved case description</span>
                      </div>
                      <div className="p-4 text-xs text-slate-700 dark:text-slate-300 font-mono leading-relaxed overflow-y-auto max-h-[200px]">
                         <p className="font-bold mb-2">Investigation description</p>
                         <p className="text-slate-500 whitespace-pre-wrap">{caseData.description || 'No description has been recorded.'}</p>
                      </div>
                   </div>

                   {/* Version History */}
                   <div className="bg-white dark:bg-slate-800 rounded-lg border border-slate-200 dark:border-slate-700 p-4">
                      <h4 className="text-xs font-bold text-navy-900 dark:text-white flex items-center gap-2 mb-3">
                         <History size={14} /> Version History
                      </h4>
                      <div className="space-y-3">
                         <div className="flex gap-3">
                            <div className="text-[10px] bg-slate-100 dark:bg-slate-700 px-1.5 py-0.5 rounded h-fit font-bold text-slate-600">v1.2</div>
                            <div>
                               <div className="text-xs font-bold text-navy-900 dark:text-white">Version history</div>
                               <div className="text-[10px] text-slate-400">Not available</div>
                            </div>
                         </div>
                         <div className="flex gap-3">
                            <div className="text-[10px] bg-slate-100 dark:bg-slate-700 px-1.5 py-0.5 rounded h-fit font-bold text-slate-600">v1.1</div>
                            <div>
                               <div className="text-xs font-bold text-navy-900 dark:text-white">Case version history</div>
                               <div className="text-[10px] text-slate-400">Version tracking is not connected.</div>
                            </div>
                         </div>
                      </div>
                   </div>
                </div>
             </div>
          </div>
        )}

        {/* --- EVIDENCE TAB --- */}
        {activeTab === 'evidence' && (
           <div className="min-h-[400px] mt-6">
               <div role="note" className="mb-5 rounded-lg border border-amber-300 bg-amber-50 p-4 text-sm text-amber-900 dark:border-amber-800 dark:bg-amber-950/30 dark:text-amber-200">
                 Case access is controlled by your signed-in account and the server’s case permissions. This screen does not perform separate identity verification. Evidence files are not stored; only evidence metadata is shown here.
               </div>
               <div className="animate-fade-in">
                 <div className="flex justify-between items-center mb-6">
                   <h3 className="text-lg font-bold text-navy-900 dark:text-white flex items-center gap-2">
                      <ShieldAlert size={20} className="text-blue-500" /> Case Evidence Metadata
                   </h3>
                 </div>
                 
                 <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                   {evidenceFiles.length === 0 && <p className="col-span-full text-sm text-slate-500">No evidence metadata has been recorded for this case.</p>}
                   {evidenceFiles.map((file) => {
                     const fileType = file.fileType?.toLowerCase() || '';
                     const kind = fileType.startsWith('video/') ? 'video' : fileType.startsWith('image/') ? 'image' : 'doc';
                     const size = file.fileSizeBytes == null ? 'Metadata only' : `${(file.fileSizeBytes / (1024 * 1024)).toFixed(2)} MB`;
                     return (
                     <div key={file.id} className="bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg p-4 hover:shadow-md transition-shadow group">
                        <div className="flex justify-between items-start mb-3">
                          <div className={`p-3 rounded-lg ${
                            kind === 'video' ? 'bg-red-100 text-red-600' : 
                            kind === 'image' ? 'bg-purple-100 text-purple-600' :
                            'bg-blue-100 text-blue-600'
                          }`}>
                            {kind === 'video' ? <Film size={24} /> : kind === 'image' ? <ImageIcon size={24} /> : <FileText size={24} />}
                          </div>
                          <div className="flex gap-1">
                            <button disabled={!file.fileUrl} title={file.fileUrl ? 'Open evidence' : 'File storage is not connected'} onClick={() => file.fileUrl && window.open(file.fileUrl, '_blank', 'noopener,noreferrer')} className="p-1.5 rounded text-slate-400 disabled:opacity-40"><Eye size={16} /></button>
                            <a aria-disabled={!file.fileUrl} href={file.fileUrl || undefined} download className="p-1.5 rounded text-slate-400 aria-disabled:opacity-40"><Download size={16} /></a>
                          </div>
                        </div>
                        <h4 className="font-bold text-sm text-navy-900 dark:text-white truncate" title={file.title}>{file.title}</h4>
                        <div className="flex justify-between items-center mt-2 text-xs text-slate-500 dark:text-slate-400">
                          <span>{size}</span>
                          <span>{new Date(file.createdAt).toLocaleDateString()}</span>
                        </div>
                     </div>
                   );})}
                 </div>
               </div>
           </div>
        )}

        {/* --- TIMELINE TAB --- */}
        {activeTab === 'timeline' && (
          <div className="py-6 px-4 max-w-3xl mx-auto">
            <h3 className="text-lg font-bold text-navy-900 dark:text-white mb-8 border-b border-slate-200 pb-2">Investigative Chronology</h3>
            <div className="relative border-l-2 border-slate-200 dark:border-slate-700 space-y-8 pl-8">
              {timelineEvents.map((event, idx) => (
                <div key={idx} className="relative group">
                  {/* Dot */}
                  <div className={`absolute -left-[41px] top-1 w-5 h-5 rounded-full border-4 border-white dark:border-slate-800 ${
                    event.type === 'start' ? 'bg-emerald-500' : 
                    event.type === 'evidence' ? 'bg-purple-500' :
                    event.type === 'intel' ? 'bg-blue-500' :
                    event.type === 'legal' ? 'bg-red-500' : 'bg-slate-400'
                  } shadow-sm group-hover:scale-125 transition-transform`}></div>
                  
                  <div className="bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 p-4 rounded-lg shadow-sm hover:shadow-md transition-shadow">
                    <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center mb-2">
                      <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">{event.date} • {event.time}</span>
                      <span className="text-[10px] bg-slate-100 dark:bg-slate-700 px-2 py-0.5 rounded text-slate-600 dark:text-slate-300 font-medium mt-1 sm:mt-0 w-fit">
                        User: {event.user}
                      </span>
                    </div>
                    <h4 className="text-base font-bold text-navy-900 dark:text-white">{event.title}</h4>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* --- RELATED TAB (Redesigned with Connection Map) --- */}
        {activeTab === 'related' && (
           <div className="space-y-6 mt-6">
              
              {/* AI Correlation Analysis Banner */}
              <div className="bg-blue-50 dark:bg-blue-900/10 border border-blue-100 dark:border-blue-800 rounded-lg p-3 flex gap-3">
                 <div className="p-2 bg-blue-100 dark:bg-blue-800 rounded-full h-fit text-blue-600 dark:text-blue-300">
                    <Link2 size={16} className="rotate-45" /> {/* Mock link icon using Link from lucide not available, using GitMerge or simple div */}
                    <GitMerge size={16} />
                 </div>
                 <div>
                   <h4 className="text-sm font-bold text-navy-900 dark:text-white">Related cases by case type</h4>
                    <p className="text-xs text-slate-600 dark:text-slate-300">
                       Showing up to <span className="font-bold text-navy-900 dark:text-white">{relatedCases.length} saved cases</span> with the same case type. No AI correlation has been run.
                    </p>
                 </div>
              </div>

              {/* CONNECTION MAP VISUALIZATION */}
              <div className="bg-white dark:bg-slate-800 rounded-lg border border-slate-200 dark:border-slate-700 p-6 relative overflow-hidden min-h-[400px]">
                 <h3 className="text-xs font-bold text-slate-500 uppercase mb-8">CONNECTION MAP</h3>
                 
                 <div className="relative h-[300px] w-full flex items-center justify-center">
                    
                    {/* SVG Lines */}
                    <svg className="absolute inset-0 w-full h-full pointer-events-none z-0">
                       {relatedCases.map((_, i) => {
                          // Simple radial layout calc
                          const angle = (i / relatedCases.length) * Math.PI + Math.PI; // Semicircle top
                          const x = 50 + 30 * Math.cos(angle); // %
                          const y = 50 + 40 * Math.sin(angle); // %
                          return (
                             <line 
                                key={i}
                                x1="50%" y1="60%" // Center Node Position approx
                                x2={`${i === 0 ? 50 : i === 1 ? 20 : 80}%`} 
                                y2="30%" 
                                stroke="#cbd5e1" 
                                strokeWidth="2" 
                                strokeDasharray="5,5" 
                             />
                          );
                       })}
                    </svg>

                    {/* Center Node (Current Case) */}
                    <div className="absolute top-[60%] left-1/2 transform -translate-x-1/2 -translate-y-1/2 z-10 flex flex-col items-center">
                       <div className="w-16 h-16 bg-navy-900 rounded-full flex items-center justify-center border-4 border-white dark:border-slate-700 shadow-xl">
                          <FileText className="text-white" size={24} />
                       </div>
                       <div className="mt-2 text-center bg-navy-900 text-white text-[10px] font-bold px-2 py-1 rounded">
                          CURRENT<br/>{caseData.id}
                       </div>
                    </div>

                    {/* Related Nodes */}
                    {relatedCases.map((rc, i) => {
                       // Positioning logic for 3 items: Left, Center-Top, Right
                       const positions = [
                          { top: '20%', left: '50%', transform: 'translateX(-50%)' },
                          { top: '30%', left: '20%', transform: 'translateX(0)' },
                          { top: '30%', right: '20%', transform: 'translateX(0)' }
                       ];
                       const pos = positions[i] || positions[0];

                       return (
                          <div 
                             key={rc.id}
                             className="absolute z-10 bg-white dark:bg-slate-700 p-3 rounded-lg border border-slate-200 dark:border-slate-600 shadow-lg w-48 hover:scale-105 transition-transform cursor-pointer"
                             style={pos}
                             onClick={() => navigate(`/cases/${rc.id}`)}
                          >
                             <div className="text-xs font-bold text-navy-700 dark:text-blue-300 mb-1">{rc.id}</div>
                             <div className="text-[10px] text-slate-500 dark:text-slate-400 truncate mb-2">{rc.title}</div>
                             <div className="bg-emerald-100 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-400 text-[10px] font-bold px-2 py-1 rounded text-center">
                                Same case type
                             </div>
                          </div>
                       );
                    })}
                 </div>
              </div>

              {/* Case Relationships List */}
              <div>
                <h3 className="text-sm font-bold text-slate-500 uppercase mb-3">Case Relationships</h3>
                <div className="space-y-3">
                  {relatedCases.length > 0 ? relatedCases.map((relatedCase, idx) => (
                      <div key={relatedCase.id} className="flex items-center justify-between p-4 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg shadow-sm hover:border-navy-300 transition-colors group">
                        {/* Left Side Info */}
                        <div className="flex-1">
                            <div className="flex items-center gap-2 mb-1">
                              <span className="text-xs font-bold text-navy-700 dark:text-blue-400">{relatedCase.id}</span>
                              <span className="text-xs text-slate-400">• {relatedCase.date}</span>
                            </div>
                            <h4 className="font-bold text-navy-900 dark:text-white text-base mb-1">{relatedCase.title}</h4>
                            <div className="flex gap-2">
                               <span className="text-[10px] bg-slate-100 dark:bg-slate-700 px-2 py-0.5 rounded text-slate-600 dark:text-slate-300">{relatedCase.type}</span>
                               <span className="text-[10px] bg-slate-100 dark:bg-slate-700 px-2 py-0.5 rounded text-slate-600 dark:text-slate-300">{relatedCase.location.city}</span>
                            </div>
                        </div>

                        {/* Right Side Match & Action */}
                        <div className="flex items-center gap-4">
                            <div className="bg-slate-50 dark:bg-slate-900 border border-slate-100 dark:border-slate-700 rounded-lg p-2 flex items-center gap-3">
                                <div className="p-1.5 bg-white dark:bg-slate-800 rounded-full border border-slate-200 dark:border-slate-600">
                                   <ShieldAlert size={16} className="text-blue-500" />
                                </div>
                                <div>
                                   <div className="text-xs font-bold text-navy-900 dark:text-white flex items-center gap-2">
                                      Same case type
                                   </div>
                                   <div className="text-[10px] text-slate-500">Similarity analysis is not available.</div>
                                </div>
                            </div>
                            <button 
                                onClick={() => navigate(`/cases/${relatedCase.id}`)}
                                className="p-2 text-slate-400 hover:text-navy-700 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-full transition"
                                title="View Case File"
                            >
                                <ArrowRight size={20} />
                            </button>
                        </div>
                      </div>
                  )) : (
                    <div className="p-8 text-center text-slate-500 italic bg-slate-50 rounded border border-slate-200">No related cases found with similar patterns.</div>
                  )}
                </div>
              </div>
           </div>
        )}

      </div>

    </div>
  );
};

export default CaseDetail;
