import React, { useEffect, useMemo, useRef, useState } from 'react';
import * as L from 'leaflet';
import { ApiError, fetchCases } from '../../lib/api';
import { Case } from '../../types';

const CrimeHeatmap: React.FC = () => {
  const mapContainer = useRef<HTMLDivElement>(null);
  const mapRef = useRef<L.Map | null>(null);
  const markersRef = useRef<L.LayerGroup | null>(null);
  const [cases, setCases] = useState<Case[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let active = true;
    fetchCases({ page: 0, size: 100 })
      .then((page) => {
        if (!active) return;
        setCases(page.content);
        setTotal(page.totalElements);
      })
      .catch((cause) => {
        if (active) setError(cause instanceof ApiError ? cause.message : 'Could not load saved case locations.');
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, []);

  const locatedCases = useMemo(() => cases.filter((record) =>
    Number.isFinite(record.location.lat) && Number.isFinite(record.location.lng)
    && !(record.location.lat === 0 && record.location.lng === 0)), [cases]);

  useEffect(() => {
    if (!mapContainer.current || mapRef.current) return;
    const map = L.map(mapContainer.current).setView([22.5937, 78.9629], 5);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(map);
    mapRef.current = map;
    markersRef.current = L.layerGroup().addTo(map);
    return () => {
      map.remove();
      mapRef.current = null;
      markersRef.current = null;
    };
  }, []);

  useEffect(() => {
    const markers = markersRef.current;
    const map = mapRef.current;
    if (!markers || !map) return;
    markers.clearLayers();

    locatedCases.forEach((record) => {
      const color = record.priority === 'High' ? '#ef4444' : record.priority === 'Medium' ? '#3b82f6' : '#64748b';
      const popup = document.createElement('div');
      popup.className = 'p-2';
      const title = document.createElement('strong');
      title.textContent = `${record.caseNumber} · ${record.type}`;
      const details = document.createElement('div');
      details.textContent = `${record.title} · ${record.status} · ${record.priority} priority`;
      popup.append(title, document.createElement('br'), details);

      L.circleMarker([record.location.lat, record.location.lng], {
        radius: record.priority === 'High' ? 9 : 7,
        fillColor: color,
        color: '#fff',
        weight: 1,
        opacity: 1,
        fillOpacity: 0.8,
      }).bindPopup(popup).addTo(markers);
    });

    if (locatedCases.length > 0) {
      const bounds = L.latLngBounds(locatedCases.map((record) => [record.location.lat, record.location.lng] as [number, number]));
      map.fitBounds(bounds, { padding: [24, 24], maxZoom: 10 });
    }
  }, [locatedCases]);

  return (
    <div className="relative h-full w-full overflow-hidden rounded-xl border border-gray-200 shadow-lg">
      <div ref={mapContainer} className="z-0 h-full w-full" />
      <div className="absolute right-4 top-4 z-[400] rounded bg-white p-2 text-xs shadow-md">
        <div className="mb-1 font-bold">Saved case priority</div>
        <div className="flex items-center gap-2"><span className="h-3 w-3 rounded-full bg-red-500" />High</div>
        <div className="flex items-center gap-2"><span className="h-3 w-3 rounded-full bg-blue-500" />Medium</div>
        <div className="flex items-center gap-2"><span className="h-3 w-3 rounded-full bg-slate-500" />Low</div>
      </div>
      <div className="absolute bottom-4 left-4 z-[400] max-w-[80%] rounded bg-white/95 px-3 py-2 text-xs shadow-md" aria-live="polite">
        {loading ? 'Loading saved case locations…' : error ? <span className="text-red-700">{error}</span>
          : locatedCases.length === 0 ? 'No saved cases have map coordinates.'
            : `Showing ${locatedCases.length} geocoded cases from the latest ${cases.length} records${total > cases.length ? ` (${total} accessible cases total)` : ''}.`}
      </div>
    </div>
  );
};

export default CrimeHeatmap;
