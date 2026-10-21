import React, { useEffect, useState } from 'react';
import { ApiError, CaseAnalytics, fetchCaseAnalytics } from '../../lib/api';

const TrendingPatterns: React.FC = () => {
  const [analytics, setAnalytics] = useState<CaseAnalytics | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    fetchCaseAnalytics()
      .then((result) => { if (active) setAnalytics(result); })
      .catch((cause) => { if (active) setError(cause instanceof ApiError ? cause.message : 'Could not load saved case totals.'); });
    return () => { active = false; };
  }, []);

  const caseTypes = [...(analytics?.byType ?? [])].sort((a, b) => b.count - a.count).slice(0, 6);

  return (
    <section className="h-full overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm dark:border-slate-700 dark:bg-slate-800">
      <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-900">
        <div>
          <h3 className="text-sm font-bold uppercase tracking-wide text-navy-900 dark:text-white">Saved cases by type</h3>
          <p className="mt-1 text-xs text-slate-500">Counts from your agency’s persisted records</p>
        </div>
        {analytics && <span className="rounded-full bg-blue-100 px-2 py-1 text-[10px] font-bold text-blue-800">{analytics.totalCases} total</span>}
      </div>
      <div className="space-y-3 p-4" aria-live="polite">
        {error ? <p role="alert" className="text-sm text-red-700">{error}</p>
          : !analytics ? <p className="text-sm text-slate-500">Loading saved case totals…</p>
            : caseTypes.length === 0 ? <p className="text-sm text-slate-500">No saved cases yet.</p>
              : caseTypes.map((item) => {
                const percent = analytics.totalCases ? Math.round((item.count / analytics.totalCases) * 100) : 0;
                return <div key={item.label}>
                  <div className="mb-1 flex justify-between text-sm"><span className="font-medium text-slate-700 dark:text-slate-200">{item.label}</span><span className="font-mono text-slate-500">{item.count}</span></div>
                  <div className="h-2 overflow-hidden rounded bg-slate-100 dark:bg-slate-700" role="progressbar" aria-label={`${item.label} share of saved cases`} aria-valuemin={0} aria-valuemax={100} aria-valuenow={percent}>
                    <div className="h-full rounded bg-blue-600" style={{ width: `${percent}%` }} />
                  </div>
                </div>;
              })}
      </div>
    </section>
  );
};

export default TrendingPatterns;
