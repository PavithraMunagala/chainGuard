import { useState, useEffect, useRef } from "react";
import axios from "axios";
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from "recharts";

// ── Google Fonts injection ────────────────────────────────────────────────────
const FontLoader = () => (
  <style>{`
    @import url('https://fonts.googleapis.com/css2?family=Syne:wght@400;600;700;800&family=JetBrains+Mono:wght@300;400;500&display=swap');
    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
    html, body, #root { width: 100%; min-height: 100vh; background: #020810; }
    body { overflow-x: hidden; }
    ::-webkit-scrollbar { width: 4px; }
    ::-webkit-scrollbar-track { background: #020810; }
    ::-webkit-scrollbar-thumb { background: #1a3a5c; border-radius: 2px; }

    @keyframes chainMove {
      0%   { transform: translateX(0) translateY(0); opacity: 0.06; }
      50%  { opacity: 0.12; }
      100% { transform: translateX(-80px) translateY(-80px); opacity: 0.06; }
    }
    @keyframes pulse-ring {
      0%   { transform: scale(0.95); opacity: 0.6; }
      50%  { transform: scale(1.05); opacity: 1; }
      100% { transform: scale(0.95); opacity: 0.6; }
    }
    @keyframes fadeUp {
      from { opacity: 0; transform: translateY(24px); }
      to   { opacity: 1; transform: translateY(0); }
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    @keyframes scanLine {
      0%   { top: 0%; opacity: 1; }
      100% { top: 100%; opacity: 0; }
    }
    @keyframes glow-pulse {
      0%, 100% { box-shadow: 0 0 20px #00aaff18; }
      50%       { box-shadow: 0 0 40px #00aaff30; }
    }
    @keyframes float {
      0%, 100% { transform: translateY(0px); }
      50%       { transform: translateY(-6px); }
    }
    @keyframes borderRun {
      0%   { background-position: 0% 50%; }
      100% { background-position: 200% 50%; }
    }

    .fade-up { animation: fadeUp 0.6s ease forwards; }
    .fade-up-1 { animation: fadeUp 0.6s 0.1s ease both; }
    .fade-up-2 { animation: fadeUp 0.6s 0.2s ease both; }
    .fade-up-3 { animation: fadeUp 0.6s 0.3s ease both; }
    .fade-up-4 { animation: fadeUp 0.6s 0.4s ease both; }

    .tab-btn { transition: all 0.2s ease; }
    .tab-btn:hover { color: #e8f4f8 !important; }
    .analyze-btn {
      position: relative; overflow: hidden;
      transition: all 0.25s ease;
    }
    .analyze-btn::after {
      content: ''; position: absolute; inset: 0;
      background: linear-gradient(90deg, transparent, rgba(255,255,255,0.08), transparent);
      transform: translateX(-100%); transition: transform 0.4s ease;
    }
    .analyze-btn:hover::after { transform: translateX(100%); }
    .analyze-btn:hover { transform: translateY(-1px); box-shadow: 0 8px 24px #00aaff25; }

    .issue-row { transition: background 0.15s ease; }
    .issue-row:hover { background: #ffffff06 !important; }
  `}</style>
);

