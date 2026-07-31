import { useCallback, useEffect, useState } from "react";
import type { User } from "firebase/auth";
import { Check, LoaderCircle, LogOut, RefreshCw, Truck } from "lucide-react";
import { googleSignIn, initAuth, logout } from "./auth";
import { getTodaySheet, updateCompleted } from "./sheets";
import type { DailySheet, Load } from "./types";

function localDateStamp(date = new Date()) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function messageFor(error: unknown) {
  return error instanceof Error ? error.message : "Something went wrong. Please try again.";
}

export default function App() {
  const [user, setUser] = useState<User | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [sheet, setSheet] = useState<DailySheet | null>(null);
  const [loading, setLoading] = useState(true);
  const [signingIn, setSigningIn] = useState(false);
  const [savingRows, setSavingRows] = useState<Set<number>>(new Set());
  const [error, setError] = useState<string | null>(null);
  const [date, setDate] = useState(localDateStamp);

  const loadToday = useCallback(async (token: string) => {
    const currentDate = localDateStamp();
    setDate(currentDate);
    setLoading(true);
    setError(null);
    try { setSheet(await getTodaySheet(token, currentDate)); }
    catch (err) { setSheet(null); setError(messageFor(err)); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => initAuth(
    (currentUser, token) => { setUser(currentUser); setAccessToken(token); void loadToday(token); },
    () => setLoading(false),
  ), [loadToday]);

  const signIn = async () => {
    setSigningIn(true); setError(null);
    try {
      const result = await googleSignIn();
      if (result) { setUser(result.user); setAccessToken(result.accessToken); await loadToday(result.accessToken); }
    } catch (err) { setError(messageFor(err)); }
    finally { setSigningIn(false); }
  };

  const signOut = async () => {
    await logout(); setUser(null); setAccessToken(null); setSheet(null); setError(null);
  };

  const toggleCompleted = async (load: Load) => {
    if (!accessToken || !sheet || savingRows.has(load.rowNumber)) return;
    const completed = !load.completed;
    setSheet({ ...sheet, loads: sheet.loads.map((item) => item.rowNumber === load.rowNumber ? { ...item, completed } : item) });
    setSavingRows((rows) => new Set(rows).add(load.rowNumber)); setError(null);
    try { await updateCompleted(accessToken, sheet, load.rowNumber, completed); }
    catch (err) {
      setSheet((current) => current ? { ...current, loads: current.loads.map((item) => item.rowNumber === load.rowNumber ? { ...item, completed: load.completed } : item) } : current);
      setError(messageFor(err));
    } finally {
      setSavingRows((rows) => { const next = new Set(rows); next.delete(load.rowNumber); return next; });
    }
  };

  if (!user || !accessToken) {
    return <main className="sign-in-page"><section className="sign-in-card">
      <div className="brand-mark"><Truck aria-hidden="true" /></div><p className="eyebrow">Jenkins Daily</p>
      <h1>Today’s loads, ready to roll.</h1><p className="intro">Sign in with your configured Google account to view and update today’s load sheet.</p>
      {error && <div className="error" role="alert">{error}</div>}
      <button className="primary-button" onClick={signIn} disabled={signingIn}>{signingIn ? <LoaderCircle className="spin" /> : <GoogleIcon />}{signingIn ? "Signing in…" : "Sign in with Google"}</button>
    </section></main>;
  }

  return <main className="app-shell">
    <header className="topbar"><div><p className="eyebrow">Jenkins Daily</p><h1>Today’s loads</h1><p className="date">{date}</p></div>
      <div className="header-actions"><button className="icon-button" onClick={() => loadToday(accessToken)} disabled={loading || savingRows.size > 0} aria-label="Refresh today’s loads"><RefreshCw className={loading ? "spin" : ""} /></button><button className="icon-button" onClick={signOut} aria-label="Sign out"><LogOut /></button></div>
    </header>
    {error && <div className="error content-error" role="alert">{error}</div>}
    {loading ? <div className="state-panel"><LoaderCircle className="spin" /><p>Loading today’s sheet…</p></div>
      : sheet && sheet.loads.length > 0 ? <section className="load-list" aria-label="Today’s vehicle loads">
        <div className="list-summary"><span>{sheet.loads.length} {sheet.loads.length === 1 ? "vehicle" : "vehicles"}</span><span>{sheet.loads.filter((load) => load.completed).length} completed</span></div>
        {sheet.loads.map((load) => { const saving = savingRows.has(load.rowNumber); return <article className={`load-card${load.completed ? " completed" : ""}`} key={load.rowNumber}>
          <label className="complete-control"><input type="checkbox" checked={load.completed} onChange={() => void toggleCompleted(load)} disabled={saving} /><span className="checkbox" aria-hidden="true">{saving ? <LoaderCircle className="spin" /> : <Check />}</span><span>{saving ? "Saving…" : "Completed"}</span></label>
          <div className="load-content"><h2>{load.vehicleDetails || "Vehicle details unavailable"}</h2><p className="vin">VIN {load.vin || "—"}</p><dl><Detail label="Origin" value={load.origin} /><Detail label="Destination" value={load.destination} /><Detail label="Drivetrain" value={load.drivetrain} /><Detail label="EPB" value={load.epb} /><Detail label="Notes" value={load.notes} wide /></dl></div>
        </article>; })}
      </section> : sheet ? <div className="state-panel"><Truck /><h2>No vehicle rows yet</h2><p>Sheet1 is available, but it doesn’t contain any loads.</p></div> : null}
  </main>;
}

function Detail({ label, value, wide = false }: { label: string; value: string; wide?: boolean }) { return <div className={wide ? "wide" : ""}><dt>{label}</dt><dd>{value || "—"}</dd></div>; }
function GoogleIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path fill="#4285F4" d="M21.6 12.23c0-.71-.06-1.4-.18-2.07H12v3.92h5.38a4.6 4.6 0 0 1-2 3.02v2.54h3.24c1.9-1.74 2.98-4.31 2.98-7.41Z"/><path fill="#34A853" d="M12 22c2.7 0 4.97-.9 6.62-2.36l-3.24-2.54c-.9.6-2.05.96-3.38.96-2.6 0-4.81-1.76-5.6-4.13H3.06v2.62A10 10 0 0 0 12 22Z"/><path fill="#FBBC05" d="M6.4 13.93A6.02 6.02 0 0 1 6.08 12c0-.67.11-1.32.32-1.93V7.45H3.06A10 10 0 0 0 2 12c0 1.61.39 3.14 1.06 4.55l3.34-2.62Z"/><path fill="#EA4335" d="M12 5.94c1.47 0 2.79.5 3.83 1.5l2.87-2.87A9.64 9.64 0 0 0 12 2a10 10 0 0 0-8.94 5.45l3.34 2.62c.79-2.37 3-4.13 5.6-4.13Z"/></svg>; }
