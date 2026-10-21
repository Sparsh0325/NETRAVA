import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { BarChart3, GitMerge, AlertTriangle, Activity, ArrowLeft, BrainCircuit } from 'lucide-react';
import PatternDetection from './PatternDetection';
import PredictiveAnalytics from './PredictiveAnalytics';
import LinkAnalysis from './LinkAnalysis';
import SentimentAnalysis from './SentimentAnalysis';
import VisualContinuityBoard from './VisualContinuityBoard';
import { Case } from '../../types';
import { ApiError, CaseAnalytics, fetchCaseAnalytics, fetchCase } from '../../lib/api';

const AnalyticsDashboard = () => {
  const { id } = useParams();
  const [activeTab, setActiveTab] = useState('pattern');
  const [caseData, setCaseData] = useState<Case | null>(null);
  const [analytics, setAnalytics] = useState<CaseAnalytics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true); setError('');
    if (id) fetchCase(id).then(setCaseData).catch(cause => setError(cause instanceof ApiError ? cause.message : 'Could not load the saved case.')).finally(() => setLoading(false));
    else fetchCaseAnalytics().then(setAnalytics).catch(cause => setError(cause instanceof ApiError ? cause.message : 'Could not load case analytics.')).finally(() => setLoading(false));
  }, [id]);

  if (loading) return <div className="p-6 text-sm text-slate-500">Loading saved case data…</div>;
  if (error) return <div role="alert" className="rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>;
  if (!id && analytics) {
    const sections = [ ['Cases by status', analytics.byStatus], ['Cases by priority', analytics.byPriority], ['Cases by type', analytics.byType], ['Cases by incident month', analytics.byMonth] ] as const;
    return <div className="space-y-6"><header><h2 className="text-2xl font-bold text-navy-900 dark:text-white">Case Analytics</h2><p className="mt-1 text-sm text-slate-500">Counts from saved database records in your agency.</p></header>
      <div className="rounded-lg border border-slate-200 bg-white p-5 dark:border-slate-700 dark:bg-slate-800"><p className="text-sm text-slate-500">Saved cases</p><p className="text-3xl font-bold text-navy-900 dark:text-white">{analytics.totalCases}</p></div>
      <div className="grid gap-5 lg:grid-cols-2">{sections.map(([title, values]) => <section key={title} className="rounded-lg border border-slate-200 bg-white p-5 dark:border-slate-700 dark:bg-slate-800"><h3 className="mb-4 font-bold text-navy-900 dark:text-white">{title}</h3>
        {values.length ? <ul className="space-y-3">{values.map(item => <li key={item.label} className="flex justify-between border-b border-slate-100 pb-2 text-sm last:border-0 dark:border-slate-700"><span>{item.label}</span><strong>{item.count}</strong></li>)}</ul> : <p className="text-sm text-slate-500">No saved values yet.</p>}
      </section>)}</div></div>;
  }

  return (
    <div className="space-y-6">
       <div className="border-b border-slate-200 dark:border-slate-700 pb-4">
          <div className="flex justify-between items-start">
            <div>
              {caseData ? (
                <>
                  <div className="flex items-center gap-2 mb-2">
                    <Link to="/dashboard" className="text-slate-400 hover:text-navy-600 dark:hover:text-blue-400 transition">
                      <ArrowLeft size={18} />
                    </Link>
                    <span className="text-xs font-mono font-bold bg-navy-100 text-navy-800 px-2 py-0.5 rounded">
                      ANALYSIS FOR: {caseData.id}
                    </span>
                  </div>
                  <h2 className="text-2xl font-bold text-navy-900 dark:text-white tracking-tight">
                    {caseData.title}
                  </h2>
                  <p className="text-slate-500 dark:text-slate-400 text-sm mt-1">
                    Case details for {caseData.type}.
                  </p>
                </>
              ) : (
                <>
                  <h2 className="text-2xl font-bold text-navy-900 dark:text-white tracking-tight">Global Analytics Engine</h2>
                  <p className="text-slate-500 dark:text-slate-400 text-sm mt-1">Aggregated intelligence processing, pattern recognition, and predictive modeling.</p>
                </>
              )}
            </div>
            {caseData && (
               <div className={`px-3 py-1 rounded border text-xs font-bold uppercase ${
                 caseData.priority === 'High' ? 'bg-red-50 text-red-700 border-red-200' : 'bg-blue-50 text-blue-700 border-blue-200'
               }`}>
                 {caseData.priority} Priority
               </div>
            )}
          </div>
        </div>

      {caseData && <div role="note" className="rounded-lg border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-900 dark:border-amber-800 dark:bg-amber-900/20 dark:text-amber-200">
        Some visual panels below contain synthetic examples. They do not produce verified predictions, identity matches, or intelligence findings.
      </div>}

      <div className="flex border-b border-slate-200 dark:border-slate-700 mb-6 overflow-x-auto">
        <button
          onClick={() => setActiveTab('pattern')}
          className={`px-4 py-2 border-b-2 font-medium text-sm flex items-center gap-2 transition-colors whitespace-nowrap ${
            activeTab === 'pattern' 
              ? 'border-navy-600 text-navy-900 dark:text-white dark:border-blue-400' 
              : 'border-transparent text-slate-500 hover:text-navy-700 dark:text-slate-400 dark:hover:text-slate-200'
          }`}
        >
          <BarChart3 size={16} /> Pattern Detection
        </button>
        <button
          onClick={() => setActiveTab('link')}
          className={`px-4 py-2 border-b-2 font-medium text-sm flex items-center gap-2 transition-colors whitespace-nowrap ${
            activeTab === 'link' 
              ? 'border-navy-600 text-navy-900 dark:text-white dark:border-blue-400' 
              : 'border-transparent text-slate-500 hover:text-navy-700 dark:text-slate-400 dark:hover:text-slate-200'
          }`}
        >
          <GitMerge size={16} /> Link Analysis
        </button>
        <button
          onClick={() => setActiveTab('continuity')}
          className={`px-4 py-2 border-b-2 font-medium text-sm flex items-center gap-2 transition-colors whitespace-nowrap ${
            activeTab === 'continuity' 
              ? 'border-navy-600 text-navy-900 dark:text-white dark:border-blue-400' 
              : 'border-transparent text-slate-500 hover:text-navy-700 dark:text-slate-400 dark:hover:text-slate-200'
          }`}
        >
          <BrainCircuit size={16} /> Continuity Board
        </button>
        <button
          onClick={() => setActiveTab('predictive')}
          className={`px-4 py-2 border-b-2 font-medium text-sm flex items-center gap-2 transition-colors whitespace-nowrap ${
            activeTab === 'predictive' 
              ? 'border-navy-600 text-navy-900 dark:text-white dark:border-blue-400' 
              : 'border-transparent text-slate-500 hover:text-navy-700 dark:text-slate-400 dark:hover:text-slate-200'
          }`}
        >
          <Activity size={16} /> Predictive & Anomalies
        </button>
        <button
          onClick={() => setActiveTab('sentiment')}
          className={`px-4 py-2 border-b-2 font-medium text-sm flex items-center gap-2 transition-colors whitespace-nowrap ${
            activeTab === 'sentiment' 
              ? 'border-navy-600 text-navy-900 dark:text-white dark:border-blue-400' 
              : 'border-transparent text-slate-500 hover:text-navy-700 dark:text-slate-400 dark:hover:text-slate-200'
          }`}
        >
          <AlertTriangle size={16} /> Threat & Sentiment
        </button>
      </div>

      <div className="min-h-[500px]">
        {activeTab === 'pattern' && <PatternDetection caseData={caseData} />}
        {activeTab === 'predictive' && <PredictiveAnalytics />}
        {activeTab === 'link' && <LinkAnalysis caseData={caseData} />}
        {activeTab === 'sentiment' && <SentimentAnalysis />}
        {activeTab === 'continuity' && <VisualContinuityBoard caseData={caseData} />}
      </div>
    </div>
  );
};

export default AnalyticsDashboard;