// ── Animated Blockchain Background ───────────────────────────────────────────
const BlockchainBg = () => {
  const canvasRef = useRef(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    const ctx = canvas.getContext("2d");
    let animId;
    let t = 0;

    const resize = () => {
      canvas.width  = window.innerWidth;
      canvas.height = window.innerHeight;
    };
    resize();
    window.addEventListener("resize", resize);

    // Node grid
    const nodes = [];
    const cols = 12, rows = 8;
    for (let i = 0; i < cols; i++) {
      for (let j = 0; j < rows; j++) {
        nodes.push({
          x: (i / (cols - 1)) * canvas.width,
          y: (j / (rows - 1)) * canvas.height,
          ox: (i / (cols - 1)) * canvas.width,
          oy: (j / (rows - 1)) * canvas.height,
          phase: Math.random() * Math.PI * 2,
          speed: 0.3 + Math.random() * 0.4,
          r: 1.5 + Math.random() * 1.5,
        });
      }
    }

    // Floating blocks
    const blocks = Array.from({ length: 18 }, () => ({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      size: 14 + Math.random() * 24,
      speed: 0.08 + Math.random() * 0.15,
      opacity: 0.03 + Math.random() * 0.05,
      rotation: Math.random() * Math.PI * 2,
      rotSpeed: (Math.random() - 0.5) * 0.003,
    }));

    const draw = () => {
      t += 0.005;
      ctx.clearRect(0, 0, canvas.width, canvas.height);

      // Subtle gradient background
      const grad = ctx.createRadialGradient(
        canvas.width * 0.3, canvas.height * 0.3, 0,
        canvas.width * 0.5, canvas.height * 0.5, canvas.width * 0.8
      );
      grad.addColorStop(0, "#03111f");
      grad.addColorStop(1, "#020810");
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      // Animate nodes
      nodes.forEach(n => {
        n.x = n.ox + Math.sin(t * n.speed + n.phase) * 18;
        n.y = n.oy + Math.cos(t * n.speed * 0.7 + n.phase) * 12;
      });

      // Draw chain links between nearby nodes
      for (let i = 0; i < nodes.length; i++) {
        for (let j = i + 1; j < nodes.length; j++) {
          const dx = nodes[i].x - nodes[j].x;
          const dy = nodes[i].y - nodes[j].y;
          const dist = Math.sqrt(dx * dx + dy * dy);
          const maxDist = canvas.width / (cols - 1) * 1.6;
          if (dist < maxDist) {
            const alpha = (1 - dist / maxDist) * 0.07;
            ctx.beginPath();
            ctx.moveTo(nodes[i].x, nodes[i].y);
            ctx.lineTo(nodes[j].x, nodes[j].y);
            ctx.strokeStyle = `rgba(0, 160, 220, ${alpha})`;
            ctx.lineWidth = 0.5;
            ctx.stroke();
          }
        }
      }

      // Draw nodes
      nodes.forEach(n => {
        ctx.beginPath();
        ctx.arc(n.x, n.y, n.r, 0, Math.PI * 2);
        ctx.fillStyle = `rgba(0, 180, 240, 0.15)`;
        ctx.fill();
      });

      // Floating blockchain blocks
      blocks.forEach(b => {
        b.y -= b.speed;
        b.rotation += b.rotSpeed;
        if (b.y < -50) { b.y = canvas.height + 50; b.x = Math.random() * canvas.width; }

        ctx.save();
        ctx.translate(b.x, b.y);
        ctx.rotate(b.rotation);
        ctx.strokeStyle = `rgba(0, 160, 220, ${b.opacity})`;
        ctx.lineWidth = 0.8;
        ctx.strokeRect(-b.size / 2, -b.size / 2, b.size, b.size);

        // Chain connector dots
        ctx.fillStyle = `rgba(0, 160, 220, ${b.opacity * 1.5})`;
        ctx.fillRect(-1.5, -b.size / 2 - 4, 3, 4);
        ctx.fillRect(-1.5, b.size / 2, 3, 4);
        ctx.restore();
      });

      // Moving scan line
      const scanY = ((t * 40) % canvas.height);
      const scanGrad = ctx.createLinearGradient(0, scanY - 60, 0, scanY + 60);
      scanGrad.addColorStop(0, "transparent");
      scanGrad.addColorStop(0.5, "rgba(0, 180, 240, 0.03)");
      scanGrad.addColorStop(1, "transparent");
      ctx.fillStyle = scanGrad;
      ctx.fillRect(0, scanY - 60, canvas.width, 120);

      animId = requestAnimationFrame(draw);
    };

    draw();
    return () => {
      cancelAnimationFrame(animId);
      window.removeEventListener("resize", resize);
    };
  }, []);

  return (
    <canvas ref={canvasRef} style={{
      position: "fixed", inset: 0, zIndex: 0, pointerEvents: "none",
    }} />
  );
};

// ── Design tokens ─────────────────────────────────────────────────────────────
const T = {
  bg:      "#020810",
  surface: "#060f1c",
  card:    "#08111d",
  border:  "#0f2035",
  borderHi:"#1a3a5c",
  accent:  "#00aaff",
  accentLo:"#003d5c",
  green:   "#00e5a0",
  amber:   "#f5a623",
  orange:  "#ff6b35",
  red:     "#ff3d5a",
  muted:   "#2a4a6a",
  text:    "#c8dff0",
  textDim: "#4a7090",
  white:   "#f0f8ff",
  display: "'Syne', sans-serif",
  mono:    "'JetBrains Mono', monospace",
};

const riskMeta = (score) => {
  if (score >= 80) return { color: T.red,    label: "CRITICAL",  sub: "Immediate action required",  glow: "#ff3d5a" };
  if (score >= 60) return { color: T.orange,  label: "HIGH",      sub: "Serious vulnerabilities found", glow: "#ff6b35" };
  if (score >= 40) return { color: T.amber,   label: "MODERATE",  sub: "Review before deployment",   glow: "#f5a623" };
  if (score >= 16) return { color: "#a8e63d", label: "LOW",       sub: "Minor issues detected",      glow: "#a8e63d" };
  return               { color: T.green,  label: "SECURE",    sub: "No critical issues found",   glow: "#00e5a0" };
};

const impactColor = (impact) => {
  if (!impact) return T.muted;
  const i = impact.toLowerCase();
  if (i === "high")         return T.red;
  if (i === "medium")       return T.amber;
  if (i === "low")          return "#a8e63d";
  if (i === "optimization") return T.accent;
  return T.textDim;
};

// ── Card ──────────────────────────────────────────────────────────────────────
const Card = ({ children, style = {}, className = "" }) => (
  <div className={className} style={{
    background: T.card,
    border: `1px solid ${T.border}`,
    borderRadius: "16px",
    padding: "28px",
    position: "relative",
    overflow: "hidden",
    ...style,
  }}>
    {children}
  </div>
);

const Label = ({ children }) => (
  <div style={{
    fontFamily: T.mono, fontSize: "12px", letterSpacing: "0.15em",
    color: T.textDim, textTransform: "uppercase", marginBottom: "14px",
  }}>{children}</div>
);

