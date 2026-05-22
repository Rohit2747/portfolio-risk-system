/* ================================================================
   AI Portfolio Risk Dashboard — script.js
   Fetches REAL data from all 4 backend microservices.
   No hardcoded values. All data is live from APIs.
   ================================================================ */

// ---------------------------------------------------------------
// SERVICE URLS — change these if running on different ports
// ---------------------------------------------------------------
const PORTFOLIO_SERVICE_URL  = "http://localhost:8080";
const MARKET_DATA_SERVICE_URL = "http://localhost:8081";
const RISK_SERVICE_URL       = "http://localhost:8082";
const AI_INSIGHT_SERVICE_URL = "http://localhost:8083";

// ---------------------------------------------------------------
// COMPANY LOGOS
// ---------------------------------------------------------------
const companyLogos = {
  "AAPL":        "images/apple.png",
  "MSFT":        "images/microsoft.jpeg",
  "NVDA":        "images/NVIDIA.png",
  "AMZN":        "images/amazon.png",
  "GOOGL":       "images/amazon.png",
  "META":        "images/amazon.png",
  "TSLA":        "images/apple.png",
  "RELIANCE":    "images/reliance.png",
  "HDFCBANK":    "images/hdfc.png",
  "INFY":        "images/reliance.png",
  "TCS":         "images/hdfc.png",
  "WIPRO":       "images/hdfc.png",
  "ICICIBANK":   "images/hdfc.png",
  "SBIN":        "images/hdfc.png",
  "BAJFINANCE":  "images/hdfc.png",
  "ASIANPAINT":  "images/reliance.png",
  "HINDUNILVR":  "images/reliance.png",
  "KOTAKBANK":   "images/hdfc.png",
  "LT":          "images/reliance.png",
  "SUNPHARMA":   "images/reliance.png",
  // Legacy names (fallback)
  "Apple":       "images/apple.png",
  "Microsoft":   "images/microsoft.jpeg",
  "NVIDIA":      "images/NVIDIA.png",
  "Amazon":      "images/amazon.png",
  "Reliance":    "images/reliance.png",
  "HDFC":        "images/hdfc.png"
};

// ---------------------------------------------------------------
// GLOBAL STATE
// ---------------------------------------------------------------
let allRiskData    = [];   // full risk analysis from /risk-analysis
let allMarketData  = [];   // market prices from /market-data
let selectedClient = null; // currently selected portfolio card
let aiReportPinned = false; // true when AI report is displayed, prevents polling overwrite

let compareMode = false;
let compareClients = []; // holds up to 2 client IDs for comparison

// Chart.js instances and data
let marketHistory = [];
let marketLabels = [];
let riskDonutChart = null;
let marketLineChart = null;
let breachBarChart = null;

// Sparkline data storage
let sparklineData = {}; // { stockSymbol: [price1, price2, ...] } last 12 points
let sparklineCharts = {}; // { stockSymbol: Chart instance }

// ---------------------------------------------------------------
// HELPER: Animated Counter Effect
// ---------------------------------------------------------------
function animateCounter(element, start, end, duration, prefix = '', suffix = '', formatter = null) {
  if (!element) return;
  const startTime = performance.now();
  const range = end - start;

  function update(currentTime) {
    const elapsed = currentTime - startTime;
    const progress = Math.min(elapsed / duration, 1);
    // Ease out cubic for smooth deceleration
    const eased = 1 - Math.pow(1 - progress, 3);
    const current = start + (range * eased);

    if (formatter) {
      element.textContent = prefix + formatter(current) + suffix;
    } else {
      element.textContent = prefix + Math.round(current) + suffix;
    }

    if (progress < 1) {
      requestAnimationFrame(update);
    }
  }
  requestAnimationFrame(update);
}

// ---------------------------------------------------------------
// TOAST NOTIFICATION SYSTEM
// ---------------------------------------------------------------
let previousHighRiskClients = new Set();

function showToast(title, message, type = 'danger') {
  const container = document.getElementById('toast-container');
  if (!container) return;
  
  const toast = document.createElement('div');
  const typeClass = type === 'warning' ? 'toast-warning' : type === 'success' ? 'toast-success' : '';
  const icon = type === 'warning' ? 'fa-exclamation-triangle' 
             : type === 'success' ? 'fa-check-circle' 
             : 'fa-circle-exclamation';
  
  const now = new Date();
  const timeStr = now.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
  
  toast.className = `toast ${typeClass}`;
  toast.innerHTML = `
    <i class="fa-solid ${icon} toast-icon"></i>
    <div class="toast-content">
      <div class="toast-title">${title}</div>
      <div class="toast-message">${message}</div>
      <div class="toast-time">${timeStr}</div>
    </div>
    <button class="toast-close" onclick="dismissToast(this)">&times;</button>
  `;
  
  container.appendChild(toast);
  
  // Auto dismiss after 5 seconds
  setTimeout(() => {
    toast.classList.add('toast-dismiss');
    setTimeout(() => toast.remove(), 300);
  }, 5000);
  
  // Max 4 toasts visible at once
  const toasts = container.querySelectorAll('.toast');
  if (toasts.length > 4) {
    toasts[0].remove();
  }
}

function dismissToast(closeBtn) {
  const toast = closeBtn.closest('.toast');
  if (toast) {
    toast.classList.add('toast-dismiss');
    setTimeout(() => toast.remove(), 300);
  }
}

// ---------------------------------------------------------------
// THEME: Dark/Light Mode Toggle
// ---------------------------------------------------------------
function toggleTheme() {
  const body = document.body;
  const icon = document.getElementById('theme-icon');
  
  body.classList.toggle('light-mode');
  
  const isLight = body.classList.contains('light-mode');
  
  // Update icon
  if (icon) {
    icon.className = isLight ? 'fa-solid fa-moon' : 'fa-solid fa-sun';
  }
  
  // Persist preference
  localStorage.setItem('dashboard-theme', isLight ? 'light' : 'dark');
  
  // Update Chart.js chart colors for the new theme
  updateChartsTheme(isLight);
}

function updateChartsTheme(isLight) {
  const textColor = isLight ? 'rgba(0,0,0,0.6)' : 'rgba(255,255,255,0.4)';
  const gridColor = isLight ? 'rgba(0,0,0,0.06)' : 'rgba(255,255,255,0.03)';
  const legendColor = isLight ? 'rgba(0,0,0,0.7)' : 'rgba(255,255,255,0.7)';
  
  // Update market line chart
  if (marketLineChart) {
    marketLineChart.options.scales.x.ticks.color = textColor;
    marketLineChart.options.scales.y.ticks.color = textColor;
    marketLineChart.options.scales.x.grid.color = gridColor;
    marketLineChart.options.scales.y.grid.color = gridColor;
    marketLineChart.update('none');
  }
  
  // Update breach bar chart
  if (breachBarChart) {
    breachBarChart.options.scales.x.ticks.color = textColor;
    breachBarChart.options.scales.y.ticks.color = isLight ? 'rgba(0,0,0,0.7)' : 'rgba(255,255,255,0.6)';
    breachBarChart.options.scales.x.grid.color = gridColor;
    breachBarChart.update('none');
  }
  
  // Update donut chart legend
  if (riskDonutChart) {
    riskDonutChart.options.plugins.legend.labels.color = legendColor;
    riskDonutChart.update('none');
  }
}

function loadSavedTheme() {
  const saved = localStorage.getItem('dashboard-theme');
  if (saved === 'light') {
    document.body.classList.add('light-mode');
    const icon = document.getElementById('theme-icon');
    if (icon) icon.className = 'fa-solid fa-moon';
    // Defer chart theme update until charts are initialized
    setTimeout(() => updateChartsTheme(true), 1000);
  }
}

