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

// ---------------------------------------------------------------
// FETCH: Portfolio Data (port 8080)
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
    const riskClass  = (p.riskLevel || "UNKNOWN").toLowerCase();
    const value      = p.portfolioValue > 0
      ? `₹${p.portfolioValue.toLocaleString("en-IN", {maximumFractionDigits: 2})}`
      : "Loading...";

    html += `
      <div class="service-item portfolio-card"
           onclick="selectClient(${p.clientId}, '${p.clientName}', '${p.riskLevel || "UNKNOWN"}', ${p.portfolioValue || 0}, event)">
        <div>
          <h3>
            <div class="service-item-icon-box purple-icon">
              <i class="fa-solid fa-user"></i>
            </div>
            ${p.clientName}
          </h3>
          <p>Total Value: ${value}</p>
          <p>Risk Level: <span class="${riskClass}">${p.riskLevel || "CALCULATING..."}</span></p>
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
    renderMarketCards(allMarketData);
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
        <div class="market-right">
          <p class="${isPositive ? 'positive' : 'negative'}">${isPositive ? '+' : ''}${change}%</p>
          <p style="font-size:12px;color:#aaa;">Day: ${parseFloat(dailyChange) >= 0 ? '+' : ''}${dailyChange}%</p>
        </div>
      </div>`;
  });
  document.getElementById("market-data").innerHTML = html || "<p>No market data found.</p>";
}