// ── Score Gauge ───────────────────────────────────────────────────────────────
const ScoreGauge = ({ score }) => {
  const meta = riskMeta(score);
  const r = 56, circ = 2 * Math.PI * r;
  const offset = circ - (score / 100) * circ;

  return (
    <div style={{ display: "flex", flexDirection: "column", alignItems: "center", padding: "12px 0" }}>
      <div style={{ position: "relative", width: "152px", height: "152px" }}>
        {/* Outer glow ring */}
        <div style={{
          position: "absolute", inset: "-8px", borderRadius: "50%",
          background: `radial-gradient(circle, ${meta.glow}15 0%, transparent 70%)`,
          animation: score > 15 ? "pulse-ring 3s ease-in-out infinite" : "none",
        }} />
        <svg width="152" height="152" style={{ transform: "rotate(-90deg)" }}>
          <circle cx="76" cy="76" r={r} fill="none" stroke={T.border} strokeWidth="8" />
          <circle cx="76" cy="76" r={r} fill="none"
            stroke={meta.color} strokeWidth="8"
            strokeDasharray={circ} strokeDashoffset={offset}
            strokeLinecap="round"
            style={{
              filter: `drop-shadow(0 0 10px ${meta.glow}88)`,
              transition: "stroke-dashoffset 1.2s cubic-bezier(0.4,0,0.2,1)",
            }}
          />
        </svg>
        <div style={{
          position: "absolute", inset: 0,
          display: "flex", flexDirection: "column",
          alignItems: "center", justifyContent: "center",
        }}>
          <span style={{ fontFamily: T.display, fontSize: "38px", fontWeight: "800",
            color: meta.color, lineHeight: 1,
            textShadow: `0 0 20px ${meta.glow}66`,
          }}>{score}</span>
          <span style={{ fontFamily: T.mono, fontSize: "12px", color: T.textDim, marginTop: "2px" }}>/100</span>
        </div>
      </div>

      <div style={{ marginTop: "16px", textAlign: "center" }}>
        <div style={{
          fontFamily: T.display, fontSize: "13px", fontWeight: "700",
          letterSpacing: "0.2em", color: meta.color,
          padding: "6px 20px",
          background: meta.glow + "12",
          border: `1px solid ${meta.glow}30`,
          borderRadius: "20px",
        }}>{meta.label}</div>
        <div style={{ fontFamily: T.mono, fontSize: "12px", color: T.textDim, marginTop: "8px" }}>
          {meta.sub}
        </div>
      </div>
    </div>
  );
};

// ── Severity Donut ────────────────────────────────────────────────────────────
const SeverityDonut = ({ dashboard }) => {
  const data = [
    { name: "Critical",     value: dashboard.high         || 0, color: T.red },
    { name: "Medium",       value: dashboard.medium       || 0, color: T.amber },
    { name: "Low",          value: dashboard.low          || 0, color: "#a8e63d" },
    { name: "Advisory",     value: dashboard.informational|| 0, color: T.accent },
    { name: "Optimization", value: dashboard.optimization || 0, color: "#7b61ff" },
  ].filter(d => d.value > 0);

  const total = data.reduce((s, d) => s + d.value, 0);
  if (!total) return (
    <div style={{ textAlign: "center", padding: "40px 0", fontFamily: T.mono,
      fontSize: "12px", color: T.green }}>
      ✓ No issues detected
    </div>
  );

  return (
    <div style={{ display: "flex", alignItems: "center", gap: "24px" }}>
      <ResponsiveContainer width={160} height={160}>
        <PieChart>
          <Pie data={data} dataKey="value" innerRadius={46} outerRadius={72} paddingAngle={2} startAngle={90} endAngle={-270}>
            {data.map((d, i) => (
              <Cell key={i} fill={d.color} style={{ filter: `drop-shadow(0 0 4px ${d.color}66)`, outline: "none" }} />
            ))}
          </Pie>
          <Tooltip contentStyle={{ background: T.surface, border: `1px solid ${T.border}`,
            borderRadius: "8px", fontFamily: T.mono, fontSize: "11px", color: T.text }} />
        </PieChart>
      </ResponsiveContainer>
      <div style={{ flex: 1 }}>
        {data.map((d, i) => (
          <div key={i} style={{ display: "flex", justifyContent: "space-between",
            alignItems: "center", padding: "5px 0",
            borderBottom: i < data.length - 1 ? `1px solid ${T.border}` : "none" }}>
            <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
              <div style={{ width: "6px", height: "6px", borderRadius: "50%",
                background: d.color, boxShadow: `0 0 6px ${d.color}` }} />
              <span style={{ fontFamily: T.mono, fontSize: "12px", color: T.text }}>{d.name}</span>
            </div>
            <span style={{ fontFamily: T.mono, fontSize: "12px", fontWeight: "500", color: d.color }}>{d.value}</span>
          </div>
        ))}
      </div>
    </div>
  );
};

// ── Stat Tile ─────────────────────────────────────────────────────────────────
const StatTile = ({ label, value, color = T.text, sub }) => (
  <div style={{ padding: "20px", background: T.surface, borderRadius: "12px",
    border: `1px solid ${T.border}` }}>
    <div style={{ fontFamily: T.mono, fontSize: "12px", color: T.textDim,
      letterSpacing: "0.12em", marginBottom: "8px" }}>{label}</div>
    <div style={{ fontFamily: T.display, fontSize: "26px", fontWeight: "700",
      color, lineHeight: 1 }}>{value}</div>
    {sub && <div style={{ fontFamily: T.mono, fontSize: "10px", color: T.textDim, marginTop: "6px" }}>{sub}</div>}
  </div>
);

