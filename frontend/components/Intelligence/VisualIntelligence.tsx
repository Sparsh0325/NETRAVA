import React from 'react';
import { Info, ScanFace, ShieldAlert } from 'lucide-react';

const VisualIntelligence: React.FC = () => (
  <div className="space-y-6">
    <header className="flex flex-wrap items-end justify-between gap-4 border-b border-slate-200 pb-4 dark:border-slate-700">
      <div>
        <h2 className="flex items-center gap-2 text-2xl font-bold tracking-tight text-navy-900 dark:text-white">
          <ScanFace className="text-blue-600" /> Visual Intelligence
        </h2>
        <p className="mt-1 text-sm text-slate-500 dark:text-slate-400">
          Synthetic demonstration information only.
        </p>
      </div>
      <span className="rounded border border-amber-300 bg-amber-100 px-2 py-1 text-[10px] font-bold uppercase text-amber-800">
        Identification unavailable
      </span>
    </header>

    <section role="note" className="rounded-xl border border-amber-300 bg-amber-50 p-5 text-amber-950 dark:border-amber-800 dark:bg-amber-950/30 dark:text-amber-100">
      <div className="flex items-start gap-3">
        <ShieldAlert className="mt-0.5 shrink-0 text-amber-700 dark:text-amber-300" />
        <div>
          <h3 className="font-bold">No facial identification is performed</h3>
          <p className="mt-2 text-sm leading-6">
            This demonstration does not accept or process photos, access a watchlist, identify people, or produce suspect matches or confidence scores. Earlier simulated match behavior has been removed.
          </p>
        </div>
      </div>
    </section>

    <section className="rounded-xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
      <div className="mb-5 flex items-center gap-2 text-sm font-semibold text-slate-700 dark:text-slate-200">
        <Info size={18} className="text-blue-600" /> Demonstration status
      </div>
      <div className="grid gap-4 sm:grid-cols-3">
        {[
          ['Photo upload', 'Disabled'],
          ['Identity matching', 'Unavailable'],
          ['Synthetic match results', 'Not generated'],
        ].map(([label, value]) => (
          <div key={label} className="rounded-lg border border-slate-200 bg-slate-50 p-4 dark:border-slate-700 dark:bg-slate-900">
            <p className="text-xs uppercase tracking-wide text-slate-500">{label}</p>
            <p className="mt-2 font-semibold text-slate-800 dark:text-slate-100">{value}</p>
          </div>
        ))}
      </div>
      <p className="mt-5 text-xs leading-5 text-slate-500 dark:text-slate-400">
        The original IntelliShare visual-analysis page is retained as a safety notice. The app has no real-world face recognition or suspect identification feature.
      </p>
    </section>
  </div>
);

export default VisualIntelligence;
