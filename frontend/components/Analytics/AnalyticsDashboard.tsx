import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Activity, AlertTriangle, ArrowLeft, BarChart3, BrainCircuit, FileClock, FileText, GitMerge, Network, ShieldAlert } from 'lucide-react';
import { Case } from '../../types';
import { ApiError, CaseAnalytics, fetchCase, fetchCaseAnalytics } from '../../lib/api';

const cardClass = 'rounded-lg border border-slate-200 bg-white p-5 dark:border-slate-700 dark:bg-slate-800';

const AnalyticsDashboard: React.FC = () => {
  const { id } = useParams();
  const [activeTab, setActiveTab] = useState('pattern');
  const [caseData, setCaseData] = useState<Case | null>(null);
  const [analytics, setAnalytics] = useState<CaseAnalytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    setCaseData(null);
    setAnalytics(null);
    const request = id ? fetchCase(id) : fetchCaseAnalytics();
    request
      .then((result) => {
        if (!active) return;
        if (id) setCaseData(result as Case);
        else setAnalytics(result as CaseAnalytics);
      })
      .catch((cause) => {
        if (active) setError(cause instanceof ApiError ? cause.message : 'Could not load saved case data.');
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [id]);

  if (loading) return <div className="p-6 text-sm text-slate-500">Loading saved case data…</div>;
  if (error) return <div role="alert" className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>;

  if (!id && analytics) {
    const sections = [
      ['Cases by status', analytics.byStatus],
      ['Cases by priority', analytics.byPriority],
      ['Cases by type', analytics.byType],
      ['Cases by incident month', analytics.byMonth],
    ] as const;
    return <div className="space-y-6">
      <header><h2 className="text-2xl font-bold text-navy-900 dark:text-white">Case Analytics</h2><p className="mt-1 text-sm text-slate-500">Counts calculated from saved database records in your agency.</p></header>
      <div className={cardClass}><p className="text-sm text-slate-500">Saved cases</p><p className="text-3xl font-bold text-navy-900 dark:text-white">{analytics.totalCases}</p></div>
      <div className="grid gap-5 lg:grid-cols-2">{sections.map(([title, values]) => <section key={title} className={cardClass}>
        <h3 className="mb-4 font-bold text-navy-900 dark:text-white">{title}</h3>
        {values.length ? <ul className="space-y-3">{values.map((item) => <li key={item.label} className="flex justify-between border-b border-slate-100 pb-2 text-sm last:border-0 dark:border-slate-700"><span>{item.label}</span><strong>{item.count}</strong></li>)}</ul> : <p className="text-sm text-slate-500">No saved values yet.</p>}
      </section>)}</div>
    </div>;
  }

  if (id && caseData) {
    const location = [caseData.location.address, caseData.location.city].filter(Boolean).join(', ');
    const metrics = [
      ['Linked entities', caseData.entities.length],
      ['Evidence records', caseData.evidence?.length ?? 0],
      ['Timeline events', caseData.timeline?.length ?? 0],
    ];
    const tabs = [
      { id: 'pattern', label: 'Pattern Detection', icon: BarChart3 },
      { id: 'link', label: 'Link Analysis', icon: GitMerge },
      { id: 'continuity', label: 'Continuity Board', icon: BrainCircuit },
      { id: 'predictive', label: 'Predictive & Anomalies', icon: Activity },
      { id: 'sentiment', label: 'Threat & Sentiment', icon: AlertTriangle },
    ];
    return <div className="space-y-6">
      <header className="border-b border-slate-200 pb-4 dark:border-slate-700">
        <Link to={`/cases/${encodeURIComponent(caseData.id)}`} className="mb-3 inline-flex items-center gap-2 text-sm text-slate-500 hover:text-blue-600"><ArrowLeft size={16} /> Back to case</Link>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div><p className="font-mono text-xs font-bold text-slate-500">CASE {caseData.caseNumber}</p><h2 className="mt-1 text-2xl font-bold text-navy-900 dark:text-white">{caseData.title}</h2><p className="mt-1 text-sm text-slate-500">Saved case record · {caseData.type}{location ? ` · ${location}` : ''}</p></div>
          <div className="rounded border border-slate-200 px-3 py-1 text-xs font-bold uppercase text-slate-700 dark:border-slate-600 dark:text-slate-200">{caseData.priority} priority · {caseData.status}</div>
        </div>
      </header>

      <div role="note" className="flex items-start gap-3 rounded-lg border border-amber-300 bg-amber-50 p-4 text-sm text-amber-950 dark:border-amber-800 dark:bg-amber-950/30 dark:text-amber-100">
        <ShieldAlert className="mt-0.5 shrink-0" size={18} />
        <p>These counts and records come from this saved case. NETRAVA does not generate case-specific predictions, identity matches, sentiment findings, or unverified links.</p>
      </div>

      <div className="flex overflow-x-auto border-b border-slate-200 dark:border-slate-700">{tabs.map(({ id: tabId, label, icon: Icon }) => <button key={tabId} onClick={() => setActiveTab(tabId)} className={`flex shrink-0 items-center gap-2 border-b-2 px-4 py-2 text-sm font-medium ${activeTab === tabId ? 'border-blue-600 text-navy-900 dark:text-white' : 'border-transparent text-slate-500 hover:text-navy-700 dark:text-slate-400'}`}><Icon size={16} />{label}</button>)}</div>

      {activeTab === 'pattern' && <div className="grid gap-4 sm:grid-cols-3">{metrics.map(([label, value]) => <div className={cardClass} key={label}><p className="text-sm text-slate-500">{label}</p><p className="mt-1 text-3xl font-bold text-navy-900 dark:text-white">{value}</p></div>)}</div>}

      {activeTab === 'link' && <section className={cardClass}><h3 className="mb-2 flex items-center gap-2 font-bold text-navy-900 dark:text-white"><Network size={17} /> Recorded case entities</h3><p className="mb-4 text-sm text-slate-500">These values were saved with the case. No links or identities are inferred.</p>
        {caseData.entities.length ? <ul className="grid gap-3 sm:grid-cols-2">{caseData.entities.map((entity) => <li key={entity.id} className="rounded border border-slate-200 p-3 dark:border-slate-700"><p className="text-xs font-bold uppercase text-slate-500">{entity.type}</p><p className="mt-1 font-medium">{entity.value}</p>{entity.details && <p className="mt-1 text-sm text-slate-500">{entity.details}</p>}</li>)}</ul> : <p className="text-sm text-slate-500">No entities are saved for this case.</p>}
      </section>}

      {activeTab === 'continuity' && <div className="grid gap-5 lg:grid-cols-2">
        <section className={cardClass}><h3 className="mb-4 flex items-center gap-2 font-bold text-navy-900 dark:text-white"><FileText size={17} /> Evidence records</h3>
          {caseData.evidence?.length ? <ul className="space-y-3">{caseData.evidence.map((item) => <li key={item.id} className="border-b border-slate-100 pb-3 last:border-0 dark:border-slate-700"><p className="font-medium">{item.title}</p><p className="mt-1 text-xs text-slate-500">{item.fileType || 'Type not recorded'} · {item.createdAt ? new Date(item.createdAt).toLocaleString() : 'Date unavailable'}</p></li>)}</ul> : <p className="text-sm text-slate-500">No evidence records are saved for this case.</p>}
        </section>
        <section className={cardClass}><h3 className="mb-4 flex items-center gap-2 font-bold text-navy-900 dark:text-white"><FileClock size={17} /> Recorded timeline</h3>
          {caseData.timeline?.length ? <ol className="space-y-3">{caseData.timeline.map((event) => <li key={event.id} className="border-b border-slate-100 pb-3 last:border-0 dark:border-slate-700"><p className="font-medium">{event.action}</p><p className="mt-1 text-xs text-slate-500">{event.timestamp ? new Date(event.timestamp).toLocaleString() : 'Date unavailable'}{event.performedBy ? ` · ${event.performedBy}` : ''}</p>{event.details && <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">{event.details}</p>}</li>)}</ol> : <p className="text-sm text-slate-500">No timeline events are saved for this case.</p>}
        </section>
      </div>}
      {activeTab === 'predictive' && <section role="status" className={cardClass}><h3 className="mb-2 font-bold text-navy-900 dark:text-white">Predictive analysis unavailable</h3><p className="text-sm text-slate-500">No validated forecasting or anomaly model is configured. No risk predictions are shown.</p></section>}
      {activeTab === 'sentiment' && <section role="status" className={cardClass}><h3 className="mb-2 font-bold text-navy-900 dark:text-white">Threat and sentiment analysis unavailable</h3><p className="text-sm text-slate-500">No external social feeds or sentiment service is connected. No sample posts or findings are presented as live intelligence.</p></section>}
    </div>;
  }

  return <div role="status" className="p-6 text-sm text-slate-500">No saved analytics are available.</div>;
};

export default AnalyticsDashboard;