// ── Main App ──────────────────────────────────────────────────────────────────
export default function App() {
  const [address, setAddress] = useState("");
  const [file, setFile]       = useState(null);
  const [result, setResult]   = useState(null);
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState("vulnerabilities");
  const [dragOver, setDragOver]   = useState(false);

  const analyzeAddress = async () => {
    if (!address) return;
    setLoading(true); setResult(null);
    try {
      const res = await axios.post("http://localhost:8080/api/contracts/address", { address });
      if (res.data.sourceAvailable === false) {
        alert("Contract source code not verified on Etherscan. Please upload the .sol file directly.");
        setLoading(false); return;
      }
      setResult(res.data);
      setActiveTab("vulnerabilities");
    } catch { alert("Address analysis failed. Check the address and try again."); }
    setLoading(false);
  };

  const uploadFile = async (f) => {
    const target = f || file;
    if (!target) return;
    const formData = new FormData();
    formData.append("file", target);
    setLoading(true); setResult(null);
    try {
      const res = await axios.post("http://localhost:8080/api/contracts/upload", formData,
        { headers: { "Content-Type": "multipart/form-data" } });
      setResult(res.data);
      setActiveTab("vulnerabilities");
    } catch { alert("File analysis failed."); }
    setLoading(false);
  };

  const onDrop = (e) => {
    e.preventDefault(); setDragOver(false);
    const f = e.dataTransfer.files[0];
    if (f?.name.endsWith(".sol")) { setFile(f); uploadFile(f); }
  };

  const risk      = result?.riskAnalysis;
  const breakdown = risk?.scoreBreakdown;
  const dashboard = result?.dashboard;
  const ml        = result?.mlResult;
  const micro     = result?.microVulnerabilities || [];
  const meta      = risk ? riskMeta(risk.finalScore) : null;

  const tabs = [
    { id: "vulnerabilities", label: "Vulnerabilities" },
    { id: "threat",          label: "Threat Analysis" },
    { id: "code-quality",    label: "Code Quality" },
    { id: "gas",             label: "Optimization" },
    { id: "remediation",     label: "Remediation" },
  ];

  return (
    <>
      <FontLoader />
      <BlockchainBg />

      <div style={{ position: "relative", zIndex: 1, minHeight: "100vh",
        fontFamily: T.mono, color: T.text }}>

        {/* ── Header ── */}
        <header style={{
          borderBottom: `1px solid ${T.border}`,
          background: "rgba(2,8,16,0.85)",
          backdropFilter: "blur(20px)",
          position: "sticky", top: 0, zIndex: 100,
        }}>
          <div style={{ maxWidth: "1280px", margin: "0 auto",
            padding: "0 40px", height: "64px",
            display: "flex", alignItems: "center", justifyContent: "space-between" }}>

            <div style={{ display: "flex", alignItems: "center", gap: "14px" }}>
              {/* Logo */}
              <div style={{
                width: "40px", height: "40px", borderRadius: "10px",
                display: "flex", alignItems: "center", justifyContent: "center",
                flexShrink: 0,
              }}>
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40" width="40" height="40">
                  <defs>
                    <linearGradient id="npg" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" stopColor="#00ccff"/>
                      <stop offset="100%" stopColor="#0066cc"/>
                    </linearGradient>
                    <filter id="npglow" x="-20%" y="-20%" width="140%" height="140%">
                      <feGaussianBlur stdDeviation="1.2" result="blur"/>
                      <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
                    </filter>
                  </defs>
                  <rect width="40" height="40" rx="9" fill="#00aaff" fillOpacity="0.12"/>
                  <rect width="40" height="40" rx="9" fill="none" stroke="#00aaff" strokeOpacity="0.5" strokeWidth="1"/>
                  <path d="M5.5 30 L5.5 10 L15.5 28 L15.5 10"
                    stroke="url(#npg)" strokeWidth="2.4" strokeLinecap="round"
                    strokeLinejoin="round" fill="none" filter="url(#npglow)"/>
                  <line x1="20.5" y1="10" x2="20.5" y2="30"
                    stroke="url(#npg)" strokeWidth="2.4" strokeLinecap="round" filter="url(#npglow)"/>
                  <path d="M20.5 10 C20.5 10 32 10 32 18 C32 26 20.5 24.5 20.5 24.5"
                    stroke="url(#npg)" strokeWidth="2.4" strokeLinecap="round"
                    strokeLinejoin="round" fill="none" filter="url(#npglow)"/>
                </svg>
              </div>
              <div>
                <div style={{ fontFamily: T.display, fontSize: "17px", fontWeight: "700",
                  color: T.white, letterSpacing: "0.04em" }}>ChainGuard</div>
                <div style={{ fontFamily: T.mono, fontSize: "11px", color: T.textDim,
                  letterSpacing: "0.15em" }}>SMART CONTRACT SECURITY</div>
              </div>
            </div>

            <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
              {["Automated Audit", "AI-Powered", "Real-time Analysis"].map(tag => (
                <span key={tag} style={{
                  padding: "4px 12px", borderRadius: "20px",
                  border: `1px solid ${T.border}`,
                  fontFamily: T.mono, fontSize: "11px",
                  color: T.textDim, letterSpacing: "0.08em",
                }}>{tag}</span>
              ))}
            </div>
          </div>
        </header>

        <main style={{ maxWidth: "1280px", margin: "0 auto", padding: "48px 40px" }}>

          {/* ── Hero input section ── */}
          {!result && !loading && (
            <div className="fade-up" style={{ textAlign: "center", marginBottom: "56px" }}>
              <div style={{
                display: "inline-block", padding: "4px 16px", marginBottom: "24px",
                border: `1px solid ${T.accent}44`, borderRadius: "20px",
                fontFamily: T.mono, fontSize: "10px", color: T.accent, letterSpacing: "0.15em",
                background: `${T.accent}08`,
              }}>ENTERPRISE SECURITY AUDIT PLATFORM</div>

              <h1 style={{
                fontFamily: T.display, fontSize: "clamp(32px, 5vw, 56px)",
                fontWeight: "800", color: T.white, lineHeight: 1.1,
                marginBottom: "16px", letterSpacing: "-0.02em",
              }}>
                Audit Smart Contracts<br />
                <span style={{ color: T.accent }}>Before They Go Live</span>
              </h1>

              <p style={{ fontFamily: T.mono, fontSize: "13px", color: T.textDim,
                maxWidth: "480px", margin: "0 auto 48px", lineHeight: "1.8" }}>
                Detect vulnerabilities, assess risk, and get remediation guidance
                in seconds — not days.
              </p>
            </div>
          )}

          {/* ── Input panel ── */}
          <Card className={result ? "" : "fade-up-1"} style={{
            marginBottom: "32px",
            background: result ? T.card : "rgba(8,17,29,0.95)",
            backdropFilter: "blur(20px)",
            animation: result ? "none" : undefined,
          }}>
            {!result && (
              <div style={{ borderBottom: `1px solid ${T.border}`, paddingBottom: "24px", marginBottom: "24px" }}>
                <Label>Analyze by Contract Address</Label>
                <div style={{ display: "flex", gap: "12px" }}>
                  <input
                    style={{
                      flex: 1, background: T.surface, border: `1px solid ${T.border}`,
                      borderRadius: "10px", padding: "14px 18px",
                      fontFamily: T.mono, fontSize: "14px", color: T.text,
                      outline: "none", transition: "border-color 0.2s",
                    }}
                    placeholder="0x..."
                    value={address}
                    onChange={e => setAddress(e.target.value)}
                    onKeyDown={e => e.key === "Enter" && analyzeAddress()}
                    onFocus={e => e.target.style.borderColor = T.accent + "66"}
                    onBlur={e => e.target.style.borderColor = T.border}
                  />
                  <button className="analyze-btn" onClick={analyzeAddress} style={{
                    padding: "14px 28px", borderRadius: "10px", cursor: "pointer",
                    fontFamily: T.mono, fontSize: "11px", letterSpacing: "0.12em",
                    fontWeight: "500", border: `1px solid ${T.accent}55`,
                    background: `linear-gradient(135deg, ${T.accent}18, ${T.accentLo}88)`,
                    color: T.accent, whiteSpace: "nowrap",
                  }}>AUDIT ADDRESS</button>
                </div>
              </div>
            )}

            <Label>{result ? "Contract Source" : "Upload Solidity File"}</Label>
            <div
              onDrop={onDrop}
              onDragOver={e => { e.preventDefault(); setDragOver(true); }}
              onDragLeave={() => setDragOver(false)}
              style={{
                border: `1.5px dashed ${dragOver ? T.accent : T.border}`,
                borderRadius: "12px", padding: result ? "16px 20px" : "32px",
                textAlign: result ? "left" : "center",
                background: dragOver ? `${T.accent}08` : T.surface,
                transition: "all 0.2s ease", cursor: "pointer",
                display: "flex", alignItems: "center",
                justifyContent: result ? "space-between" : "center",
                gap: "16px",
              }}
              onClick={() => !result && document.getElementById("sol-input").click()}
            >
              {result ? (
                <>
                  <div style={{ display: "flex", alignItems: "center", gap: "12px" }}>
                    <div style={{
                      width: "32px", height: "32px", borderRadius: "8px",
                      background: `${T.green}15`, border: `1px solid ${T.green}44`,
                      display: "flex", alignItems: "center", justifyContent: "center",
                      fontSize: "14px",
                    }}>✓</div>
                    <div>
                      <div style={{ fontFamily: T.mono, fontSize: "12px", color: T.text }}>
                        {file?.name || "Contract analyzed"}
                      </div>
                      <div style={{ fontFamily: T.mono, fontSize: "10px", color: T.textDim, marginTop: "2px" }}>
                        Analysis complete
                      </div>
                    </div>
                  </div>
                  <button className="analyze-btn" onClick={(e) => { e.stopPropagation(); setResult(null); setFile(null); setAddress(""); }}
                    style={{
                      padding: "8px 20px", borderRadius: "8px", cursor: "pointer",
                      fontFamily: T.mono, fontSize: "10px", letterSpacing: "0.1em",
                      border: `1px solid ${T.border}`, background: "transparent",
                      color: T.textDim,
                    }}>NEW AUDIT</button>
                </>
              ) : (
                <div>
                  <div style={{ marginBottom: "12px", opacity: 0.4 }}><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="#00aaff" strokeWidth="1.5" strokeLinecap="round"><path d="M12 2 L2 8 L2 16 L12 22 L22 16 L22 8 Z"/></svg></div>
                  <div style={{ fontFamily: T.mono, fontSize: "12px", color: T.text, marginBottom: "6px" }}>
                    Drop your .sol file here or click to browse
                  </div>
                  <div style={{ fontFamily: T.mono, fontSize: "10px", color: T.textDim }}>
                    Solidity smart contracts only
                  </div>
                </div>
              )}
              <input id="sol-input" type="file" accept=".sol" style={{ display: "none" }}
                onChange={e => { const f = e.target.files[0]; setFile(f); uploadFile(f); }} />
            </div>
          </Card>

          {/* ── Loading ── */}
          {loading && (
            <div className="fade-up" style={{
              display: "flex", flexDirection: "column", alignItems: "center",
              justifyContent: "center", padding: "80px 0", gap: "24px",
            }}>
              <div style={{ position: "relative", width: "64px", height: "64px" }}>
                <div style={{
                  position: "absolute", inset: 0, border: `2px solid ${T.border}`,
                  borderTop: `2px solid ${T.accent}`,
                  borderRadius: "50%", animation: "spin 0.9s linear infinite",
                }} />
                <div style={{
                  position: "absolute", inset: "10px", border: `2px solid ${T.border}`,
                  borderBottom: `2px solid ${T.accent}66`,
                  borderRadius: "50%", animation: "spin 1.4s linear infinite reverse",
                }} />
              </div>
              <div>
                <div style={{ fontFamily: T.display, fontSize: "16px", fontWeight: "600",
                  color: T.text, textAlign: "center", marginBottom: "6px" }}>
                  Analyzing Contract
                </div>
                <div style={{ fontFamily: T.mono, fontSize: "11px", color: T.textDim,
                  textAlign: "center", letterSpacing: "0.1em" }}>
                  Running security analysis — this may take a few seconds
                </div>
              </div>
            </div>
          )}

          {/* ── Dashboard ── */}
          {result && risk && (
            <>
              {/* Row 1: Score + Overview + Breakdown */}
              <div className="fade-up" style={{
                display: "grid", gridTemplateColumns: "240px 1fr 320px",
                gap: "20px", marginBottom: "20px",
              }}>

                {/* Score */}
                <Card style={{ display: "flex", flexDirection: "column",
                  alignItems: "center", justifyContent: "center",
                  boxShadow: `0 0 60px ${meta.glow}10` }}>
                  <Label>Security Score</Label>
                  <ScoreGauge score={risk.finalScore} />
                </Card>

                {/* Overview */}
                <Card>
                  <Label>Audit Summary</Label>
                  <p style={{ fontFamily: T.mono, fontSize: "14px", color: T.text,
                    lineHeight: "1.8", marginBottom: "20px", borderLeft: `2px solid ${meta.color}`,
                    paddingLeft: "14px" }}>
                    {risk.explanation}
                  </p>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr 1fr", gap: "12px" }}>
                    <StatTile label="Critical"     value={dashboard?.high   || 0} color={T.red}    />
                    <StatTile label="Medium"        value={dashboard?.medium || 0} color={T.amber}  />
                    <StatTile label="Total Issues"  value={dashboard?.totalIssues || ((dashboard?.high||0)+(dashboard?.medium||0)+(dashboard?.low||0))} color={T.text} />
                  </div>
                </Card>

                {/* Distribution */}
                <Card>
                  <Label>Issue Distribution</Label>
                  {dashboard && <SeverityDonut dashboard={dashboard} />}
                </Card>
              </div>

              {/* Row 2: Stats + Score Breakdown */}
              <div className="fade-up-1" style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "20px", marginBottom: "20px" }}>

                {/* Quick stats */}
                <Card>
                  <Label>Risk Overview</Label>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                    {[
                      { label: "Risk Level",           value: meta.label,                              color: meta.color },
                      { label: "Threat Confidence",    value: `${(risk.mlRiskScore*100).toFixed(0)}%`, color: T.accent },
                      { label: "Efficiency Rating",    value: risk.gasLevel?.split(" ")[0] || "—",     color: "#7b61ff" },
                      { label: "Code Pattern Issues",  value: micro.length,                            color: T.amber },
                    ].map(s => (
                      <div key={s.label} style={{ padding: "16px", background: T.surface,
                        borderRadius: "10px", border: `1px solid ${T.border}` }}>
                        <div style={{ fontFamily: T.mono, fontSize: "12px", color: T.textDim,
                          letterSpacing: "0.1em", marginBottom: "8px" }}>{s.label}</div>
                        <div style={{ fontFamily: T.display, fontSize: "22px", fontWeight: "700",
                          color: s.color }}>{s.value}</div>
                      </div>
                    ))}
                  </div>
                </Card>

                {/* Score breakdown */}
                <Card>
                  <Label>Score Breakdown</Label>
                  {breakdown && (
                    <div>
                      {[
                        { label: "Static Analysis Score", value: breakdown.baseScore,          max: 60, color: T.textDim },
                        { label: "Fund Drain Risk",        value: breakdown.reentrancyImpact,   max: 45, color: T.red },
                        { label: "Code Injection Risk",    value: breakdown.delegatecallImpact, max: 40, color: T.orange },
                        { label: "External Exposure",      value: breakdown.externalCallImpact, max: 30, color: T.amber },
                        { label: "Pattern Risk",           value: breakdown.microImpact,        max: 10, color: "#7b61ff" },
                        { label: "Behavioral Risk Score",  value: breakdown.mlImpact,           max: 20, color: T.accent },
                      ].map(({ label, value, max, color }) => (
                        <div key={label} style={{ display: "flex", justifyContent: "space-between",
                          alignItems: "center", padding: "8px 0", borderBottom: `1px solid ${T.border}22` }}>
                          <span style={{ fontFamily: T.mono, fontSize: "13px", color: T.textDim }}>{label}</span>
                          <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                            <div style={{ width: "80px", height: "3px", background: T.border, borderRadius: "2px" }}>
                              <div style={{ width: `${Math.min((value/max)*100,100)}%`, height: "100%",
                                background: color, borderRadius: "2px",
                                transition: "width 0.8s cubic-bezier(0.4,0,0.2,1)" }} />
                            </div>
                            <span style={{ fontFamily: T.mono, fontSize: "12px", fontWeight: "600",
                              color: T.white, minWidth: "24px", textAlign: "right" }}>{value}</span>
                          </div>
                        </div>
                      ))}
                      <div style={{ display: "flex", justifyContent: "space-between",
                        alignItems: "center", paddingTop: "12px", marginTop: "4px",
                        borderTop: `1px solid ${T.borderHi}` }}>
                        <span style={{ fontFamily: T.mono, fontSize: "13px", fontWeight: "600", color: T.text }}>Final Security Score</span>
                        <span style={{ fontFamily: T.display, fontSize: "22px", fontWeight: "800", color: meta.color }}>{breakdown.finalComputedScore}</span>
                      </div>
                    </div>
                  )}
                </Card>
              </div>

              {/* ── Tabs ── */}
              <Card className="fade-up-2" style={{ padding: 0, overflow: "hidden" }}>

                {/* Tab nav */}
                <div style={{
                  display: "flex", borderBottom: `1px solid ${T.border}`,
                  padding: "0 28px", background: T.surface,
                }}>
                  {tabs.map(tab => (
                    <button key={tab.id} className="tab-btn"
                      onClick={() => setActiveTab(tab.id)}
                      style={{
                        background: "transparent", border: "none",
                        borderBottom: activeTab === tab.id
                          ? `2px solid ${T.accent}` : "2px solid transparent",
                        padding: "16px 20px", cursor: "pointer",
                        fontFamily: T.mono, fontSize: "13px", letterSpacing: "0.08em",
                        color: activeTab === tab.id ? T.accent : T.textDim,
                        fontWeight: activeTab === tab.id ? "500" : "400",
                        whiteSpace: "nowrap",
                      }}>
                      {tab.label}
                    </button>
                  ))}
                </div>

                <div style={{ padding: "28px" }}>

                  {/* ── Vulnerabilities ── */}
                  {activeTab === "vulnerabilities" && (
                    <div>
                      {!dashboard?.issues?.length
                        ? <p style={{ fontFamily: T.mono, fontSize: "13px", color: T.green }}>
                            ✓ No vulnerabilities detected by static analysis.
                          </p>
                        : dashboard.issues.map((issue, i) => (
                          <div key={i} className="issue-row" style={{
                            display: "grid", gridTemplateColumns: "1fr 110px 60px",
                            gap: "16px", alignItems: "start",
                            padding: "18px 12px", borderRadius: "8px",
                            borderBottom: i < dashboard.issues.length - 1
                              ? `1px solid ${T.border}` : "none",
                          }}>
                            <div>
                              <div style={{ display: "flex", alignItems: "center", gap: "10px", marginBottom: "6px" }}>
                                <div style={{ width: "6px", height: "6px", borderRadius: "50%",
                                  background: impactColor(issue.impact),
                                  boxShadow: `0 0 6px ${impactColor(issue.impact)}` }} />
                                <span style={{ fontFamily: T.mono, fontSize: "13px",
                                  color: T.accent, fontWeight: "500" }}>{issue.check}</span>
                              </div>
                              <p style={{ fontFamily: T.mono, fontSize: "13px",
                                color: T.textDim, lineHeight: "1.7", paddingLeft: "16px" }}>
                                {issue.explanation || issue.description}
                              </p>
                            </div>
                            <div>
                              <span style={{
                                padding: "4px 12px", borderRadius: "4px", fontSize: "10px",
                                fontFamily: T.mono, fontWeight: "600", letterSpacing: "0.08em",
                                background: impactColor(issue.impact) + "15",
                                border: `1px solid ${impactColor(issue.impact)}44`,
                                color: impactColor(issue.impact),
                              }}>{issue.impact}</span>
                            </div>
                            <div style={{ textAlign: "right", fontFamily: T.mono,
                              fontSize: "13px", color: T.text, fontWeight: "600" }}>
                              ×{issue.count}
                            </div>
                          </div>
                        ))
                      }
                    </div>
                  )}

                  {/* ── Threat Analysis ── */}
                  {activeTab === "threat" && (
                    <div>
                      {/* AI assessment */}
                      {ml?.confidenceExplanation && (
                        <div style={{
                          padding: "18px 20px", borderRadius: "10px", marginBottom: "20px",
                          background: `${T.accent}08`, border: `1px solid ${T.accent}22`,
                          borderLeft: `3px solid ${T.accent}`,
                        }}>
                          <div style={{ fontFamily: T.mono, fontSize: "12px", color: T.accent,
                            letterSpacing: "0.12em", marginBottom: "8px" }}>BEHAVIORAL ANALYSIS</div>
                          <p style={{ fontFamily: T.mono, fontSize: "13px", color: T.text, lineHeight: "1.8" }}>
                            {ml.confidenceExplanation}
                          </p>
                        </div>
                      )}

                      {/* Rule detections */}
                      {ml?.ruleFlagExplanations?.length > 0 && (
                        <>
                          <div style={{ fontFamily: T.mono, fontSize: "12px", color: T.textDim,
                            letterSpacing: "0.12em", marginBottom: "14px" }}>
                            CRITICAL PATTERN MATCHES — {ml.ruleFlagExplanations.length} found
                          </div>
                          {ml.ruleFlagExplanations.map((desc, i) => (
                            <div key={i} style={{
                              padding: "16px 18px", borderRadius: "10px", marginBottom: "10px",
                              background: `${T.red}06`, border: `1px solid ${T.red}18`,
                              borderLeft: `3px solid ${T.red}66`,
                            }}>
                              <div style={{ fontFamily: T.mono, fontSize: "10px", fontWeight: "600",
                                color: T.red, letterSpacing: "0.15em", marginBottom: "8px" }}>
                                ⚠ {ml.ruleFlags?.[i]?.replace(/_/g, " ").toUpperCase() || `PATTERN ${i + 1}`}
                              </div>
                              <p style={{ fontFamily: T.mono, fontSize: "12px",
                                color: T.text, lineHeight: "1.8" }}>{desc}</p>
                            </div>
                          ))}
                        </>
                      )}

                      {ml?.ruleFlagExplanations?.length === 0 && (
                        <p style={{ fontFamily: T.mono, fontSize: "13px", color: T.green }}>
                          ✓ No high-risk threat patterns detected.
                        </p>
                      )}

                      {/* Score contribution */}
                      {ml?.mlImpactExplanation && (
                        <div style={{ marginTop: "20px", padding: "14px 18px", borderRadius: "10px",
                          background: T.surface, border: `1px solid ${T.border}` }}>
                          <div style={{ fontFamily: T.mono, fontSize: "12px", color: T.textDim,
                            letterSpacing: "0.12em", marginBottom: "6px" }}>RISK SCORE CONTRIBUTION</div>
                          <p style={{ fontFamily: T.mono, fontSize: "12px", color: T.textDim, lineHeight: "1.7" }}>
                            {ml.mlImpactExplanation}
                          </p>
                        </div>
                      )}
                    </div>
                  )}

                  {/* ── Code Quality ── */}
                  {activeTab === "code-quality" && (
                    <div>
                      {micro.length === 0
                        ? <p style={{ fontFamily: T.mono, fontSize: "13px", color: T.green }}>
                            ✓ No code quality issues detected.
                          </p>
                        : micro.map((v, i) => (
                          <div key={i} style={{
                            padding: "14px 18px", borderRadius: "10px", marginBottom: "10px",
                            background: `${T.amber}06`, border: `1px solid ${T.amber}18`,
                            borderLeft: `3px solid ${T.amber}55`,
                            fontFamily: T.mono, fontSize: "13px", color: T.text, lineHeight: "1.8",
                          }}>{v}</div>
                        ))
                      }
                    </div>
                  )}

                  {/* ── Optimization ── */}
                  {activeTab === "gas" && (
                    <div>
                      <div style={{ display: "flex", alignItems: "center", gap: "16px", marginBottom: "20px" }}>
                        <div>
                          <div style={{ fontFamily: T.mono, fontSize: "10px", color: T.textDim,
                            marginBottom: "4px" }}>GAS EFFICIENCY RATING</div>
                          <div style={{ fontFamily: T.display, fontSize: "20px", fontWeight: "700",
                            color: "#7b61ff" }}>{risk.gasLevel}</div>
                        </div>
                        <div style={{ marginLeft: "auto", padding: "8px 20px",
                          background: "#7b61ff15", border: "1px solid #7b61ff33",
                          borderRadius: "8px", fontFamily: T.mono, fontSize: "13px",
                          color: "#7b61ff", fontWeight: "600" }}>
                          Score: {risk.gasScore}/100
                        </div>
                      </div>
                      {risk.gasSuggestions?.length > 0
                        ? risk.gasSuggestions.map((g, i) => (
                          <div key={i} style={{
                            padding: "14px 18px", borderRadius: "10px", marginBottom: "10px",
                            background: "#7b61ff08", border: "1px solid #7b61ff18",
                            borderLeft: "3px solid #7b61ff55",
                            fontFamily: T.mono, fontSize: "13px", color: T.text, lineHeight: "1.8",
                          }}>{g}</div>
                        ))
                        : <p style={{ fontFamily: T.mono, fontSize: "13px", color: T.green }}>
                            ✓ No optimization issues detected.
                          </p>
                      }
                    </div>
                  )}

                  {/* ── Remediation ── */}
                  {activeTab === "remediation" && (
                    <div>
                      {risk.fixSuggestions?.length > 0
                        ? risk.fixSuggestions.map((fix, i) => {
                            const isCritical = fix.startsWith("🔴");
                            const isHigh     = fix.startsWith("🟠");
                            const isMedium   = fix.startsWith("🟡");
                            const color      = isCritical ? T.red : isHigh ? T.orange : isMedium ? T.amber : T.textDim;
                            return (
                              <div key={i} style={{
                                padding: "16px 18px", borderRadius: "10px", marginBottom: "10px",
                                background: `${color}06`, border: `1px solid ${color}18`,
                                borderLeft: `3px solid ${color}55`,
                                fontFamily: T.mono, fontSize: "13px", color: T.text, lineHeight: "1.8",
                              }}>{fix}</div>
                            );
                          })
                        : <p style={{ fontFamily: T.mono, fontSize: "13px", color: T.green }}>
                            ✓ No remediation steps required.
                          </p>
                      }
                    </div>
                  )}

                </div>
              </Card>
            </>
          )}

          {/* ── Footer ── */}
          <div style={{ textAlign: "center", padding: "48px 0 24px",
            fontFamily: T.mono, fontSize: "10px", color: T.muted, letterSpacing: "0.15em" }}>
            CHAINGUARD SECURITY PLATFORM · AUTOMATED SMART CONTRACT AUDIT
          </div>
        </main>
      </div>
    </>
  );
}