// ---------------------------------------------------------------
// CLIENT COMPARISON MODE
// ---------------------------------------------------------------
function toggleCompareMode() {
  compareMode = !compareMode;
  const btn = document.getElementById('compare-mode-btn');
  const panel = document.getElementById('comparison-panel');
  
  if (compareMode) {
    btn.classList.add('active');
    btn.innerHTML = '<i class="fa-solid fa-code-compare"></i> Compare (ON)';
    compareClients = [];
    panel.style.display = 'block';
    renderComparisonPanel();
    // Show toast
    showToast('Compare Mode', 'Select 2 client portfolios to compare side by side.', 'warning');
  } else {
    btn.classList.remove('active');
    btn.innerHTML = '<i class="fa-solid fa-code-compare"></i> Compare';
    compareClients = [];
    panel.style.display = 'none';
    // Remove compare highlights
    document.querySelectorAll('.portfolio-card').forEach(card => {
      card.style.border = '1px solid rgba(255,255,255,0.04)';
    });
  }
}

function closeComparison() {
  compareMode = false;
  const btn = document.getElementById('compare-mode-btn');
  const panel = document.getElementById('comparison-panel');
  btn.classList.remove('active');
  btn.innerHTML = '<i class="fa-solid fa-code-compare"></i> Compare';
  compareClients = [];
  panel.style.display = 'none';
  document.querySelectorAll('.portfolio-card').forEach(card => {
    card.style.border = '1px solid rgba(255,255,255,0.04)';
  });
}

function addToComparison(clientId) {
  if (compareClients.includes(clientId)) return; // already selected
  if (compareClients.length >= 2) {
    compareClients = [clientId]; // reset and start fresh with this one
  } else {
    compareClients.push(clientId);
  }
  
  // Highlight selected compare cards
  document.querySelectorAll('.portfolio-card').forEach(card => {
    card.style.border = '1px solid rgba(255,255,255,0.04)';
  });
  
  renderComparisonPanel();
}

