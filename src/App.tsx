import React, { useState, useEffect } from "react";
import { 
  initAuth, 
  googleSignIn, 
  logout 
} from "./auth";
import { User } from "firebase/auth";
import { DBEntry, AuditLog } from "./types";
import { 
  FileText, 
  UploadCloud, 
  Loader2, 
  CheckCircle, 
  FileCheck, 
  Search, 
  TrendingUp, 
  DollarSign, 
  Briefcase, 
  Database, 
  RefreshCw, 
  LogOut, 
  User as UserIcon, 
  ArrowRight, 
  AlertTriangle,
  ExternalLink,
  Plus,
  Trash2,
  ListFilter,
  Smartphone,
  Moon,
  Sun,
  Wifi,
  Battery,
  Settings,
  Cpu,
  Layers,
  Monitor,
  Check,
  Clock
} from "lucide-react";
import { motion, AnimatePresence } from "motion/react";

// Accent style utility helper for MD3
const getAccentColors = (theme: "sapphire" | "emerald" | "coral" | "lavender") => {
  switch (theme) {
    case "emerald":
      return {
        brand: "emerald",
        primary: "bg-emerald-600 hover:bg-emerald-500",
        text: "text-emerald-500",
        textLight: "text-emerald-400",
        badge: "bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300",
        pill: "bg-emerald-500/10 border-emerald-500/20 text-emerald-500",
        ring: "focus:ring-emerald-500",
        border: "border-emerald-500/20",
        accentGlow: "shadow-emerald-500/15"
      };
    case "coral":
      return {
        brand: "rose",
        primary: "bg-rose-600 hover:bg-rose-500",
        text: "text-rose-500",
        textLight: "text-rose-400",
        badge: "bg-rose-100 text-rose-800 dark:bg-rose-950/60 dark:text-rose-300",
        pill: "bg-rose-500/10 border-rose-500/20 text-rose-500",
        ring: "focus:ring-rose-500",
        border: "border-rose-500/20",
        accentGlow: "shadow-rose-500/15"
      };
    case "lavender":
      return {
        brand: "purple",
        primary: "bg-purple-600 hover:bg-purple-500",
        text: "text-purple-500",
        textLight: "text-purple-400",
        badge: "bg-purple-100 text-purple-800 dark:bg-purple-950/60 dark:text-purple-300",
        pill: "bg-purple-500/10 border-purple-500/20 text-purple-500",
        ring: "focus:ring-purple-500",
        border: "border-purple-500/20",
        accentGlow: "shadow-purple-500/15"
      };
    case "sapphire":
    default:
      return {
        brand: "indigo",
        primary: "bg-indigo-600 hover:bg-indigo-500",
        text: "text-indigo-500",
        textLight: "text-indigo-400",
        badge: "bg-indigo-100 text-indigo-800 dark:bg-indigo-950/60 dark:text-indigo-300",
        pill: "bg-indigo-500/10 border-indigo-500/20 text-indigo-500",
        ring: "focus:ring-indigo-500",
        border: "border-indigo-500/20",
        accentGlow: "shadow-indigo-500/15"
      };
  }
};