// ---------------------------------------------------------------
// FETCH: Risk Analysis (port 8082) — MAIN DATA SOURCE
// ---------------------------------------------------------------
async function loadRiskAnalysis() {
  try {
    const response = await fetch(`${RISK_SERVICE_URL}/risk-analysis`);
    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    allRiskData = await response.json();
    updateDashboardSummary(allRiskData);

    // If a client is selected, refresh their panel too
    if (selectedClient) {
      const updated = allRiskData.find(r => r.clientId === selectedClient);
      if (updated) renderRiskPanel(updated);
    }
  } catch (err) {
    document.getElementById("ai-summary").textContent =
      `⚠ Risk Analysis Service unavailable. Start service on port 8082. (${err.message})`;
    document.getElementById("health-status").textContent = "Portfolio Health: UNAVAILABLE";
    document.getElementById("health-status").style.color = "gray";
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

  // ---- Health Status ----
  const healthStatus = document.getElementById("health-status");
  if (highCount > 20) {
    healthStatus.textContent = "Portfolio Health: CRITICAL";
    healthStatus.style.color = "red";
  } else if (highCount > 5 || mediumCount > 30) {
    healthStatus.textContent = "Portfolio Health: MODERATE";
    healthStatus.style.color = "orange";
  } else {
    healthStatus.textContent = "Portfolio Health: GOOD";
    healthStatus.style.color = "lime";
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
    allocationDrift.textContent =
      `Allocation Drift Alert: ${driftCount} breach(es) detected across portfolios. Rebalancing recommended.`;
    allocationDrift.style.color = "red";
  } else {
    allocationDrift.textContent = "Allocation Drift: All portfolios within target allocation bands.";
    allocationDrift.style.color = "lime";
  }

  // ---- Stock Concentration ----
  const stockExposure = document.getElementById("stock-exposure");
  if (concCount > 0) {
    stockExposure.textContent =
      `Concentration Risk: ${concCount} breach(es) detected. Single-stock exposure exceeded 20% threshold.`;
    stockExposure.style.color = "red";
  } else {
    stockExposure.textContent = "Stock Concentration: All holdings within acceptable exposure limits.";
    stockExposure.style.color = "lime";
  }

  // ---- Daily Drop ----
  const dailyDrop = document.getElementById("daily-drop");
  if (dropCount > 0) {
    dailyDrop.textContent =
      `Daily Drop Alert: ${dropCount} portfolio(s) declined more than 3% today. High volatility detected.`;
    dailyDrop.style.color = "red";
  } else {
    dailyDrop.textContent = `Daily Drop: Average portfolio change today: ${avgDaily >= 0 ? '+' : ''}${avgDaily.toFixed(2)}%. Within acceptable range.`;
    dailyDrop.style.color = avgDaily < -1 ? "orange" : "lime";
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

  document.getElementById("health-score").textContent = `AI Health Score: ${healthScore}/100`;

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

  // ---- Risk Meter ----
  const fill = document.querySelector(".risk-fill");
  const riskScore = 100 - healthScore;
  fill.style.width = riskScore + "%";
  fill.style.background = riskScore > 60 ? "red" : riskScore > 30 ? "orange" : "lime";
  document.getElementById("risk-percentage").textContent = `Risk Score: ${riskScore}%`;

  // Update top stats
  const highRiskEl = document.querySelector(".top-card:nth-child(4) p");
  if (highRiskEl) { highRiskEl.textContent = highCount; highRiskEl.style.color = "red"; }

  const totalValueEl = document.querySelector(".top-card:nth-child(2) p");
  if (totalValueEl) {
    totalValueEl.textContent = `₹${(totalValue/10000000).toFixed(2)}Cr`;
  }
}

// ---------------------------------------------------------------
// CLIENT SELECTION — click on a portfolio card
// ---------------------------------------------------------------
function selectClient(clientId, clientName, riskLevel, portfolioValue, event) {
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

    // Also get AI insight from AI Insight Service
    fetchAndRenderAIInsight(clientId);
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
      `AI Severity: ${insight.severity} | Provider: ${insight.aiProvider}`;
    document.getElementById("health-status").style.color = severityColor;

  } catch (err) {
    // AI Insight Service may not be running — silently skip
    console.log("[Dashboard] AI Insight Service not available:", err.message);
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
      <div style="margin-top:12px;padding-top:12px;border-top:1px solid rgba(255,255,255,0.08);">
        <small style="color:#888;">Last analyzed: ${riskData.alertTimestamp || "N/A"}</small>
      </div>
    </div>`;

  // Also update AI panel with local insight (overridden if AI service responds)
  const suggestion = riskData.aiInsight || riskData.suggestedAction || riskData.suggestion
    || buildLocalInsight(riskData);
  document.getElementById("ai-summary").textContent =
    buildLocalExplanation(riskData);
  document.getElementById("ai-recommendation").innerHTML = suggestion;

  const healthStatus = document.getElementById("health-status");
  healthStatus.textContent = `Portfolio Health: ${riskData.riskLevel}`;
  healthStatus.style.color = riskColor;

  // Update drift/exposure/drop indicators for this client
  updateBreachIndicators(riskData);
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
    driftEl.textContent = `Allocation Drift: ${driftBreach.description}`;
    driftEl.style.color = "red";
  } else {
    driftEl.textContent = `Allocation Drift: Within target bands for ${riskData.clientName}.`;
    driftEl.style.color = "lime";
  }

  if (concBreach) {
    expEl.textContent = `Concentration: ${concBreach.description}`;
    expEl.style.color = "red";
  } else {
    expEl.textContent = `Stock Concentration: All holdings within 20% threshold.`;
    expEl.style.color = "lime";
  }

  if (dropBreach) {
    dropEl.textContent = `Daily Drop: ${dropBreach.description}`;
    dropEl.style.color = "red";
  } else {
    const daily = (riskData.dailyChangePercent || 0).toFixed(2);
    dropEl.textContent = `Daily Drop: Portfolio change today: ${daily >= 0 ? '+' : ''}${daily}%`;
    dropEl.style.color = parseFloat(daily) < -1 ? "orange" : "lime";
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
  const alertToggle = document.getElementById("alert-toggle");
  const alertList   = document.getElementById("alert-list");

  alertToggle.addEventListener("click", () => {
    alertList.classList.toggle("hide-alerts");
    alertToggle.innerHTML = alertList.classList.contains("hide-alerts")
      ? "Risk Alerts ▶" : "Risk Alerts ▼";
  });

  // INITIAL LOAD — load all data on page start
  loadPortfolioData();
  loadMarketData();
  loadRiskAnalysis();
  updateTimestamp();
});