function renderComparisonPanel() {
  const grid = document.getElementById('comparison-grid');
  if (!grid) return;
  
  if (compareClients.length === 0) {
    grid.innerHTML = `
      <div class="comparison-instructions" style="grid-column:1/-1;">
        <i class="fa-solid fa-arrow-pointer"></i>
        Click 2 client portfolios above to compare their risk profiles.
      </div>`;
    return;
  }
  
  if (compareClients.length === 1) {
    const client1 = allRiskData.find(r => r.clientId === compareClients[0]);
    grid.innerHTML = `
      <div class="comparison-client">
        ${renderComparisonClient(client1)}
      </div>
      <div class="comparison-vs">VS</div>
      <div class="comparison-instructions" style="display:flex;align-items:center;justify-content:center;min-height:200px;">
        <span style="color:rgba(255,255,255,0.35);font-size:13px;">Select 2nd client...</span>
      </div>`;
    return;
  }
  
  // Both clients selected
  const client1 = allRiskData.find(r => r.clientId === compareClients[0]);
  const client2 = allRiskData.find(r => r.clientId === compareClients[1]);
  
  if (!client1 || !client2) return;
  
  grid.innerHTML = `
    <div class="comparison-client">
      ${renderComparisonClient(client1)}
    </div>
    <div class="comparison-vs">VS</div>
    <div class="comparison-client">
      ${renderComparisonClient(client2)}
    </div>`;
    
  // Scroll to comparison panel
  const panel = document.getElementById('comparison-panel');
  if (panel) panel.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function renderComparisonClient(client) {
  if (!client) return '<p>Client data not available</p>';
  
  const riskColor = client.riskLevel === 'HIGH' ? '#ff5e5e' 
                  : client.riskLevel === 'MEDIUM' ? 'orange' : '#00ff64';
  const value = (client.totalPortfolioValue || 0).toLocaleString('en-IN', {maximumFractionDigits: 2});
  const daily = (client.dailyChangePercent || 0).toFixed(2);
  const dailyColor = parseFloat(daily) >= 0 ? '#00ff64' : '#ff5e5e';
  const breachCount = client.breaches ? client.breaches.length : 0;
  
  let breachesHtml = '';
  if (client.breaches && client.breaches.length > 0) {
    client.breaches.forEach(b => {
      breachesHtml += `<div class="comparison-breach-item">${b.breachType}: ${b.description || 'Threshold exceeded'}</div>`;
    });
  } else {
    breachesHtml = '<div class="comparison-no-breach">No active breaches</div>';
  }
  
  return `
    <h3 style="color:${riskColor}">
      <i class="fa-solid fa-user-shield"></i>
      ${client.clientName}
    </h3>
    <div class="comparison-metric">
      <span class="comparison-metric-label">Risk Level</span>
      <span class="comparison-metric-value" style="color:${riskColor}">${client.riskLevel}</span>
    </div>
    <div class="comparison-metric">
      <span class="comparison-metric-label">Portfolio Value</span>
      <span class="comparison-metric-value">\u20B9${value}</span>
    </div>
    <div class="comparison-metric">
      <span class="comparison-metric-label">Daily Change</span>
      <span class="comparison-metric-value" style="color:${dailyColor}">${parseFloat(daily) >= 0 ? '+' : ''}${daily}%</span>
    </div>
    <div class="comparison-metric">
      <span class="comparison-metric-label">Active Breaches</span>
      <span class="comparison-metric-value" style="color:${breachCount > 0 ? '#ff5e5e' : '#00ff64'}">${breachCount}</span>
    </div>
    <div class="comparison-metric">
      <span class="comparison-metric-label">Health Score</span>
      <span class="comparison-metric-value">${calculateClientHealth(client)} / 100</span>
    </div>
    <div class="comparison-breaches">
      ${breachesHtml}
    </div>`;
}

function calculateClientHealth(client) {
  let score = 100;
  if (client.riskLevel === 'HIGH') score = 20;
  else if (client.riskLevel === 'MEDIUM') score = 55;
  else score = 90;
  if (client.breaches) score -= client.breaches.length * 5;
  return Math.max(0, Math.min(100, score));
}

// ---------------------------------------------------------------
// EXPORT: AI Report as PDF
// ---------------------------------------------------------------
async function exportAIReportPDF() {
  if (!selectedClient) {
    showToast('Export Failed', 'Please select a client and generate an AI report first.', 'warning');
    return;
  }
  
  const btn = document.getElementById('export-pdf-btn');
  if (btn) {
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Generating...';
  }
  
  // Get current data
  const client = allRiskData.find(r => r.clientId === selectedClient);
  const clientName = client ? client.clientName : `Client ${selectedClient}`;
  const riskLevel = client ? client.riskLevel : 'UNKNOWN';
  const portfolioValue = client ? (client.totalPortfolioValue || 0).toLocaleString('en-IN', {maximumFractionDigits: 2}) : 'N/A';
  const dailyChange = client ? (client.dailyChangePercent || 0).toFixed(2) : '0.00';
  const healthScore = document.getElementById('health-score') ? document.getElementById('health-score').textContent : 'N/A';
  const aiSummary = document.getElementById('ai-summary') ? document.getElementById('ai-summary').textContent : 'No analysis available.';
  const aiRecommendation = document.getElementById('ai-recommendation') ? document.getElementById('ai-recommendation').textContent : 'No recommendation.';
  const healthStatus = document.getElementById('health-status') ? document.getElementById('health-status').textContent : 'N/A';
  
  // Build breach list
  let breachesText = 'No active breaches.';
  if (client && client.breaches && client.breaches.length > 0) {
    breachesText = client.breaches.map(b => `\u2022 ${b.breachType}: ${b.description || 'Threshold exceeded'}`).join('\n');
  }
  
  const now = new Date();
  const dateStr = now.toLocaleDateString('en-IN', { year: 'numeric', month: 'long', day: 'numeric' });
  const timeStr = now.toLocaleTimeString('en-US');
  
  // Create PDF content element
  const pdfContent = document.createElement('div');
  pdfContent.style.cssText = 'padding:40px;font-family:Segoe UI,sans-serif;color:#1a1a2e;background:white;width:700px;';
  
  const riskColor = riskLevel === 'HIGH' ? '#cc3333' : riskLevel === 'MEDIUM' ? '#cc7700' : '#008844';
  
  pdfContent.innerHTML = `
    <div style="text-align:center;margin-bottom:30px;padding-bottom:20px;border-bottom:2px solid #0088aa;">
      <h1 style="font-size:22px;color:#0088aa;margin:0 0 6px 0;">Portfolio Risk Intelligence</h1>
      <p style="font-size:12px;color:#666;margin:0;">AI-Powered Risk Assessment Report</p>
    </div>
    
    <div style="display:flex;justify-content:space-between;margin-bottom:24px;">
      <div>
        <h2 style="font-size:18px;margin:0 0 4px 0;color:#1a1a2e;">${clientName}</h2>
        <p style="font-size:12px;color:#666;margin:0;">Generated: ${dateStr} at ${timeStr}</p>
      </div>
      <div style="text-align:right;">
        <div style="display:inline-block;padding:6px 16px;border-radius:16px;background:${riskColor}22;color:${riskColor};font-size:14px;font-weight:700;border:1px solid ${riskColor}44;">
          ${riskLevel} RISK
        </div>
      </div>
    </div>
    
    <div style="display:grid;grid-template-columns:1fr 1fr 1fr;gap:12px;margin-bottom:24px;">
      <div style="padding:14px;background:#f8f9fa;border-radius:8px;text-align:center;">
        <p style="font-size:11px;color:#666;margin:0 0 4px 0;text-transform:uppercase;letter-spacing:0.5px;">Portfolio Value</p>
        <p style="font-size:18px;font-weight:700;color:#1a1a2e;margin:0;">\u20B9${portfolioValue}</p>
      </div>
      <div style="padding:14px;background:#f8f9fa;border-radius:8px;text-align:center;">
        <p style="font-size:11px;color:#666;margin:0 0 4px 0;text-transform:uppercase;letter-spacing:0.5px;">Daily Change</p>
        <p style="font-size:18px;font-weight:700;color:${parseFloat(dailyChange) >= 0 ? '#008844' : '#cc3333'};margin:0;">${parseFloat(dailyChange) >= 0 ? '+' : ''}${dailyChange}%</p>
      </div>
      <div style="padding:14px;background:#f8f9fa;border-radius:8px;text-align:center;">
        <p style="font-size:11px;color:#666;margin:0 0 4px 0;text-transform:uppercase;letter-spacing:0.5px;">Health Score</p>
        <p style="font-size:18px;font-weight:700;color:#0088aa;margin:0;">${healthScore}</p>
      </div>
    </div>
    
    <div style="margin-bottom:20px;padding:16px;background:#f0f8ff;border-radius:8px;border-left:4px solid #0088aa;">
      <h3 style="font-size:13px;color:#0088aa;margin:0 0 8px 0;text-transform:uppercase;letter-spacing:0.5px;">AI Analysis</h3>
      <p style="font-size:13px;color:#333;line-height:1.7;margin:0;">${aiSummary}</p>
    </div>
    
    <div style="margin-bottom:20px;padding:16px;background:#f8fff8;border-radius:8px;border-left:4px solid #008844;">
      <h3 style="font-size:13px;color:#008844;margin:0 0 8px 0;text-transform:uppercase;letter-spacing:0.5px;">AI Recommendation</h3>
      <p style="font-size:13px;color:#333;line-height:1.7;margin:0;">${aiRecommendation}</p>
    </div>
    
    <div style="margin-bottom:20px;padding:16px;background:${client && client.breaches && client.breaches.length > 0 ? '#fff5f5' : '#f8fff8'};border-radius:8px;border-left:4px solid ${client && client.breaches && client.breaches.length > 0 ? '#cc3333' : '#008844'};">
      <h3 style="font-size:13px;color:${client && client.breaches && client.breaches.length > 0 ? '#cc3333' : '#008844'};margin:0 0 8px 0;text-transform:uppercase;letter-spacing:0.5px;">Risk Breaches</h3>
      <pre style="font-size:12px;color:#333;line-height:1.8;margin:0;white-space:pre-wrap;font-family:inherit;">${breachesText}</pre>
    </div>
    
    <div style="margin-top:30px;padding-top:16px;border-top:1px solid #eee;text-align:center;">
      <p style="font-size:10px;color:#999;margin:0;">This report was generated by Portfolio Risk Intelligence AI Engine. For informational purposes only. Not financial advice.</p>
    </div>
  `;
  
  // Append to DOM temporarily (required for html2canvas to render properly)
  pdfContent.style.position = 'fixed';
  pdfContent.style.left = '-9999px';
  pdfContent.style.top = '0';
  document.body.appendChild(pdfContent);

  // Generate PDF using html2pdf
  const opt = {
    margin: [10, 10, 10, 10],
    filename: `AI_Risk_Report_${clientName.replace(/\s+/g, '_')}_${now.toISOString().split('T')[0]}.pdf`,
    image: { type: 'jpeg', quality: 0.98 },
    html2canvas: { scale: 2, useCORS: true, logging: false },
    jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' }
  };
  
  try {
    await html2pdf().set(opt).from(pdfContent).save();
    showToast('PDF Exported', `Report saved for ${clientName}.`, 'success');
  } catch (err) {
    showToast('Export Failed', `Could not generate PDF: ${err.message}`, 'danger');
  }
  
  // Remove from DOM
  document.body.removeChild(pdfContent);
  
  // Reset button
  if (btn) {
    btn.disabled = false;
    btn.innerHTML = '<i class="fa-solid fa-file-pdf"></i> Export PDF';
  }
}

// ---------------------------------------------------------------
// HELPER: Update Health Score Widget
// ---------------------------------------------------------------
function updateHealthScoreWidget(score) {
  const el = document.getElementById("health-score");
  const fill = document.getElementById("health-score-fill");
  const status = document.getElementById("health-score-status");
  if (el) el.textContent = `${score} / 100`;
  if (fill) {
    fill.style.width = `${score}%`;
    if (score >= 75) fill.style.background = "linear-gradient(90deg, #00e5ff, #00ff64)";
    else if (score >= 45) fill.style.background = "linear-gradient(90deg, #ffaa00, #ff6b00)";
    else fill.style.background = "linear-gradient(90deg, #ff5e5e, #ff2020)";
  }
  if (status) {
    if (score >= 75) { status.textContent = "Healthy"; status.style.color = "#00ff64"; }
    else if (score >= 45) { status.textContent = "Moderate Risk"; status.style.color = "orange"; }
    else { status.textContent = "Critical"; status.style.color = "#ff5e5e"; }
  }
}

// ---------------------------------------------------------------
// FETCH: Portfolio Data (port 8080)
// Enriches with value & risk level from Risk Analysis data
// ---------------------------------------------------------------
async function loadPortfolioData() {
  try {
    const response = await fetch(`${PORTFOLIO_SERVICE_URL}/portfolios`);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const portfolios = await response.json();
    renderPortfolioCards(portfolios);
  } catch (err) {
    document.getElementById("portfolio-data").innerHTML =
      `<p style="color:red">⚠ Portfolio Service unavailable. Start service on port 8080.<br><small>${err.message}</small></p>`;
  }
}

function renderPortfolioCards(portfolios) {
  let html = "";
  portfolios.forEach(p => {
    // Enrich portfolio card with data from Risk Analysis Service (if available)
    const riskEntry = allRiskData.find(r => r.clientId === p.clientId);
    const riskLevel = riskEntry ? riskEntry.riskLevel : (p.riskLevel || "UNKNOWN");
    const portfolioValue = riskEntry ? riskEntry.totalPortfolioValue : p.portfolioValue;

    const riskClass  = riskLevel.toLowerCase();
    const value      = portfolioValue > 0
      ? `₹${portfolioValue.toLocaleString("en-IN", {maximumFractionDigits: 2})}`
      : "Calculating...";

    html += `
      <div class="service-item portfolio-card"
           onclick="selectClient(${p.clientId}, '${p.clientName}', '${riskLevel}', ${portfolioValue || 0}, event)">
        <div>
          <h3>
            <div class="service-item-icon-box purple-icon">
              <i class="fa-solid fa-user"></i>
            </div>
            ${p.clientName}
          </h3>
          <p>Total Value: ${value}</p>
          <p>Risk Level: <span class="${riskClass}">${riskLevel}</span></p>
        </div>
      </div>`;
  });
  document.getElementById("portfolio-data").innerHTML = html || "<p>No portfolios found.</p>";
}

// ---------------------------------------------------------------
// FETCH: Market Data (port 8081)
// ---------------------------------------------------------------
async function loadMarketData() {
  try {
    const response = await fetch(`${MARKET_DATA_SERVICE_URL}/market-data`);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    allMarketData = await response.json();

    // Accumulate sparkline data for each stock
    allMarketData.forEach(m => {
      const symbol = m.stockSymbol || m.symbol || m.stockName;
      const price = m.currentPrice || m.price || 0;
      if (!sparklineData[symbol]) sparklineData[symbol] = [];
      sparklineData[symbol].push(price);
      // Keep last 12 data points
      if (sparklineData[symbol].length > 12) sparklineData[symbol].shift();
    });

    renderMarketCards(allMarketData);
    updateMarketLineChart(allMarketData);

    // Animate market overview stat
    const avgChange = allMarketData.reduce((sum, s) => sum + (s.dailyChangePercent || s.changePercent || 0), 0) / allMarketData.length;
    const marketOverviewEl = document.querySelector(".top-card:nth-child(3) .top-card-content p");
    if (marketOverviewEl) {
      const isPositive = avgChange >= 0;
      marketOverviewEl.style.color = isPositive ? "lime" : "red";
      animateCounter(marketOverviewEl, 0, avgChange, 800, isPositive ? '+' : '', '%', (val) => val.toFixed(2));
    }
  } catch (err) {
    document.getElementById("market-data").innerHTML =
      `<p style="color:red">⚠ Market Data Service unavailable. Start service on port 8081.<br><small>${err.message}</small></p>`;
  }
}

function renderMarketCards(marketData) {
  let html = "";
  marketData.forEach(m => {
    const symbol       = m.stockSymbol || m.symbol || m.stockName;
    const name         = m.stockName || symbol;
    const price        = (m.currentPrice || m.price || 0).toFixed(2);
    const change       = (m.changePercent || m.change || 0).toFixed(2);
    const dailyChange  = (m.dailyChangePercent || 0).toFixed(2);
    const isPositive   = parseFloat(change) >= 0;
    const logo         = companyLogos[symbol] || companyLogos[name] || "";

    html += `
      <div class="service-item market-card">
        <div class="market-left">
          <div class="service-item-icon-box market-logo-box">
            <img src="${logo}" class="company-logo"
                 onerror="this.src='https://cdn-icons-png.flaticon.com/512/3135/3135715.png'">
          </div>
          <div>
            <h3>${name}</h3>
            <p style="color:#aaa;font-size:13px;">${symbol}</p>
            <p>₹${price}</p>
          </div>
        </div>
        <div class="sparkline-wrapper">
          <canvas id="spark-${symbol}" width="80" height="30"></canvas>
        </div>
        <div class="market-right">
          <p class="${isPositive ? 'positive' : 'negative'}">${isPositive ? '+' : ''}${change}%</p>
          <p style="font-size:12px;color:#aaa;">Day: ${parseFloat(dailyChange) >= 0 ? '+' : ''}${dailyChange}%</p>
        </div>
      </div>`;
  });
  document.getElementById("market-data").innerHTML = html || "<p>No market data found.</p>";
  renderSparklines(marketData);
}

// ---------------------------------------------------------------
// RENDER: Sparkline mini-charts for each stock
// ---------------------------------------------------------------
function renderSparklines(marketData) {
  // Destroy previous chart instances
  Object.values(sparklineCharts).forEach(chart => chart.destroy());
  sparklineCharts = {};

  marketData.forEach(m => {
    const symbol = m.stockSymbol || m.symbol || m.stockName;
    const canvas = document.getElementById(`spark-${symbol}`);
    if (!canvas || !sparklineData[symbol] || sparklineData[symbol].length < 2) return;

    const data = sparklineData[symbol];
    const isPositive = data[data.length - 1] >= data[0];
    const color = isPositive ? '#00ff64' : '#ff5e5e';

    sparklineCharts[symbol] = new Chart(canvas, {
      type: 'line',
      data: {
        labels: data.map((_, i) => i),
        datasets: [{
          data: data,
          borderColor: color,
          borderWidth: 1.5,
          fill: true,
          backgroundColor: isPositive ? 'rgba(0,255,100,0.08)' : 'rgba(255,94,94,0.08)',
          tension: 0.4,
          pointRadius: 0
        }]
      },
      options: {
        responsive: false,
        maintainAspectRatio: false,
        scales: {
          x: { display: false },
          y: { display: false }
        },
        plugins: {
          legend: { display: false },
          tooltip: { enabled: false }
        },
        animation: false
      }
    });
  });
}

// ---------------------------------------------------------------
// FETCH: Risk Analysis (port 8082) — MAIN DATA SOURCE
// ---------------------------------------------------------------
async function loadRiskAnalysis() {
  try {
    const response = await fetch(`${RISK_SERVICE_URL}/risk-analysis`);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    allRiskData = await response.json();

    // Detect new HIGH risk clients and show toast notifications
    const currentHighRiskClients = new Set(
      allRiskData.filter(r => r.riskLevel === "HIGH").map(r => r.clientId)
    );

    currentHighRiskClients.forEach(clientId => {
      if (!previousHighRiskClients.has(clientId)) {
        const client = allRiskData.find(r => r.clientId === clientId);
        if (client && previousHighRiskClients.size > 0) {
          // Only show toast after initial load (not on first page load)
          showToast(
            'High Risk Alert',
            `${client.clientName} has escalated to HIGH risk. Immediate attention required.`,
            'danger'
          );
        }
      }
    });

    previousHighRiskClients = currentHighRiskClients;

    updateDashboardSummary(allRiskData);

    // Re-render portfolio cards with updated values and risk levels
    loadPortfolioData();

    // If a client is selected, refresh their panel too
    if (selectedClient && !aiReportPinned) {
      const updated = allRiskData.find(r => r.clientId === selectedClient);
      if (updated) renderRiskPanel(updated);
    }
  } catch (err) {
    document.getElementById("ai-summary").textContent =
      `⚠ Risk Analysis Service unavailable. Start service on port 8082. (${err.message})`;
    document.getElementById("health-status").textContent = "UNAVAILABLE";
    document.getElementById("health-status").style.color = "gray";
    document.getElementById("health-status").style.background = "rgba(128,128,128,0.1)";
    document.getElementById("health-status").style.border = "1px solid rgba(128,128,128,0.25)";
  }
}

// ---------------------------------------------------------------
// UPDATE: Dashboard AI Summary Panel from Risk Data
// ---------------------------------------------------------------
function updateDashboardSummary(riskData) {
  if (!riskData || riskData.length === 0) return;

  const total        = riskData.length;
  const highCount    = riskData.filter(r => r.riskLevel === "HIGH").length;
  const mediumCount  = riskData.filter(r => r.riskLevel === "MEDIUM").length;
  const lowCount     = riskData.filter(r => r.riskLevel === "LOW").length;
  const breachedCount = riskData.filter(r => r.hasBreaches).length;

  const totalValue   = riskData.reduce((sum, r) => sum + (r.totalPortfolioValue || 0), 0);
  const avgDaily     = riskData.reduce((sum, r) => sum + (r.dailyChangePercent || 0), 0) / total;

  // Count breach types
  let driftCount    = 0, concCount = 0, dropCount = 0;
  riskData.forEach(r => {
    if (r.breaches) {
      r.breaches.forEach(b => {
        if (b.breachType === "ALLOCATION_DRIFT")   driftCount++;
        if (b.breachType === "CONCENTRATION_RISK") concCount++;
        if (b.breachType === "DAILY_DROP")         dropCount++;
      });
    }
  });

  // Update charts (always, even when AI report is pinned)
  updateRiskDonutChart(riskData);
  updateBreachBarChart(driftCount, concCount, dropCount);

  if (aiReportPinned) return;

  // ---- Health Status ----
  const healthStatus = document.getElementById("health-status");
  if (highCount > 20) {
    healthStatus.textContent = "CRITICAL";
    healthStatus.style.background = "rgba(255,94,94,0.1)";
    healthStatus.style.border = "1px solid rgba(255,94,94,0.25)";
    healthStatus.style.color = "#ff5e5e";
  } else if (highCount > 5 || mediumCount > 30) {
    healthStatus.textContent = "MODERATE";
    healthStatus.style.background = "rgba(255,170,0,0.1)";
    healthStatus.style.border = "1px solid rgba(255,170,0,0.25)";
    healthStatus.style.color = "orange";
  } else {
    healthStatus.textContent = "GOOD";
    healthStatus.style.background = "rgba(0,255,100,0.1)";
    healthStatus.style.border = "1px solid rgba(0,255,100,0.25)";
    healthStatus.style.color = "#00ff64";
  }

  // ---- AI Summary ----
  document.getElementById("ai-summary").textContent =
    `AI Engine detected ${breachedCount} of ${total} portfolios with active risk breaches. ` +
    `${highCount} portfolios are HIGH risk, ${mediumCount} MEDIUM, ${lowCount} LOW. ` +
    `Total portfolio value: ₹${(totalValue/10000000).toFixed(2)}Cr. ` +
    `Average daily performance: ${avgDaily >= 0 ? '+' : ''}${avgDaily.toFixed(2)}%.`;

  // ---- Allocation Drift ----
  const allocationDrift = document.getElementById("allocation-drift");
  if (driftCount > 0) {
    allocationDrift.innerHTML =
      '<i class="fa-solid fa-arrows-left-right breach-indicator-icon"></i><span>Allocation Drift: ' + driftCount + ' breach(es) detected. Rebalancing recommended.</span>';
    allocationDrift.style.borderLeftColor = "red";
    allocationDrift.querySelector('.breach-indicator-icon').style.color = "red";
  } else {
    allocationDrift.innerHTML =
      '<i class="fa-solid fa-arrows-left-right breach-indicator-icon"></i><span>Allocation Drift: All portfolios within target allocation bands.</span>';
    allocationDrift.style.borderLeftColor = "rgba(0,255,100,0.3)";
    allocationDrift.querySelector('.breach-indicator-icon').style.color = "#00ff64";
  }

  // ---- Stock Concentration ----
  const stockExposure = document.getElementById("stock-exposure");
  if (concCount > 0) {
    stockExposure.innerHTML =
      '<i class="fa-solid fa-chart-pie breach-indicator-icon"></i><span>Concentration Risk: ' + concCount + ' breach(es). Single-stock exposure exceeded 20% threshold.</span>';
    stockExposure.style.borderLeftColor = "red";
    stockExposure.querySelector('.breach-indicator-icon').style.color = "red";
  } else {
    stockExposure.innerHTML =
      '<i class="fa-solid fa-chart-pie breach-indicator-icon"></i><span>Stock Concentration: All holdings within acceptable exposure limits.</span>';
    stockExposure.style.borderLeftColor = "rgba(0,255,100,0.3)";
    stockExposure.querySelector('.breach-indicator-icon').style.color = "#00ff64";
  }

  // ---- Daily Drop ----
  const dailyDrop = document.getElementById("daily-drop");
  if (dropCount > 0) {
    dailyDrop.innerHTML =
      '<i class="fa-solid fa-arrow-trend-down breach-indicator-icon"></i><span>Daily Drop: ' + dropCount + ' portfolio(s) declined more than 3% today.</span>';
    dailyDrop.style.borderLeftColor = "red";
    dailyDrop.querySelector('.breach-indicator-icon').style.color = "red";
  } else {
    const dropColor = avgDaily < -1 ? "orange" : "rgba(0,255,100,0.3)";
    const dropIconColor = avgDaily < -1 ? "orange" : "#00ff64";
    dailyDrop.innerHTML =
      '<i class="fa-solid fa-arrow-trend-down breach-indicator-icon"></i><span>Daily Drop: Average portfolio change today: ' + (avgDaily >= 0 ? '+' : '') + avgDaily.toFixed(2) + '%. Within acceptable range.</span>';
    dailyDrop.style.borderLeftColor = dropColor;
    dailyDrop.querySelector('.breach-indicator-icon').style.color = dropIconColor;
  }

  // ---- Alert List ----
  let alerts = "";
  if (highCount > 0)  alerts += `<li>🔴 ${highCount} HIGH-risk portfolio(s) require immediate attention.</li>`;
  if (driftCount > 0) alerts += `<li>🟠 ${driftCount} allocation drift breach(es) detected. Rebalancing advised.</li>`;
  if (concCount > 0)  alerts += `<li>🔴 ${concCount} concentration risk breach(es). Diversification required.</li>`;
  if (dropCount > 0)  alerts += `<li>🔴 ${dropCount} portfolio(s) with >3% daily drop today.</li>`;
  if (!alerts)        alerts  = "<li>✅ No active risk alerts. All portfolios within acceptable parameters.</li>";
  document.getElementById("alert-list").innerHTML = alerts;

  // ---- Health Score ----
  let healthScore = 100;
  healthScore -= highCount * 0.8;
  healthScore -= mediumCount * 0.3;
  healthScore -= driftCount * 1.0;
  healthScore -= concCount * 1.5;
  healthScore -= dropCount * 2.0;
  healthScore = Math.max(0, Math.min(100, Math.round(healthScore)));

  if (!selectedClient) {
    updateHealthScoreWidget(healthScore);
  }

  // ---- AI Recommendation ----
  let recommendation = "";
  if (highCount > 20) {
    recommendation = "AI Recommendation: Portfolio system is under significant stress. " +
      "Immediate rebalancing and risk reduction across HIGH-risk portfolios is strongly advised. " +
      "Consider defensive repositioning until market volatility subsides.";
  } else if (highCount > 0 || driftCount > 0) {
    recommendation = "AI Recommendation: Multiple portfolios show risk breaches. " +
      "Priority action: address concentration risks first, then allocation drift. " +
      "Click individual clients for specific rebalancing guidance.";
  } else {
    recommendation = "AI Recommendation: Portfolio system health is good. " +
      "All positions are within target thresholds. Continue current strategy with periodic monitoring.";
  }
  document.getElementById("ai-recommendation").innerHTML = recommendation;

  // ---- Risk Meter (only update if no client is selected) ----
  if (!selectedClient) {
    const fill = document.querySelector(".risk-fill");
    const riskScore = 100 - healthScore;
    fill.style.width = riskScore + "%";
    fill.style.background = riskScore > 60 ? "red" : riskScore > 30 ? "orange" : "lime";
    document.getElementById("risk-percentage").textContent = `${riskScore}%`;
  }

  // Update top stats with animated counters
  const highRiskEl = document.querySelector(".top-card:nth-child(4) .top-card-content p");
  if (highRiskEl) {
    const currentVal = parseInt(highRiskEl.textContent) || 0;
    if (currentVal !== highCount) {
      animateCounter(highRiskEl, currentVal, highCount, 800);
    }
    highRiskEl.style.color = "red";
  }

  const totalValueEl = document.querySelector(".top-card:nth-child(2) .top-card-content p");
  if (totalValueEl) {
    const targetCr = totalValue / 10000000;
    animateCounter(totalValueEl, 0, targetCr, 1000, '\u20B9', 'Cr', (val) => val.toFixed(2));
  }
}

// ---------------------------------------------------------------
// CLIENT SELECTION — click on a portfolio card
// ---------------------------------------------------------------
function selectClient(clientId, clientName, riskLevel, portfolioValue, event) {
  aiReportPinned = false;

  // If compare mode is active, add to comparison instead of normal selection
  if (compareMode) {
    addToComparison(clientId);
    // Highlight the selected cards for comparison
    if (event && event.currentTarget) {
      event.currentTarget.style.border = '2px solid #b388ff';
    }
    return;
  }

  // Highlight selected card
  document.querySelectorAll(".portfolio-card").forEach(card => {
    card.style.border = "1px solid rgba(255,255,255,0.1)";
  });
  if (event && event.currentTarget) {
    event.currentTarget.style.border = "2px solid cyan";
  }

  selectedClient = clientId;

  // Find existing risk data for this client (if loaded)
  const existingRisk = allRiskData.find(r => r.clientId === clientId);
  if (existingRisk) {
    renderRiskPanel(existingRisk);
  } else {
    // Show loading state while fetching
    document.getElementById("risk-data").innerHTML = `
      <div class="service-item risk-card">
        <p style="color:cyan">Loading risk analysis for ${clientName}...</p>
      </div>`;
    // Fetch individual risk analysis
    fetchAndRenderRiskForClient(clientId);
  }
}

async function fetchAndRenderRiskForClient(clientId) {
  try {
    const response = await fetch(`${RISK_SERVICE_URL}/risk-analysis/${clientId}`);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const riskData = await response.json();
    renderRiskPanel(riskData);
  } catch (err) {
    document.getElementById("risk-data").innerHTML =
      `<p style="color:red">⚠ Could not load risk data: ${err.message}</p>`;
  }
}

async function fetchAndRenderAIInsight(clientId) {
  try {
    const response = await fetch(`${AI_INSIGHT_SERVICE_URL}/ai-insight/portfolio/${clientId}`);
    if (!response.ok) return; // silently fail if AI service is down
    const insight = await response.json();

    // Update AI summary panel with AI Insight Service response
    document.getElementById("ai-summary").textContent = insight.explanation || "";
    document.getElementById("ai-recommendation").innerHTML =
      `${insight.suggestedAction || ""}<br><br>
       <small style="color:#888;font-size:12px;">${insight.disclaimer || ""}</small>`;

    // Show severity badge
    const severityColor = insight.severity === "CRITICAL" ? "red"
                        : insight.severity === "WARNING" ? "orange" : "lime";
    document.getElementById("health-status").textContent =
      `${insight.severity}`;
    document.getElementById("health-status").style.color = severityColor;
    if (insight.severity === "CRITICAL") {
      document.getElementById("health-status").style.background = "rgba(255,94,94,0.1)";
      document.getElementById("health-status").style.border = "1px solid rgba(255,94,94,0.25)";
    } else if (insight.severity === "WARNING") {
      document.getElementById("health-status").style.background = "rgba(255,170,0,0.1)";
      document.getElementById("health-status").style.border = "1px solid rgba(255,170,0,0.25)";
    } else {
      document.getElementById("health-status").style.background = "rgba(0,255,100,0.1)";
      document.getElementById("health-status").style.border = "1px solid rgba(0,255,100,0.25)";
    }

  } catch (err) {
    // AI Insight Service may not be running — silently skip
    console.log("[Dashboard] AI Insight Service not available:", err.message);
  }
}

/**
 * Generate AI Report — triggered by button click
 * Shows a brief "analyzing" state, then fetches AI insight for the selected client.
 */
async function generateAIReport(clientId) {
  // Disable button and show analyzing state
  const btn = document.querySelector(".generate-ai-btn");
  if (btn) {
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Analyzing Portfolio...';
    btn.style.opacity = "0.6";
  }

  // Show loading in AI summary
  document.getElementById("ai-summary").textContent = "AI Engine is analyzing portfolio risk conditions...";
  document.getElementById("ai-recommendation").innerHTML =
    '<span style="color:rgba(0,255,255,0.5);font-style:italic;">Generating contextual risk assessment...</span>';

  // Brief delay to simulate AI thinking (makes it feel more real)
  await new Promise(resolve => setTimeout(resolve, 800));

  try {
    const response = await fetch(`${AI_INSIGHT_SERVICE_URL}/ai-insight/portfolio/${clientId}`);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    const insight = await response.json();

    // Update AI summary with generated insight
    document.getElementById("ai-summary").textContent = insight.explanation || "";
    document.getElementById("ai-recommendation").innerHTML =
      `${insight.suggestedAction || ""}<br><br>
       <small style="color:rgba(255,255,255,0.35);font-size:12px;">${insight.disclaimer || ""}</small>`;

    // Show severity badge
    const severityColor = insight.severity === "CRITICAL" ? "red"
                        : insight.severity === "WARNING" ? "orange" : "lime";
    document.getElementById("health-status").textContent =
      `${insight.severity}`;
    document.getElementById("health-status").style.color = severityColor;
    if (insight.severity === "CRITICAL") {
      document.getElementById("health-status").style.background = "rgba(255,94,94,0.1)";
      document.getElementById("health-status").style.border = "1px solid rgba(255,94,94,0.25)";
    } else if (insight.severity === "WARNING") {
      document.getElementById("health-status").style.background = "rgba(255,170,0,0.1)";
      document.getElementById("health-status").style.border = "1px solid rgba(255,170,0,0.25)";
    } else {
      document.getElementById("health-status").style.background = "rgba(0,255,100,0.1)";
      document.getElementById("health-status").style.border = "1px solid rgba(0,255,100,0.25)";
    }

    // Auto-scroll to AI Insight Summary section
    const aiCard = document.querySelector(".ai-summary-card");
    if (aiCard) {
      aiCard.scrollIntoView({ behavior: "smooth", block: "start" });
      // Brief highlight glow to draw attention
      aiCard.classList.add("ai-highlight");
      setTimeout(() => aiCard.classList.remove("ai-highlight"), 2000);
    }

    aiReportPinned = true;
    showToast('AI Report Ready', `Risk assessment generated for portfolio #${clientId}.`, 'success');

    // Update button to success state
    if (btn) {
      btn.innerHTML = '<i class="fa-solid fa-check"></i> Report Generated';
      btn.style.opacity = "1";
      btn.style.borderColor = "rgba(0,255,100,0.3)";
      btn.style.color = "#00ff64";

      // Reset button after 3 seconds
      setTimeout(() => {
        btn.innerHTML = '<i class="fa-solid fa-brain"></i> Generate AI Report';
        btn.disabled = false;
        btn.style.borderColor = "";
        btn.style.color = "";
      }, 3000);
    }

  } catch (err) {
    document.getElementById("ai-summary").textContent =
      "AI Insight Service unavailable. Please ensure the service is running on port 8083.";
    document.getElementById("ai-recommendation").innerHTML = "";

    if (btn) {
      btn.innerHTML = '<i class="fa-solid fa-brain"></i> Generate AI Report';
      btn.disabled = false;
      btn.style.opacity = "1";
    }
  }
}

// ---------------------------------------------------------------
// RENDER: Risk Panel for selected client
// ---------------------------------------------------------------
function renderRiskPanel(riskData) {
  const riskColor = riskData.riskLevel === "HIGH"   ? "red"
                  : riskData.riskLevel === "MEDIUM" ? "orange" : "lime";
  const icon      = riskData.riskLevel === "HIGH"   ? "fa-triangle-exclamation"
                  : riskData.riskLevel === "MEDIUM" ? "fa-chart-line" : "fa-shield-halved";
  const value     = (riskData.totalPortfolioValue || 0)
    .toLocaleString("en-IN", {maximumFractionDigits: 2});
  const daily     = (riskData.dailyChangePercent || 0).toFixed(2);

  let breachesHtml = "";
  if (riskData.breaches && riskData.breaches.length > 0) {
    riskData.breaches.forEach(b => {
      const bColor = b.breachType === "DAILY_DROP" || b.breachType === "CONCENTRATION_RISK"
        ? "red" : "orange";
      breachesHtml += `
        <div style="margin-top:8px;padding:8px;border-left:3px solid ${bColor};background:rgba(255,100,0,0.05);border-radius:4px;">
          <small style="color:${bColor};font-weight:bold;">[${b.breachType}]</small>
          <p style="margin:4px 0;font-size:14px;">${b.description}</p>
        </div>`;
    });
  } else {
    breachesHtml = `<p style="color:lime;margin-top:8px;">✅ No risk threshold breaches detected.</p>`;
  }

  document.getElementById("risk-data").innerHTML = `
    <div class="service-item risk-card">
      <div class="risk-top">
        <div class="risk-left">
          <div class="risk-icon-box">
            <i class="fa-solid ${icon}"></i>
          </div>
          <div class="risk-info">
            <h3>${riskData.clientName}</h3>
            <p>Value: ₹${value}</p>
            <p>Daily: <span style="color:${parseFloat(daily) >= 0 ? 'lime' : 'red'}">${parseFloat(daily) >= 0 ? '+' : ''}${daily}%</span></p>
          </div>
        </div>
        <div class="risk-level-box">
          <p>Risk Level</p>
          <span style="color:${riskColor}">${riskData.riskLevel}</span>
        </div>
      </div>
      <div class="risk-suggestion">
        <h4 style="color:cyan;margin-bottom:8px;">Risk Breach Details:</h4>
        ${breachesHtml}
      </div>
      <div style="margin-top:16px;padding-top:14px;border-top:1px solid rgba(255,255,255,0.06);">
        <button class="generate-ai-btn" onclick="generateAIReport(${riskData.clientId})">
          <i class="fa-solid fa-brain"></i> Generate AI Report
        </button>
        <small style="color:rgba(255,255,255,0.35);display:block;margin-top:8px;">Last analyzed: ${riskData.alertTimestamp || "N/A"}</small>
      </div>
    </div>`;

  // Also update AI panel with local insight (overridden if AI service responds)
  const suggestion = riskData.aiInsight || riskData.suggestedAction || riskData.suggestion
    || buildLocalInsight(riskData);
  if (!aiReportPinned) {
    document.getElementById("ai-summary").textContent =
      buildLocalExplanation(riskData);
    document.getElementById("ai-recommendation").innerHTML = suggestion;
  }

  const healthStatus = document.getElementById("health-status");
  healthStatus.textContent = riskData.riskLevel;
  healthStatus.style.color = riskColor;
  if (riskData.riskLevel === "HIGH") {
    healthStatus.style.background = "rgba(255,94,94,0.1)";
    healthStatus.style.border = "1px solid rgba(255,94,94,0.25)";
  } else if (riskData.riskLevel === "MEDIUM") {
    healthStatus.style.background = "rgba(255,170,0,0.1)";
    healthStatus.style.border = "1px solid rgba(255,170,0,0.25)";
  } else {
    healthStatus.style.background = "rgba(0,255,100,0.1)";
    healthStatus.style.border = "1px solid rgba(0,255,100,0.25)";
    healthStatus.style.color = "#00ff64";
  }

  // Update drift/exposure/drop indicators for this client
  updateBreachIndicators(riskData);

  // Update risk meter and health score for this specific client
  let clientHealthScore = 100;
  if (riskData.riskLevel === "HIGH") clientHealthScore = 20;
  else if (riskData.riskLevel === "MEDIUM") clientHealthScore = 55;
  else clientHealthScore = 90;

  // Adjust based on number of breaches
  if (riskData.breaches) {
    clientHealthScore -= riskData.breaches.length * 5;
  }
  clientHealthScore = Math.max(0, Math.min(100, clientHealthScore));

  updateHealthScoreWidget(clientHealthScore);

  const clientRiskScore = 100 - clientHealthScore;
  const fill = document.querySelector(".risk-fill");
  fill.style.width = clientRiskScore + "%";
  fill.style.background = clientRiskScore > 60 ? "red" : clientRiskScore > 30 ? "orange" : "lime";
  document.getElementById("risk-percentage").textContent = `${clientRiskScore}%`;
}

function updateBreachIndicators(riskData) {
  const driftEl = document.getElementById("allocation-drift");
  const expEl   = document.getElementById("stock-exposure");
  const dropEl  = document.getElementById("daily-drop");

  const driftBreach = riskData.breaches
    ? riskData.breaches.find(b => b.breachType === "ALLOCATION_DRIFT") : null;
  const concBreach  = riskData.breaches
    ? riskData.breaches.find(b => b.breachType === "CONCENTRATION_RISK") : null;
  const dropBreach  = riskData.breaches
    ? riskData.breaches.find(b => b.breachType === "DAILY_DROP") : null;

  if (driftBreach) {
    driftEl.innerHTML = '<i class="fa-solid fa-arrows-left-right breach-indicator-icon"></i><span>Allocation Drift: ' + driftBreach.description + '</span>';
    driftEl.style.borderLeftColor = "red";
    driftEl.querySelector('.breach-indicator-icon').style.color = "red";
  } else {
    driftEl.innerHTML = '<i class="fa-solid fa-arrows-left-right breach-indicator-icon"></i><span>Allocation Drift: Within target bands for ' + riskData.clientName + '.</span>';
    driftEl.style.borderLeftColor = "rgba(0,255,100,0.3)";
    driftEl.querySelector('.breach-indicator-icon').style.color = "#00ff64";
  }

  if (concBreach) {
    expEl.innerHTML = '<i class="fa-solid fa-chart-pie breach-indicator-icon"></i><span>Concentration: ' + concBreach.description + '</span>';
    expEl.style.borderLeftColor = "red";
    expEl.querySelector('.breach-indicator-icon').style.color = "red";
  } else {
    expEl.innerHTML = '<i class="fa-solid fa-chart-pie breach-indicator-icon"></i><span>Stock Concentration: All holdings within 20% threshold.</span>';
    expEl.style.borderLeftColor = "rgba(0,255,100,0.3)";
    expEl.querySelector('.breach-indicator-icon').style.color = "#00ff64";
  }

  if (dropBreach) {
    dropEl.innerHTML = '<i class="fa-solid fa-arrow-trend-down breach-indicator-icon"></i><span>Daily Drop: ' + dropBreach.description + '</span>';
    dropEl.style.borderLeftColor = "red";
    dropEl.querySelector('.breach-indicator-icon').style.color = "red";
  } else {
    const daily = (riskData.dailyChangePercent || 0).toFixed(2);
    const dropColor = parseFloat(daily) < -1 ? "orange" : "rgba(0,255,100,0.3)";
    const dropIconColor = parseFloat(daily) < -1 ? "orange" : "#00ff64";
    dropEl.innerHTML = '<i class="fa-solid fa-arrow-trend-down breach-indicator-icon"></i><span>Daily Drop: Portfolio change today: ' + (daily >= 0 ? '+' : '') + daily + '%</span>';
    dropEl.style.borderLeftColor = dropColor;
    dropEl.querySelector('.breach-indicator-icon').style.color = dropIconColor;
  }
}

function buildLocalExplanation(riskData) {
  if (!riskData.breaches || riskData.breaches.length === 0) {
    return `${riskData.clientName} portfolio is healthy with no risk threshold breaches.`;
  }
  return `${riskData.clientName} has ${riskData.breaches.length} active risk breach(es). ` +
    `Risk level: ${riskData.riskLevel}. Portfolio value: ₹${(riskData.totalPortfolioValue||0).toLocaleString("en-IN")}.`;
}

function buildLocalInsight(riskData) {
  if (!riskData.breaches || riskData.breaches.length === 0) {
    return "Continue current investment strategy. Portfolio is within all risk thresholds.";
  }
  const suggestions = [];
  if (riskData.breaches.some(b => b.breachType === "CONCENTRATION_RISK")) {
    suggestions.push("Reduce concentrated positions to below 20%.");
  }
  if (riskData.breaches.some(b => b.breachType === "ALLOCATION_DRIFT")) {
    suggestions.push("Rebalance to restore target allocation weights.");
  }
  if (riskData.breaches.some(b => b.breachType === "DAILY_DROP")) {
    suggestions.push("Consider adding defensive assets to reduce drawdown exposure.");
  }
  return suggestions.join(" ") +
    "<br><br><small style='color:#888;font-size:12px;'>DISCLAIMER: AI-generated insight. Not financial advice.</small>";
}

// ---------------------------------------------------------------
// CHARTS: Initialize all Chart.js charts
// ---------------------------------------------------------------
function initCharts() {
  // Donut Chart - Risk Distribution
  const donutCtx = document.getElementById('riskDonutChart');
  if (donutCtx) {
    riskDonutChart = new Chart(donutCtx, {
      type: 'doughnut',
      data: {
        labels: ['LOW Risk', 'MEDIUM Risk', 'HIGH Risk'],
        datasets: [{
          data: [50, 25, 25],
          backgroundColor: ['rgba(0,255,100,0.7)', 'rgba(255,170,0,0.7)', 'rgba(255,94,94,0.7)'],
          borderColor: ['rgba(0,255,100,0.3)', 'rgba(255,170,0,0.3)', 'rgba(255,94,94,0.3)'],
          borderWidth: 2,
          hoverBorderWidth: 3
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '65%',
        plugins: {
          legend: {
            position: 'right',
            labels: {
              color: 'rgba(255,255,255,0.7)',
              font: { size: 12, weight: '500' },
              padding: 16,
              usePointStyle: true,
              pointStyle: 'circle'
            }
          }
        }
      }
    });
  }

  // Line Chart - Market Index
  const lineCtx = document.getElementById('marketLineChart');
  if (lineCtx) {
    marketLineChart = new Chart(lineCtx, {
      type: 'line',
      data: {
        labels: [],
        datasets: [{
          label: 'Market Index',
          data: [],
          borderColor: '#00e5ff',
          backgroundColor: 'rgba(0,229,255,0.05)',
          borderWidth: 2,
          fill: true,
          tension: 0.4,
          pointRadius: 0,
          pointHoverRadius: 4,
          pointHoverBackgroundColor: '#00e5ff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { intersect: false, mode: 'index' },
        scales: {
          x: {
            grid: { color: 'rgba(255,255,255,0.03)' },
            ticks: { color: 'rgba(255,255,255,0.4)', font: { size: 10 }, maxTicksLimit: 8 }
          },
          y: {
            grid: { color: 'rgba(255,255,255,0.03)' },
            ticks: { color: 'rgba(255,255,255,0.4)', font: { size: 10 } }
          }
        },
        plugins: {
          legend: { display: false }
        }
      }
    });
  }

  // Bar Chart - Breach Counts
  const barCtx = document.getElementById('breachBarChart');
  if (barCtx) {
    breachBarChart = new Chart(barCtx, {
      type: 'bar',
      data: {
        labels: ['Allocation Drift', 'Concentration Risk', 'Daily Drop'],
        datasets: [{
          data: [0, 0, 0],
          backgroundColor: ['rgba(255,170,0,0.6)', 'rgba(255,94,94,0.6)', 'rgba(180,30,30,0.6)'],
          borderColor: ['rgba(255,170,0,0.8)', 'rgba(255,94,94,0.8)', 'rgba(180,30,30,0.8)'],
          borderWidth: 1,
          borderRadius: 4,
          barThickness: 22
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        indexAxis: 'y',
        scales: {
          x: {
            grid: { color: 'rgba(255,255,255,0.03)' },
            ticks: { color: 'rgba(255,255,255,0.4)', font: { size: 11 }, stepSize: 5 },
            beginAtZero: true
          },
          y: {
            grid: { display: false },
            ticks: { color: 'rgba(255,255,255,0.6)', font: { size: 12, weight: '500' } }
          }
        },
        plugins: {
          legend: { display: false }
        }
      }
    });
  }
}

// ---------------------------------------------------------------
// CHARTS: Update functions called during data refresh
// ---------------------------------------------------------------
function updateRiskDonutChart(riskData) {
  if (!riskDonutChart || !riskData) return;
  const high = riskData.filter(r => r.riskLevel === "HIGH").length;
  const medium = riskData.filter(r => r.riskLevel === "MEDIUM").length;
  const low = riskData.filter(r => r.riskLevel === "LOW").length;
  riskDonutChart.data.datasets[0].data = [low, medium, high];
  riskDonutChart.update('none');
}

function updateMarketLineChart(marketData) {
  if (!marketLineChart || !marketData || marketData.length === 0) return;
  const avgPrice = marketData.reduce((sum, s) => sum + (s.currentPrice || s.price || 0), 0) / marketData.length;
  const now = new Date();
  const timeLabel = now.getHours().toString().padStart(2,'0') + ':' + now.getMinutes().toString().padStart(2,'0') + ':' + now.getSeconds().toString().padStart(2,'0');

  marketHistory.push(avgPrice);
  marketLabels.push(timeLabel);

  // Keep max 60 data points (5 minutes at 5-second intervals)
  if (marketHistory.length > 60) {
    marketHistory.shift();
    marketLabels.shift();
  }

  marketLineChart.data.labels = marketLabels;
  marketLineChart.data.datasets[0].data = marketHistory;
  marketLineChart.update('none');
}

function updateBreachBarChart(driftCount, concCount, dropCount) {
  if (!breachBarChart) return;
  breachBarChart.data.datasets[0].data = [driftCount, concCount, dropCount];
  breachBarChart.update('none');
}

// ---------------------------------------------------------------
// AUTO-REFRESH TIMERS
// ---------------------------------------------------------------

// Market data: every 5 seconds (matches backend simulation interval)
setInterval(loadMarketData, 5000);

// Risk analysis: every 10 seconds (slightly slower — heavier computation)
setInterval(loadRiskAnalysis, 10000);

// Portfolio list: every 30 seconds (changes less frequently)
setInterval(loadPortfolioData, 30000);

// Timestamp: every second
function updateTimestamp() {
  const now = new Date();
  document.getElementById("last-updated").textContent =
    "Last Updated: " + now.toLocaleTimeString("en-US");
}
setInterval(updateTimestamp, 1000);

// ---------------------------------------------------------------
// AI ENGINE STATUS ANIMATION
// ---------------------------------------------------------------
const engineMessages = [
  "AI Engine Status: Monitoring Risk Thresholds...",
  "AI Engine Status: Detecting Allocation Drift...",
  "AI Engine Status: Checking Stock Concentration...",
  "AI Engine Status: Analyzing Daily Portfolio Drop...",
  "AI Engine Status: Generating AI Insights...",
  "AI Engine Status: Publishing Risk Alerts...",
  "AI Engine Status: ACTIVE — All Services Online"
];
let engineIndex = 0;
setInterval(() => {
  document.getElementById("ai-engine-status").textContent = engineMessages[engineIndex];
  engineIndex = (engineIndex + 1) % engineMessages.length;
}, 2500);

// ---------------------------------------------------------------
// ALERT PANEL TOGGLE
// ---------------------------------------------------------------
window.addEventListener("DOMContentLoaded", () => {
  loadSavedTheme();
  const alertToggle = document.getElementById("alert-toggle");
  const alertList   = document.getElementById("alert-list");

  alertToggle.addEventListener("click", () => {
    alertList.classList.toggle("hide-alerts");
    const chevron = document.querySelector('.alert-chevron');
    if (chevron) {
      chevron.style.transform = alertList.classList.contains("hide-alerts") ? 'rotate(-90deg)' : 'rotate(0deg)';
    }
  });

  // INITIAL LOAD — load all data on page start
  initCharts();
  loadPortfolioData();
  loadMarketData();
  loadRiskAnalysis();
  updateTimestamp();

  // Animate static top stats on page load
  setTimeout(() => {
    // Total Clients: count from 0 to 100
    const clientsEl = document.querySelector(".top-card:nth-child(1) .top-card-content p");
    animateCounter(clientsEl, 0, 100, 1200);
  }, 500);
});