export default function App() {
  // Auth state
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [needsAuth, setNeedsAuth] = useState(false);
  const [isLoggingIn, setIsLoggingIn] = useState(false);

  // App UI states
  const [activeTab, setActiveTab] = useState<"workspace" | "dashboard" | "logs">("workspace");
  const [uploadedFiles, setUploadedFiles] = useState<any[]>([]);
  const [isDragOver, setIsDragOver] = useState(false);

  // Android Simulator and Theme controls
  const [isPhoneFrame, setIsPhoneFrame] = useState(true);
  const [accentTheme, setAccentTheme] = useState<"sapphire" | "emerald" | "coral" | "lavender">("sapphire");
  const [isDark, setIsDark] = useState(true);
  const [simulatedTime, setSimulatedTime] = useState("");

  useEffect(() => {
    const updateTime = () => {
      const now = new Date();
      setSimulatedTime(now.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit", hour12: true }));
    };
    updateTime();
    const interval = setInterval(updateTime, 60000);
    return () => clearInterval(interval);
  }, []);
  
  // Processing & Data states
  const [isProcessing, setIsProcessing] = useState(false);
  const [processingStep, setProcessingStep] = useState(0);
  const [extractedRecords, setExtractedRecords] = useState<any[]>([]);
  const [dateHauled, setDateHauled] = useState<string>(() => {
    return new Date().toISOString().split("T")[0];
  });
  
  // Submission & Save States
  const [isSyncing, setIsSyncing] = useState(false);
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [syncResult, setSyncResult] = useState<any | null>(null);

  // DB Logs & Metrics States
  const [dbEntries, setDbEntries] = useState<DBEntry[]>([]);
  const [dbLogs, setDbLogs] = useState<AuditLog[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [isLoadingDb, setIsLoadingDb] = useState(false);

  // Initialize Auth
  useEffect(() => {
    initAuth(
      (currentUser, cachedToken) => {
        setUser(currentUser);
        setToken(cachedToken);
        setNeedsAuth(false);
      },
      () => {
        setNeedsAuth(true);
      }
    );
    loadDatabase();
  }, []);

  // Fetch metrics and history from local database
  const loadDatabase = async () => {
    setIsLoadingDb(true);
    try {
      const res = await fetch("/api/database");
      if (res.ok) {
        const data = await res.json();
        setDbEntries(data.entries || []);
        setDbLogs(data.logs || []);
      }
    } catch (err) {
      console.error("Failed to load local database logs:", err);
    } finally {
      setIsLoadingDb(false);
    }
  };

  const handleLogin = async () => {
    setIsLoggingIn(true);
    try {
      const result = await googleSignIn();
      if (result) {
        setToken(result.accessToken);
        setUser(result.user);
        setNeedsAuth(false);
      }
    } catch (err) {
      console.error("Login failed:", err);
    } finally {
      setIsLoggingIn(false);
    }
  };

  const handleLogout = async () => {
    await logout();
    setUser(null);
    setToken(null);
    setNeedsAuth(true);
  };

  // Drag & Drop / Upload handlers
  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(true);
  };

  const handleDragLeave = () => {
    setIsDragOver(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    if (e.dataTransfer.files) {
      processFiles(Array.from(e.dataTransfer.files));
    }
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      processFiles(Array.from(e.target.files));
    }
  };

  const processFiles = (fileList: File[]) => {
    const validFiles = fileList.filter(file => file.type === "application/pdf");
    if (validFiles.length < fileList.length) {
      alert("Only PDF files are supported. Non-PDF files have been filtered out.");
    }

    let readCount = 0;
    const loadedFiles: any[] = [];

    if (validFiles.length === 0) return;

    validFiles.forEach((file) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        if (e.target?.result) {
          loadedFiles.push({
            name: file.name,
            size: file.size,
            data: e.target.result as string,
          });
        }
        readCount++;
        if (readCount === validFiles.length) {
          setUploadedFiles((prev) => [...prev, ...loadedFiles]);
        }
      };
      reader.readAsDataURL(file);
    });
  };

  const removeUploadedFile = (index: number) => {
    setUploadedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const clearUploadedFiles = () => {
    setUploadedFiles([]);
  };

  // Run OCR scanning via server side Gemini + NHTSA APIs
  const handleRunOCR = async () => {
    if (uploadedFiles.length === 0) return;
    setIsProcessing(true);
    setProcessingStep(1); // scanning OCR step
    setSyncResult(null);

    // Dynamic step timer simulation for smooth UX
    const stepInterval = setInterval(() => {
      setProcessingStep((prev) => {
        if (prev < 3) return prev + 1;
        clearInterval(stepInterval);
        return prev;
      });
    }, 700);

    try {
      const response = await fetch("/api/ocr", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ files: uploadedFiles }),
      });

      clearInterval(stepInterval);

      if (!response.ok) {
        const err = await response.json();
        throw new Error(err.error || "OCR scanning failed.");
      }

      const data = await response.json();
      
      // Extract records to editable local state, flattening arrays for multipage/multirecord support
      const records: any[] = [];
      if (data && Array.isArray(data.results)) {
        data.results.forEach((r: any) => {
          if (Array.isArray(r.extracted)) {
            r.extracted.forEach((item: any) => {
              records.push({
                ...item,
                filename: r.filename,
              });
            });
          } else if (r.extracted) {
            records.push({
              ...r.extracted,
              filename: r.filename,
            });
          }
        });
      }

      setExtractedRecords(records);
      setProcessingStep(4); // success / verification stage
    } catch (err: any) {
      console.error(err);
      alert(`OCR Processing Error: ${err.message}`);
      setIsProcessing(false);
    }
  };

  // Update specific extracted field inline
  const handleFieldChange = (index: number, field: string, value: any) => {
    setExtractedRecords((prev) => {
      const updated = [...prev];
      updated[index] = { ...updated[index], [field]: value };
      return updated;
    });
  };

  const removeExtractedRecord = (index: number) => {
    setExtractedRecords((prev) => prev.filter((_, i) => i !== index));
  };

  // Submit to Sheets & Create load sheet copy
  const handleConfirmSubmit = async () => {
    if (extractedRecords.length === 0) return;
    setIsSyncing(true);
    setShowConfirmModal(false);

    try {
      const response = await fetch("/api/submit-sheets", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          entries: extractedRecords,
          accessToken: token,
          dateHauled: dateHauled,
        }),
      });

      if (!response.ok) {
        const err = await response.json();
        throw new Error(err.error || "Sheets submission failed.");
      }

      const data = await response.json();
      setSyncResult(data);
      setUploadedFiles([]);
      setExtractedRecords([]);
      setIsProcessing(false);
      loadDatabase(); // reload metrics & history
    } catch (err: any) {
      console.error(err);
      alert(`Sync Error: ${err.message}`);
    } finally {
      setIsSyncing(false);
    }
  };

  // Calculation metrics
  const totalLoads = dbEntries.length;
  const totalEarnings = dbEntries.reduce((sum, entry) => sum + (entry.price || 0), 0);
  const activeClients = Array.from(new Set(dbEntries.map((e) => e.for_client).filter(Boolean))).length;

  const topOrigin = dbEntries.reduce((acc: any, entry) => {
    if (!entry.origin) return acc;
    acc[entry.origin] = (acc[entry.origin] || 0) + 1;
    return acc;
  }, {});
  const sortedOrigins = Object.entries(topOrigin).sort((a: any, b: any) => b[1] - a[1]);
  const primaryRoute = sortedOrigins.length > 0 ? `${sortedOrigins[0][0]}` : "None";

  // Filtered History search
  const filteredEntries = dbEntries.filter((e) => {
    const q = searchQuery.toLowerCase();
    return (
      (e.id && e.id.toLowerCase().includes(q)) ||
      (e.vin && e.vin.toLowerCase().includes(q)) ||
      (e.vehicle_details && e.vehicle_details.toLowerCase().includes(q)) ||
      (e.origin && e.origin.toLowerCase().includes(q)) ||
      (e.destination && e.destination.toLowerCase().includes(q)) ||
      (e.for_client && e.for_client.toLowerCase().includes(q))
    );
  });

  const accent = getAccentColors(accentTheme);

  // Dynamic theme colors for Material Day/Night modes
  const themeClasses = {
    screenBg: isDark ? "bg-[#0c101b]" : "bg-[#f1f3f9]",
    cardBg: isDark ? "bg-[#182032]" : "bg-[#ffffff]",
    inputBg: isDark ? "bg-[#101522]" : "bg-[#f8fafc]",
    border: isDark ? "border-[#2b364a]" : "border-[#e2e8f0]",
    textMain: isDark ? "text-slate-100" : "text-slate-800",
    textMuted: isDark ? "text-slate-400" : "text-slate-500",
    textInverse: isDark ? "text-[#0c101b]" : "text-[#f1f3f9]",
    statusBarBg: isDark ? "bg-[#101522]" : "bg-[#e5e9f0]",
    navbarBg: isDark ? "bg-[#151c2b]" : "bg-[#ffffff]",
    badgeBg: isDark ? "bg-slate-800" : "bg-slate-100",
    hoverBg: isDark ? "hover:bg-[#1f293d]" : "hover:bg-[#f3f4f6]"
  };

  return (
    <div id="root_container" className={`min-h-screen bg-[#07090f] text-slate-100 font-sans flex flex-col justify-center items-center p-0 md:p-6 selection:bg-${accent.brand}-500 selection:text-white transition-colors duration-300`}>
      
      {/* Dynamic Style injection for tailwind color compatibility */}
      <style>{`
        .accent-border { border-color: ${accentTheme === 'sapphire' ? '#6366f1' : accentTheme === 'emerald' ? '#10b981' : accentTheme === 'coral' ? '#f43f5e' : '#a855f7'}30; }
        .accent-text { color: ${accentTheme === 'sapphire' ? '#818cf8' : accentTheme === 'emerald' ? '#34d399' : accentTheme === 'coral' ? '#fb7185' : '#c084fc'}; }
        .accent-bg { background-color: ${accentTheme === 'sapphire' ? '#4f46e5' : accentTheme === 'emerald' ? '#059669' : accentTheme === 'coral' ? '#e11d48' : '#9333ea'}; }
        .accent-bg-hover:hover { background-color: ${accentTheme === 'sapphire' ? '#4338ca' : accentTheme === 'emerald' ? '#047857' : accentTheme === 'coral' ? '#be123c' : '#7e22ce'}; }
        .accent-badge { background-color: ${accentTheme === 'sapphire' ? '#4f46e5' : accentTheme === 'emerald' ? '#059669' : accentTheme === 'coral' ? '#e11d48' : '#9333ea'}15; color: ${accentTheme === 'sapphire' ? '#818cf8' : accentTheme === 'emerald' ? '#34d399' : accentTheme === 'coral' ? '#fb7185' : '#c084fc'}; }
        .accent-pill-active { background-color: ${accentTheme === 'sapphire' ? '#4f46e5' : accentTheme === 'emerald' ? '#059669' : accentTheme === 'coral' ? '#e11d48' : '#9333ea'}25; color: ${accentTheme === 'sapphire' ? '#a5b4fc' : accentTheme === 'emerald' ? '#6ee7b7' : accentTheme === 'coral' ? '#fda4af' : '#d8b4fe'}; }
      `}</style>

      {/* PHYSICAL ANDROID PHONE SIMULATOR (Centered on screen as a dedicated Android app) */}
      <div 
        className="relative mx-auto w-[410px] h-[840px] rounded-[56px] bg-[#000000] border-[14px] border-[#1f1f1f] shadow-[0_25px_60px_-15px_rgba(0,0,0,0.8)] flex flex-col overflow-hidden"
      >
        {/* Physical Device Camera Pinhole */}
        <div className="absolute top-3.5 left-1/2 -translate-x-1/2 w-3.5 h-3.5 bg-[#0d0d0d] rounded-full z-50 border border-neutral-800/40 flex items-center justify-center">
          <div className="w-1 h-1 bg-[#1a1a2e] rounded-full"></div>
        </div>

        {/* Speaker Grille Thin Line */}
        <div className="absolute top-1 left-1/2 -translate-x-1/2 w-16 h-1 bg-neutral-800 rounded-full z-50"></div>

        {/* Side Physical Buttons */}
        <div className="absolute top-32 -left-[16px] w-[3px] h-12 bg-neutral-700 rounded-l-md"></div> {/* Volume Up */}
        <div className="absolute top-48 -left-[16px] w-[3px] h-12 bg-neutral-700 rounded-l-md"></div> {/* Volume Down */}
        <div className="absolute top-36 -right-[16px] w-[3px] h-16 bg-neutral-700 rounded-r-md"></div> {/* Power Button */}

        {/* INSIDE THE PHONE SCREEN */}
        <div className={`flex-1 flex flex-col h-full ${themeClasses.screenBg} text-slate-100 select-none overflow-hidden relative transition-colors duration-300`}>
          
          {/* Android Status Bar */}
          <div className={`h-11 ${themeClasses.statusBarBg} flex items-center justify-between px-6 text-xs font-mono font-semibold tracking-wider ${isDark ? 'text-slate-300' : 'text-slate-700'} z-40 transition-colors duration-300`}>
            <div className="flex items-center gap-2">
              <span>{simulatedTime || "08:12 AM"}</span>
              {user && <span className="w-1.5 h-1.5 bg-green-500 rounded-full inline-block animate-ping"></span>}
            </div>
            <div className="flex items-center gap-2.5">
              <Wifi className="h-3.5 w-3.5" />
              <span className="text-[10px]">LTE</span>
              <div className="flex items-center gap-1">
                <Battery className="h-3.5 w-3.5" />
                <span className="text-[9px]">98%</span>
              </div>
            </div>
          </div>

          {/* Android Core App Container */}
          <div className="flex-1 flex flex-col overflow-y-auto overflow-x-hidden pb-20">
            <AndroidAppContent 
              user={user}
              needsAuth={needsAuth}
              isLoggingIn={isLoggingIn}
              handleLogin={handleLogin}
              handleLogout={handleLogout}
              activeTab={activeTab}
              setActiveTab={setActiveTab}
              uploadedFiles={uploadedFiles}
              removeUploadedFile={removeUploadedFile}
              clearUploadedFiles={clearUploadedFiles}
              handleRunOCR={handleRunOCR}
              isProcessing={isProcessing}
              processingStep={processingStep}
              isDragOver={isDragOver}
              handleDragOver={handleDragOver}
              handleDragLeave={handleDragLeave}
              handleDrop={handleDrop}
              handleFileChange={handleFileChange}
              extractedRecords={extractedRecords}
              setExtractedRecords={setExtractedRecords}
              handleFieldChange={handleFieldChange}
              removeExtractedRecord={removeExtractedRecord}
              dateHauled={dateHauled}
              setDateHauled={setDateHauled}
              setShowConfirmModal={setShowConfirmModal}
              syncResult={syncResult}
              setSyncResult={setSyncResult}
              themeClasses={themeClasses}
              accent={accent}
              isDark={isDark}
              totalLoads={totalLoads}
              totalEarnings={totalEarnings}
              activeClients={activeClients}
              primaryRoute={primaryRoute}
              filteredEntries={filteredEntries}
              isLoadingDb={isLoadingDb}
              searchQuery={searchQuery}
              setSearchQuery={setSearchQuery}
              dbLogs={dbLogs}
              loadDatabase={loadDatabase}
              accentTheme={accentTheme}
              setAccentTheme={setAccentTheme}
              setIsDark={setIsDark}
            />
          </div>

          {/* Android Navigation Bar (Bottom Tabs) */}
          {user && (
            <div className={`absolute bottom-0 inset-x-0 h-16 ${themeClasses.navbarBg} border-t ${themeClasses.border} flex items-center justify-around px-2 z-40 transition-colors duration-300`}>
              <button
                onClick={() => setActiveTab("workspace")}
                className="flex flex-col items-center justify-center flex-1 h-full py-1 text-center cursor-pointer transition-all border-none bg-transparent outline-none"
              >
                <div className={`px-4 py-1 rounded-full mb-1 transition-all ${
                  activeTab === "workspace" 
                    ? "accent-pill-active" 
                    : `${isDark ? "text-slate-400" : "text-slate-500"}`
                }`}>
                  <UploadCloud className="h-5 w-5" />
                </div>
                <span className={`text-[10px] font-bold tracking-tight uppercase ${
                  activeTab === "workspace" 
                    ? "accent-text" 
                    : `${isDark ? "text-slate-400" : "text-slate-500"}`
                }`}>
                  Workspace
                </span>
              </button>

              <button
                onClick={() => setActiveTab("dashboard")}
                className="flex flex-col items-center justify-center flex-1 h-full py-1 text-center cursor-pointer transition-all border-none bg-transparent outline-none"
              >
                <div className={`px-4 py-1 rounded-full mb-1 transition-all ${
                  activeTab === "dashboard" 
                    ? "accent-pill-active" 
                    : `${isDark ? "text-slate-400" : "text-slate-500"}`
                }`}>
                  <TrendingUp className="h-5 w-5" />
                </div>
                <span className={`text-[10px] font-bold tracking-tight uppercase ${
                  activeTab === "dashboard" 
                    ? "accent-text" 
                    : `${isDark ? "text-slate-400" : "text-slate-500"}`
                }`}>
                  Dashboard
                </span>
              </button>

              <button
                onClick={() => setActiveTab("logs")}
                className="flex flex-col items-center justify-center flex-1 h-full py-1 text-center cursor-pointer transition-all border-none bg-transparent outline-none"
              >
                <div className={`px-4 py-1 rounded-full mb-1 transition-all ${
                  activeTab === "logs" 
                    ? "accent-pill-active" 
                    : `${isDark ? "text-slate-400" : "text-slate-500"}`
                }`}>
                  <Clock className="h-5 w-5" />
                </div>
                <span className={`text-[10px] font-bold tracking-tight uppercase ${
                  activeTab === "logs" 
                    ? "accent-text" 
                    : `${isDark ? "text-slate-400" : "text-slate-500"}`
                }`}>
                  Logs
                </span>
              </button>

              <button
                onClick={() => setActiveTab("settings")}
                className="flex flex-col items-center justify-center flex-1 h-full py-1 text-center cursor-pointer transition-all border-none bg-transparent outline-none"
              >
                <div className={`px-4 py-1 rounded-full mb-1 transition-all ${
                  activeTab === "settings" 
                    ? "accent-pill-active" 
                    : `${isDark ? "text-slate-400" : "text-slate-500"}`
                }`}>
                  <Settings className="h-5 w-5" />
                </div>
                <span className={`text-[10px] font-bold tracking-tight uppercase ${
                  activeTab === "settings" 
                    ? "accent-text" 
                    : `${isDark ? "text-slate-400" : "text-slate-500"}`
                }`}>
                  Settings
                </span>
              </button>
            </div>
          )}

          {/* Android System Gestures Pill Bar at bottom center */}
          <div className="absolute bottom-1 left-1/2 -translate-x-1/2 w-32 h-1 bg-neutral-600/60 rounded-full z-50 pointer-events-none"></div>

        </div>
      </div>

      {/* SYSTEM CONFIRMATION MODAL (Designed like native Android dialog box) */}
      <AnimatePresence>
        {showConfirmModal && (
          <div className="fixed inset-0 bg-black/85 backdrop-blur-sm flex items-center justify-center z-[100] p-4 animate-fade-in">
            <motion.div 
              initial={{ scale: 0.92, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.92, opacity: 0 }}
              className={`${themeClasses.cardBg} border ${themeClasses.border} rounded-[28px] max-w-sm w-full p-6 shadow-2xl space-y-5 text-left`}
            >
              <div className="flex items-start gap-3.5">
                <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-2xl text-red-400">
                  <AlertTriangle className="h-6 w-6" />
                </div>
                <div>
                  <h3 className={`text-lg font-bold ${themeClasses.textMain} font-display`}>Sync to Sheets?</h3>
                  <p className="text-xs text-slate-400 mt-1">This writes data directly to your online Google Worksheets.</p>
                </div>
              </div>

              <div className={`space-y-3 ${themeClasses.inputBg} p-4 rounded-2xl border ${themeClasses.border} text-xs ${themeClasses.textMain}`}>
                <div className="flex justify-between">
                  <span className="text-slate-400">Target Template:</span>
                  <span className="font-semibold">Jenkins_Paysheet</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Record Count:</span>
                  <span className="font-semibold accent-text font-mono">Append {extractedRecords.length} load(s)</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400">Haul Date:</span>
                  <span className="font-semibold font-mono">{dateHauled}</span>
                </div>
              </div>

              <div className="flex justify-end gap-3.5">
                <button
                  onClick={() => setShowConfirmModal(false)}
                  className="px-4 py-2.5 text-xs font-semibold text-slate-400 hover:text-slate-200 cursor-pointer border-none bg-transparent"
                >
                  Cancel
                </button>
                <button
                  onClick={handleConfirmSubmit}
                  disabled={isSyncing}
                  className="accent-bg accent-bg-hover text-white font-bold py-2.5 px-5 rounded-full text-xs transition shadow flex items-center gap-1.5 cursor-pointer border-none"
                >
                  {isSyncing ? (
                    <>
                      <Loader2 className="h-3 w-3 animate-spin" />
                      Syncing...
                    </>
                  ) : (
                    <>
                      <span>Write Data</span>
                      <ArrowRight className="h-3.5 w-3.5" />
                    </>
                  )}
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

    </div>
  );
}

// ==========================================
// ANDROID APP CORE VIEW ROUTER COMPONENT
// ==========================================
interface AndroidAppContentProps {
  user: any;
  needsAuth: boolean;
  isLoggingIn: boolean;
  handleLogin: () => void;
  handleLogout: () => void;
  activeTab: "workspace" | "dashboard" | "logs" | "settings";
  setActiveTab: (tab: any) => void;
  uploadedFiles: any[];
  removeUploadedFile: (idx: number) => void;
  clearUploadedFiles: () => void;
  handleRunOCR: () => void;
  isProcessing: boolean;
  processingStep: number;
  isDragOver: boolean;
  handleDragOver: (e: any) => void;
  handleDragLeave: () => void;
  handleDrop: (e: any) => void;
  handleFileChange: (e: any) => void;
  extractedRecords: any[];
  setExtractedRecords: any;
  handleFieldChange: (index: number, field: string, val: any) => void;
  removeExtractedRecord: (idx: number) => void;
  dateHauled: string;
  setDateHauled: (val: string) => void;
  setShowConfirmModal: (val: boolean) => void;
  syncResult: any;
  setSyncResult: (val: any) => void;
  themeClasses: any;
  accent: any;
  isDark: boolean;
  totalLoads: number;
  totalEarnings: number;
  activeClients: number;
  primaryRoute: string;
  filteredEntries: any[];
  isLoadingDb: boolean;
  searchQuery: string;
  setSearchQuery: (val: string) => void;
  dbLogs: any[];
  loadDatabase: () => void;
  isTablet?: boolean;
  accentTheme: "sapphire" | "emerald" | "coral" | "lavender";
  setAccentTheme: (theme: "sapphire" | "emerald" | "coral" | "lavender") => void;
  setIsDark: (dark: boolean) => void;
}

function AndroidAppContent({
  user,
  needsAuth,
  isLoggingIn,
  handleLogin,
  handleLogout,
  activeTab,
  setActiveTab,
  uploadedFiles,
  removeUploadedFile,
  clearUploadedFiles,
  handleRunOCR,
  isProcessing,
  processingStep,
  isDragOver,
  handleDragOver,
  handleDragLeave,
  handleDrop,
  handleFileChange,
  extractedRecords,
  setExtractedRecords,
  handleFieldChange,
  removeExtractedRecord,
  dateHauled,
  setDateHauled,
  setShowConfirmModal,
  syncResult,
  setSyncResult,
  themeClasses,
  accent,
  isDark,
  totalLoads,
  totalEarnings,
  activeClients,
  primaryRoute,
  filteredEntries,
  isLoadingDb,
  searchQuery,
  setSearchQuery,
  dbLogs,
  loadDatabase,
  isTablet = false,
  accentTheme,
  setAccentTheme,
  setIsDark
}: AndroidAppContentProps) {

  // Auth requirement screen
  if (needsAuth) {
    return (
      <div className="p-6 flex flex-col justify-center items-center text-center space-y-6 my-auto">
        <div className="w-16 h-16 bg-indigo-600/10 border border-indigo-500/20 text-indigo-400 rounded-[24px] flex items-center justify-center shadow-inner">
          <Smartphone className="h-8 w-8" />
        </div>
        
        <div className="space-y-2">
          <h2 className={`text-xl font-bold tracking-tight ${themeClasses.textMain} font-display`}>Google Sheets Syncer</h2>
          <p className={`text-xs ${themeClasses.textMuted} leading-relaxed max-w-xs mx-auto`}>
            To access paysheet tables and generate copies of templates, please log in with your Google workspace.
          </p>
        </div>

        <div className={`${themeClasses.inputBg} p-4 rounded-2xl border ${themeClasses.border} text-left w-full max-w-sm space-y-3 text-xs`}>
          <div className="flex gap-2.5 items-start">
            <Check className="h-4 w-4 text-emerald-400 flex-shrink-0 mt-0.5" />
            <span className={themeClasses.textMuted}>Extract payloads from PDF and handwritten sheets</span>
          </div>
          <div className="flex gap-2.5 items-start">
            <Check className="h-4 w-4 text-emerald-400 flex-shrink-0 mt-0.5" />
            <span className={themeClasses.textMuted}>Decode specs instantly with official NHTSA registers</span>
          </div>
          <div className="flex gap-2.5 items-start">
            <Check className="h-4 w-4 text-emerald-400 flex-shrink-0 mt-0.5" />
            <span className={themeClasses.textMuted}>Write verified entries directly into online spreadsheets</span>
          </div>
        </div>

        <button
          onClick={handleLogin}
          disabled={isLoggingIn}
          className="w-full max-w-sm accent-bg accent-bg-hover text-white font-bold py-3 px-6 rounded-full transition shadow flex items-center justify-center gap-2.5 cursor-pointer disabled:opacity-50 border-none"
        >
          {isLoggingIn ? (
            <>
              <Loader2 className="h-4 w-4 animate-spin" />
              <span>Connecting Profile...</span>
            </>
          ) : (
            <>
              <svg className="h-4 w-4 fill-current" viewBox="0 0 24 24">
                <path d="M12.24 10.285V13.4h6.887c-.275 1.565-1.88 4.604-6.887 4.604-4.33 0-7.859-3.578-7.859-8s3.529-8 7.859-8c2.46 0 4.105 1.025 5.047 1.926l2.427-2.334C17.955 2.192 15.34 1 12.24 1 6.033 1 12.24s5.033 11.24 11.24 11.24c6.478 0 10.793-4.537 10.793-10.986 0-.743-.08-1.309-.176-1.864H12.24z"/>
              </svg>
              <span>Connect Google Account</span>
            </>
          )}
        </button>
      </div>
    );
  }

  // Waiting for session
  if (!user) {
    return (
      <div className="p-12 text-center text-slate-400 flex flex-col justify-center items-center my-auto">
        <Loader2 className="h-8 w-8 animate-spin text-indigo-400 mb-3" />
        <span className="text-xs font-semibold">Booting Android OS Core...</span>
      </div>
    );
  }

  // Authenticated Screen Router
  return (
    <div className="p-4 space-y-4">
      
      {/* Top App Bar inside Phone Simulator */}
      {!isTablet && (
        <div className={`flex items-center justify-between p-2.5 rounded-2xl ${themeClasses.cardBg} border ${themeClasses.border} shadow-sm mb-1`}>
          <div className="flex items-center gap-2 truncate">
            {user.photoURL ? (
              <img
                src={user.photoURL}
                alt="Profile"
                className="h-7 w-7 rounded-full object-cover border border-slate-700/50"
                referrerPolicy="no-referrer"
              />
            ) : (
              <div className="h-7 w-7 rounded-full bg-slate-700 flex items-center justify-center text-xs font-bold text-white">
                U
              </div>
            )}
            <div className="truncate">
              <p className={`text-xs font-bold ${themeClasses.textMain} truncate`}>Jenkins Transport</p>
              <p className="text-[9px] text-slate-400 font-mono">com.jenkins.payloadmanager</p>
            </div>
          </div>

          <button
            onClick={handleLogout}
            className="p-1.5 text-slate-400 hover:text-red-400 transition bg-transparent border-none cursor-pointer"
            title="Sign Out"
          >
            <LogOut className="h-3.5 w-3.5" />
          </button>
        </div>
      )}

      {/* SYNC RESULTS OVERLAY BANNER */}
      {syncResult && (
        <motion.div 
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-emerald-500/10 border border-emerald-500/30 p-4 rounded-2xl flex flex-col gap-3 animate-fade-in"
        >
          <div className="flex items-start justify-between gap-2">
            <div className="flex items-start gap-2.5">
              <CheckCircle className="h-5 w-5 text-emerald-400 flex-shrink-0 mt-0.5" />
              <div>
                <h4 className={`text-xs font-bold ${themeClasses.textMain} font-display`}>Worksheet Synced!</h4>
                <p className="text-[10px] text-slate-400">Successfully processed &amp; posted {syncResult.entriesAdded} load records.</p>
              </div>
            </div>
            <button
              onClick={() => setSyncResult(null)}
              className="p-1 text-slate-400 hover:text-slate-200 cursor-pointer font-bold border-none bg-transparent"
              title="Dismiss & New Scan"
            >
              ✕
            </button>
          </div>
          
          <div className="grid grid-cols-2 gap-2">
            <a
              href={syncResult.paysheetLink}
              target="_blank"
              rel="noreferrer"
              className="flex items-center justify-center gap-1 bg-indigo-600/10 hover:bg-indigo-600/25 border border-indigo-500/20 text-indigo-400 font-bold py-1.5 px-3 rounded-lg text-[9px] transition no-underline"
            >
              <span>Paysheet Link</span>
              <ExternalLink className="h-3 w-3" />
            </a>
            <a
              href={syncResult.dailyLoadLink}
              target="_blank"
              rel="noreferrer"
              className="flex items-center justify-center gap-1 bg-emerald-600/15 hover:bg-emerald-600/30 border border-emerald-500/20 text-emerald-400 font-bold py-1.5 px-3 rounded-lg text-[9px] transition no-underline"
            >
              <span>Daily Load Link</span>
              <ExternalLink className="h-3 w-3" />
            </a>
          </div>

          {/* Embedded live view of the Daily Load Sheet */}
          {syncResult.dailyLoadLink && (
            <div className="space-y-1.5 mt-1 border-t border-slate-700/20 pt-2.5">
              <span className="text-[9px] font-bold text-slate-400 uppercase tracking-wider block">Daily Load Sheet Preview</span>
              <div className="w-full h-[280px] rounded-xl border border-slate-700/30 overflow-hidden bg-white shadow-inner relative">
                <iframe
                  src={syncResult.dailyLoadLink.replace(/\/edit(\?.*)?$/, "/htmlembed?headers=false&chrome=false&widget=true")}
                  className="w-full h-full border-none"
                  title="Daily Load Sheet Live Preview"
                />
              </div>
            </div>
          )}

          <button
            onClick={() => setSyncResult(null)}
            className="mt-1 w-full bg-slate-800/40 hover:bg-slate-800/60 text-slate-300 hover:text-slate-100 py-1.5 rounded-lg text-[9px] font-bold transition border border-slate-700/30 cursor-pointer"
          >
            Done &amp; Scan More Loads
          </button>
        </motion.div>
      )}

      {/* VIEW SECTION 1: WORKSPACE */}
      {activeTab === "workspace" && (
        <div className={`space-y-4 ${isTablet ? "grid grid-cols-1 lg:grid-cols-12 gap-6 space-y-0" : ""}`}>
          
          {/* UPLOAD FORM PANEL */}
          <div className={`${isTablet ? "lg:col-span-4" : ""} ${themeClasses.cardBg} border ${themeClasses.border} rounded-2xl p-4 space-y-4`}>
            <div>
              <h3 className={`text-xs font-bold uppercase tracking-wider ${themeClasses.textMuted} font-display`}>
                Step 1: Upload Documents
              </h3>
              <p className="text-[10px] text-slate-400">Choose or drop transport manifest PDFs</p>
            </div>

            <div
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
              className={`border border-dashed rounded-xl p-5 text-center cursor-pointer transition ${
                isDragOver 
                  ? "border-indigo-500 bg-indigo-500/5" 
                  : `${themeClasses.border} hover:border-slate-500 ${themeClasses.inputBg}`
              }`}
            >
              <input
                type="file"
                multiple
                accept="application/pdf"
                onChange={handleFileChange}
                id="pdf_file_input_android"
                className="hidden"
              />
              <label htmlFor="pdf_file_input_android" className="cursor-pointer">
                <UploadCloud className="h-8 w-8 text-indigo-400 mx-auto mb-2 animate-pulse" />
                <span className={`block text-xs font-semibold ${themeClasses.textMain} font-display`}>
                  Choose PDF manifests
                </span>
                <span className="block text-[9px] text-slate-400 mt-1">
                  Supports multiple pages &amp; files
                </span>
              </label>
            </div>

            {uploadedFiles.length > 0 && (
              <div className="space-y-3">
                <div className="flex justify-between items-center text-[10px]">
                  <span className="font-bold text-slate-400 uppercase tracking-wide">
                    Queued Invoices ({uploadedFiles.length})
                  </span>
                  <button
                    onClick={clearUploadedFiles}
                    className="text-red-400 hover:text-red-300 font-bold transition border-none bg-transparent cursor-pointer"
                  >
                    Clear All
                  </button>
                </div>

                <div className="space-y-2 max-h-40 overflow-y-auto pr-1">
                  {uploadedFiles.map((file, i) => (
                    <div key={i} className={`flex items-center justify-between p-2 ${themeClasses.inputBg} rounded-xl border ${themeClasses.border} text-[10px] font-mono`}>
                      <span className={`${themeClasses.textMain} truncate max-w-[180px]`}>{file.name}</span>
                      <div className="flex items-center gap-1.5 flex-shrink-0">
                        <span className="text-[8px] text-slate-500">{(file.size / 1024).toFixed(1)} KB</span>
                        <button
                          onClick={() => removeUploadedFile(i)}
                          className="text-slate-400 hover:text-red-400 transition border-none bg-transparent cursor-pointer"
                        >
                          <Trash2 className="h-3 w-3" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>

                <button
                  onClick={handleRunOCR}
                  disabled={isProcessing}
                  className="w-full accent-bg accent-bg-hover text-white font-semibold py-2.5 px-4 rounded-xl transition text-xs flex items-center justify-center gap-1.5 shadow cursor-pointer border-none"
                >
                  {isProcessing ? (
                    <>
                      <Loader2 className="h-3.5 w-3.5 animate-spin" />
                      Scanning via AI...
                    </>
                  ) : (
                    <>
                      <RefreshCw className="h-3.5 w-3.5" />
                      Run Scanner ({uploadedFiles.length})
                    </>
                  )}
                </button>
              </div>
            )}
          </div>

          {/* ACTIVE OCR STEP PROGRESS DISPLAY */}
          {isProcessing && (
            <motion.div 
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className={`${isTablet ? "lg:col-span-8" : ""} ${themeClasses.cardBg} border ${themeClasses.border} rounded-2xl p-4`}
            >
              <h4 className={`text-xs font-bold uppercase tracking-wider ${themeClasses.textMuted} font-display mb-3`}>
                Scanning Pipeline Active
              </h4>
              <div className="space-y-4">
                <div className="flex items-start gap-2.5">
                  <div className={`p-1 rounded-lg flex-shrink-0 border ${
                    processingStep >= 1 ? "accent-bg text-white border-transparent" : `${themeClasses.inputBg} text-slate-400 ${themeClasses.border}`
                  }`}>
                    {processingStep > 1 ? <Check className="h-3 w-3" /> : <Loader2 className={`h-3 w-3 ${processingStep === 1 ? 'animate-spin' : ''}`} />}
                  </div>
                  <div>
                    <p className={`text-[11px] font-semibold ${themeClasses.textMain}`}>Gemini 3.5 AI Scanning</p>
                    <p className="text-[9px] text-slate-400">Extracting table parameters and handwriting scribbles.</p>
                  </div>
                </div>

                <div className="flex items-start gap-2.5">
                  <div className={`p-1 rounded-lg flex-shrink-0 border ${
                    processingStep >= 2 ? "accent-bg text-white border-transparent" : `${themeClasses.inputBg} text-slate-400 ${themeClasses.border}`
                  }`}>
                    {processingStep > 2 ? <Check className="h-3 w-3" /> : <Loader2 className={`h-3 w-3 ${processingStep === 2 ? 'animate-spin' : ''}`} />}
                  </div>
                  <div>
                    <p className={`text-[11px] font-semibold ${themeClasses.textMain}`}>Federal NHTSA VIN Lookup</p>
                    <p className="text-[9px] text-slate-400">Resolving vehicle drivetrain and electric brake registers.</p>
                  </div>
                </div>

                <div className="flex items-start gap-2.5">
                  <div className={`p-1 rounded-lg flex-shrink-0 border ${
                    processingStep >= 3 ? "accent-bg text-white border-transparent" : `${themeClasses.inputBg} text-slate-400 ${themeClasses.border}`
                  }`}>
                    {processingStep >= 3 ? <Loader2 className="h-3 w-3 animate-spin" /> : <Loader2 className="h-3 w-3" />}
                  </div>
                  <div>
                    <p className={`text-[11px] font-semibold ${themeClasses.textMain}`}>Completing Format</p>
                    <p className="text-[9px] text-slate-400">Consolidating lists and checking for handwritten price overlays.</p>
                  </div>
                </div>
              </div>
            </motion.div>
          )}

          {/* EDITABLE RECORDS EDITOR */}
          {extractedRecords.length > 0 ? (
            <div className={`${isTablet ? "lg:col-span-8" : ""} ${themeClasses.cardBg} border ${themeClasses.border} rounded-2xl p-4 space-y-4`}>
              <div className="flex justify-between items-center">
                <div>
                  <h3 className={`text-xs font-bold uppercase tracking-wider ${themeClasses.textMuted} font-display`}>
                    Step 2: Verification
                  </h3>
                  <p className="text-[10px] text-slate-400">Review, adjust and sign off on data before syncing</p>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-[9px] font-bold text-slate-400">Date:</span>
                  <input
                    type="date"
                    value={dateHauled}
                    onChange={(e) => setDateHauled(e.target.value)}
                    className={`bg-slate-900/10 dark:bg-slate-900/40 text-[10px] font-mono px-2 py-1 rounded-lg border ${themeClasses.border} ${themeClasses.textMain} focus:outline-none`}
                  />
                </div>
              </div>

              <div className="space-y-4 max-h-[380px] overflow-y-auto pr-1">
                {extractedRecords.map((record, index) => (
                  <div key={index} className={`p-3.5 ${themeClasses.inputBg} rounded-2xl border ${themeClasses.border} space-y-3.5 relative`}>
                    
                    {/* Record Header */}
                    <div className="flex justify-between items-start gap-2 border-b border-slate-700/20 pb-2">
                      <div className="truncate">
                        <span className="text-[9px] font-bold tracking-wider uppercase text-slate-400">Manifest #{record.id || "N/A"}</span>
                        <h4 className={`text-xs font-bold ${themeClasses.textMain} truncate`}>{record.vehicle_details || "Unknown Vehicle"}</h4>
                      </div>
                      <button
                        onClick={() => removeExtractedRecord(index)}
                        className="p-1 text-slate-400 hover:text-red-400 transition border-none bg-transparent cursor-pointer"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>

                    {/* Form Fields Grid */}
                    <div className="grid grid-cols-2 gap-2 text-[10px]">
                      <div>
                        <label className="text-[9px] text-slate-400 font-semibold block mb-0.5">Manifest ID</label>
                        <input
                          type="text"
                          value={record.id || ""}
                          onChange={(e) => handleFieldChange(index, "id", e.target.value)}
                          className={`w-full bg-slate-900/10 dark:bg-slate-900/40 p-1.5 rounded-lg border ${themeClasses.border} ${themeClasses.textMain} focus:outline-none`}
                        />
                      </div>

                      <div>
                        <label className="text-[9px] text-slate-400 font-semibold block mb-0.5">Price Quote ($)</label>
                        <input
                          type="number"
                          value={record.price || 0}
                          onChange={(e) => handleFieldChange(index, "price", parseFloat(e.target.value) || 0)}
                          className={`w-full bg-slate-900/10 dark:bg-slate-900/40 p-1.5 rounded-lg border ${themeClasses.border} font-bold text-emerald-400 focus:outline-none`}
                        />
                      </div>

                      <div className="col-span-2">
                        <label className="text-[9px] text-slate-400 font-semibold block mb-0.5">Vehicle Specs &amp; Year</label>
                        <input
                          type="text"
                          value={record.vehicle_details || ""}
                          onChange={(e) => handleFieldChange(index, "vehicle_details", e.target.value)}
                          className={`w-full bg-slate-900/10 dark:bg-slate-900/40 p-1.5 rounded-lg border ${themeClasses.border} ${themeClasses.textMain} focus:outline-none`}
                        />
                      </div>

                      <div className="col-span-2">
                        <label className="text-[9px] text-slate-400 font-semibold block mb-0.5">VIN (17-chars)</label>
                        <input
                          type="text"
                          value={record.vin || ""}
                          onChange={(e) => handleFieldChange(index, "vin", e.target.value)}
                          className={`w-full bg-slate-900/10 dark:bg-slate-900/40 p-1.5 rounded-lg border ${themeClasses.border} ${themeClasses.textMain} font-mono focus:outline-none`}
                        />
                      </div>

                      <div>
                        <label className="text-[9px] text-slate-400 font-semibold block mb-0.5">Origin</label>
                        <input
                          type="text"
                          value={record.origin || ""}
                          onChange={(e) => handleFieldChange(index, "origin", e.target.value)}
                          className={`w-full bg-slate-900/10 dark:bg-slate-900/40 p-1.5 rounded-lg border ${themeClasses.border} ${themeClasses.textMain} focus:outline-none`}
                        />
                      </div>

                      <div>
                        <label className="text-[9px] text-slate-400 font-semibold block mb-0.5">Destination</label>
                        <input
                          type="text"
                          value={record.destination || ""}
                          onChange={(e) => handleFieldChange(index, "destination", e.target.value)}
                          className={`w-full bg-slate-900/10 dark:bg-slate-900/40 p-1.5 rounded-lg border ${themeClasses.border} ${themeClasses.textMain} focus:outline-none`}
                        />
                      </div>

                      <div className="col-span-2">
                        <label className="text-[9px] text-slate-400 font-semibold block mb-0.5">For Client</label>
                        <input
                          type="text"
                          value={record.for_client || ""}
                          onChange={(e) => handleFieldChange(index, "for_client", e.target.value)}
                          className={`w-full bg-slate-900/10 dark:bg-slate-900/40 p-1.5 rounded-lg border ${themeClasses.border} ${themeClasses.textMain} focus:outline-none`}
                        />
                      </div>
                    </div>

                    {/* NHTSA Spec Badges */}
                    <div className="flex gap-2 border-t border-slate-700/15 pt-2 flex-wrap">
                      <span className="text-[8px] font-bold tracking-wider uppercase px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                        Drivetrain: {record.drivetrain || "Unknown"}
                      </span>
                      <span className="text-[8px] font-bold tracking-wider uppercase px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                        EPB Status: {record.epb || "No"}
                      </span>
                      <span className="text-[8px] font-mono text-slate-400 ml-auto">
                        PDF Source page
                      </span>
                    </div>

                  </div>
                ))}
              </div>

              {/* Action Buttons */}
              <div className="flex gap-2.5 pt-2 border-t border-slate-700/15 justify-end">
                <button
                  onClick={() => setExtractedRecords([])}
                  className="px-4 py-2 text-xs font-semibold text-slate-400 hover:text-slate-200 cursor-pointer border-none bg-transparent"
                >
                  Discard
                </button>
                <button
                  onClick={() => setShowConfirmModal(true)}
                  className="accent-bg accent-bg-hover text-white font-bold py-2 px-4 rounded-xl text-xs shadow flex items-center gap-1 cursor-pointer border-none"
                >
                  <CheckCircle className="h-3.5 w-3.5" />
                  <span>Sync Worksheets</span>
                </button>
              </div>

            </div>
          ) : (
            !isProcessing && (
              <div className={`${isTablet ? "lg:col-span-8" : ""} ${themeClasses.cardBg} border ${themeClasses.border} rounded-2xl p-8 text-center flex flex-col justify-center items-center`} style={{ minHeight: "220px" }}>
                <div className={`w-10 h-10 rounded-full ${themeClasses.inputBg} border ${themeClasses.border} text-slate-400 flex items-center justify-center mb-3`}>
                  <UploadCloud className="h-5 w-5" />
                </div>
                <h4 className={`text-xs font-bold ${themeClasses.textMain} mb-1`}>No Manifests Extracted</h4>
                <p className="text-[10px] text-slate-400 max-w-[220px] leading-relaxed mx-auto">
                  Queue up and run OCR scanning on your manifest PDFs. Results will compile here inside the verified scheduler list.
                </p>
              </div>
            )
          )}

        </div>
      )}

      {/* VIEW SECTION 2: DASHBOARD STATS */}
      {activeTab === "dashboard" && (
        <div className="space-y-4">
          
          {/* Stats Grid */}
          <div className="grid grid-cols-2 gap-3">
            <div className={`${themeClasses.cardBg} border ${themeClasses.border} p-3.5 rounded-2xl`}>
              <span className="block text-[8px] font-bold text-slate-400 uppercase tracking-wider">Loads</span>
              <span className={`block text-xl font-black ${themeClasses.textMain} mt-0.5`}>{totalLoads}</span>
              <span className="block text-[8px] text-indigo-400 mt-1 font-semibold">Online database</span>
            </div>
            
            <div className={`${themeClasses.cardBg} border ${themeClasses.border} p-3.5 rounded-2xl`}>
              <span className="block text-[8px] font-bold text-slate-400 uppercase tracking-wider">Earnings</span>
              <span className="block text-xl font-black text-emerald-400 mt-0.5">
                ${totalEarnings.toLocaleString("en-US", { maximumFractionDigits: 0 })}
              </span>
              <span className="block text-[8px] text-emerald-400/80 mt-1 font-semibold">Verified payouts</span>
            </div>

            <div className={`${themeClasses.cardBg} border ${themeClasses.border} p-3.5 rounded-2xl`}>
              <span className="block text-[8px] font-bold text-slate-400 uppercase tracking-wider">Clients</span>
              <span className={`block text-xl font-black ${themeClasses.textMain} mt-0.5`}>{activeClients}</span>
              <span className="block text-[8px] text-slate-400 mt-1">Unique accounts</span>
            </div>

            <div className={`${themeClasses.cardBg} border ${themeClasses.border} p-3.5 rounded-2xl`}>
              <span className="block text-[8px] font-bold text-slate-400 uppercase tracking-wider">Primary route</span>
              <span className="block text-xs font-bold text-indigo-400 mt-1.5 truncate" title={primaryRoute}>{primaryRoute}</span>
              <span className="block text-[8px] text-slate-400 mt-1">High frequency node</span>
            </div>
          </div>

          {/* Search bar & Historic Lists */}
          <div className={`${themeClasses.cardBg} border ${themeClasses.border} rounded-2xl p-4 space-y-4`}>
            <div className="flex flex-col gap-2">
              <h3 className={`text-xs font-bold uppercase tracking-wider ${themeClasses.textMuted} font-display`}>
                Historical Index
              </h3>
              
              {/* Search Field */}
              <div className="relative">
                <Search className="absolute left-3 top-2.5 h-3.5 w-3.5 text-slate-400" />
                <input
                  type="text"
                  placeholder="Search loads, VINs, or Clients..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className={`w-full ${themeClasses.inputBg} border ${themeClasses.border} text-[10px] rounded-xl pl-9 pr-3 py-2 text-slate-100 focus:outline-none focus:ring-1 focus:ring-indigo-500`}
                />
              </div>
            </div>

            {isLoadingDb ? (
              <div className="p-8 text-center text-slate-400 text-[11px]">
                <Loader2 className="h-5 w-5 animate-spin mx-auto mb-2 text-indigo-400" />
                <span>Syncing Database...</span>
              </div>
            ) : filteredEntries.length > 0 ? (
              <div className="space-y-3 max-h-[300px] overflow-y-auto pr-1">
                {filteredEntries.map((e, idx) => (
                  <div key={idx} className={`p-3 rounded-xl border ${themeClasses.border} ${themeClasses.inputBg} space-y-2`}>
                    <div className="flex justify-between items-start text-[10px] border-b border-slate-700/10 pb-1.5">
                      <span className="font-mono text-indigo-400 font-bold">ID: {e.id}</span>
                      <span className="text-[9px] text-slate-400 font-mono">{e.date_hauled}</span>
                    </div>
                    <div className="text-[10px] space-y-1">
                      <p className={`font-bold ${themeClasses.textMain}`}>{e.vehicle_details}</p>
                      <p className="text-[9px] text-slate-400 font-mono truncate">VIN: {e.vin}</p>
                      <div className="flex justify-between items-center text-[9px] pt-1.5 border-t border-slate-700/10">
                        <span className="text-slate-400 truncate max-w-[120px]">{e.origin} &rarr; {e.destination}</span>
                        <span className="font-bold text-emerald-400">${(e.price || 0).toFixed(2)}</span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="p-8 text-center text-slate-400 text-[10px]">
                No matches found for "{searchQuery}".
              </div>
            )}
          </div>

        </div>
      )}

      {/* VIEW SECTION 3: SYSTEM AUDIT LOGS */}
      {activeTab === "logs" && (
        <div className={`${themeClasses.cardBg} border ${themeClasses.border} rounded-2xl p-4 space-y-4`}>
          <div className="flex items-center justify-between border-b border-slate-700/20 pb-2.5">
            <div>
              <h3 className={`text-xs font-bold uppercase tracking-wider ${themeClasses.textMuted} font-display`}>
                Real-Time Operations Log
              </h3>
              <p className="text-[9px] text-slate-400">Database audits for syncing &amp; parsing</p>
            </div>
            <button 
              onClick={loadDatabase}
              className="p-1.5 text-slate-400 hover:text-indigo-400 transition cursor-pointer border-none bg-transparent"
              title="Refresh Logs"
            >
              <RefreshCw className="h-3.5 w-3.5" />
            </button>
          </div>

          <div className="space-y-3 max-h-[360px] overflow-y-auto pr-1">
            {dbLogs.length > 0 ? (
              dbLogs.map((log, idx) => (
                <div key={idx} className={`text-[10px] ${themeClasses.inputBg} border ${themeClasses.border} rounded-xl p-3 space-y-1`}>
                  <div className="flex justify-between items-center text-[9px]">
                    <span className="font-bold text-indigo-400 uppercase tracking-wider">{log.action}</span>
                    <span className="text-slate-500 font-mono">{new Date(log.timestamp).toLocaleTimeString()}</span>
                  </div>
                  <p className={`${themeClasses.textMain} leading-relaxed`}>{log.details}</p>
                </div>
              ))
            ) : (
              <div className="text-center py-12 text-slate-400 text-[10px]">
                No operations logs recorded.
              </div>
            )}
          </div>
        </div>
      )}

      {/* VIEW SECTION 4: NATIVE SETTINGS PANEL */}
      {activeTab === "settings" && (
        <div className="space-y-4">
          <div className={`${themeClasses.cardBg} border ${themeClasses.border} rounded-2xl p-4 space-y-4`}>
            <div>
              <h3 className={`text-xs font-bold uppercase tracking-wider ${themeClasses.textMuted} font-display`}>
                App Settings
              </h3>
              <p className="text-[10px] text-slate-400">Configure native Android preferences</p>
            </div>

            {/* Accent Theme color selector */}
            <div className="space-y-2">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Material You Theme</span>
              <div className="grid grid-cols-4 gap-1.5">
                {[
                  { key: "sapphire", label: "Sapphire", hex: "#6366f1" },
                  { key: "emerald", label: "Emerald", hex: "#10b981" },
                  { key: "coral", label: "Sunset", hex: "#f43f5e" },
                  { key: "lavender", label: "Lavender", hex: "#a855f7" }
                ].map((colorOpt) => (
                  <button
                    key={colorOpt.key}
                    onClick={() => setAccentTheme(colorOpt.key as any)}
                    className={`p-2 rounded-xl border text-[10px] font-semibold flex flex-col items-center gap-1 transition cursor-pointer ${
                      accentTheme === colorOpt.key 
                        ? "bg-slate-800/80 border-indigo-500 text-slate-100" 
                        : `${themeClasses.inputBg} ${themeClasses.border} text-slate-400 hover:text-slate-200`
                    }`}
                  >
                    <div className="w-3 h-3 rounded-full" style={{ backgroundColor: colorOpt.hex }}></div>
                    <span>{colorOpt.label}</span>
                  </button>
                ))}
              </div>
            </div>

            {/* Light/Dark Mode toggle */}
            <div className="space-y-2">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">Display Style</span>
              <div className="grid grid-cols-2 gap-2">
                <button
                  onClick={() => setIsDark(true)}
                  className={`p-2.5 rounded-xl border text-[10px] font-semibold flex items-center justify-center gap-1.5 transition cursor-pointer ${
                    isDark 
                      ? "bg-slate-800/80 border-indigo-500 text-slate-100" 
                      : `${themeClasses.inputBg} ${themeClasses.border} text-slate-400`
                  }`}
                >
                  <Moon className="h-3 w-3 text-amber-400" />
                  <span>Night Mode</span>
                </button>
                <button
                  onClick={() => setIsDark(false)}
                  className={`p-2.5 rounded-xl border text-[10px] font-semibold flex items-center justify-center gap-1.5 transition cursor-pointer ${
                    !isDark 
                      ? "bg-white border-slate-200 text-slate-900 shadow-sm" 
                      : `${themeClasses.inputBg} ${themeClasses.border} text-slate-400`
                  }`}
                >
                  <Sun className="h-3 w-3 text-amber-600" />
                  <span>Day Mode</span>
                </button>
              </div>
            </div>

            {/* Spec details */}
            <div className="bg-slate-950/20 dark:bg-slate-950/40 border border-slate-800/40 rounded-xl p-3.5 space-y-2 font-mono text-[9px] text-slate-400">
              <div className="flex justify-between border-b border-slate-800/40 pb-1.5">
                <span>Processor</span>
                <span className="text-slate-200">Snapdragon 8 Gen 4</span>
              </div>
              <div className="flex justify-between border-b border-slate-800/40 pb-1.5">
                <span>OS</span>
                <span className="text-slate-200">Android 15 (MD3)</span>
              </div>
              <div className="flex justify-between">
                <span>Db Engine</span>
                <span className="text-slate-200">Local SQLite Proxy</span>
              </div>
            </div>

            {/* Logout button */}
            <button
              onClick={handleLogout}
              className="w-full flex items-center justify-center gap-2 bg-rose-600/10 hover:bg-rose-600/25 border border-rose-500/20 text-rose-400 py-2.5 rounded-xl text-xs font-bold cursor-pointer transition"
            >
              <LogOut className="h-3.5 w-3.5" />
              Sign Out Account
            </button>

          </div>
        </div>
      )}

    </div>
  );
}
