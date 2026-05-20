const companyLogos = {

"NVIDIA":"images/nvidia.png",

"Apple":"images/apple.png",

"Microsoft":"images/microsoft.jpeg",

"Reliance":"images/reliance.png",

"HDFC":"images/hdfc.png",

"Amazon":"images/amazon.png"

};

/* LOAD PORTFOLIO DATA */

async function loadPortfolioData(){

const response=await fetch("http://localhost:8080/portfolios");

const data=await response.json();

let output="";

data.forEach(portfolio=>{

output+=`

<div class="service-item portfolio-card"
onclick="selectClient(
'${portfolio.clientName || portfolio.name}',
'${portfolio.riskLevel}',
${portfolio.portfolioValue},
event
)">

<div>

<h3>

<div class="service-item-icon-box purple-icon">
<i class="fa-solid fa-user"></i>
</div>

${portfolio.clientName || portfolio.name}

</h3>

<p>Total Value: ₹${portfolio.portfolioValue}</p>

<p>
Risk Level:
<span class="${portfolio.riskLevel.toLowerCase()}">
${portfolio.riskLevel}
</span>
</p>

</div>

</div>

`;

});

document.getElementById("portfolio-data").innerHTML=output;

}

/* LOAD MARKET DATA */

async function loadMarketData(){

const response=await fetch("http://localhost:8081/market-data");

const marketData=await response.json();

let output="";

marketData.forEach(market=>{

output+=`

<div class="service-item market-card">

<div class="market-left">

<div class="service-item-icon-box market-logo-box">

<img
src="${companyLogos[market.stockName]}"
class="company-logo"
onerror="this.src='https://cdn-icons-png.flaticon.com/512/3135/3135715.png'"
>

</div>

<div>

<h3>${market.stockName || market.symbol}</h3>

<p>Price: ₹${market.price}</p>

</div>

</div>

<div class="market-right">

<p class="${
market.change > 0 ? 'positive' : 'negative'
}">
${market.change}%
</p>

</div>

</div>

`;

});

document.getElementById("market-data").innerHTML=output;

}

/* INITIAL LOAD */

loadPortfolioData();

loadMarketData();

/* AUTO REFRESH */

setInterval(()=>{

loadPortfolioData();

loadMarketData();

},5000);

/* DEFAULT AI DASHBOARD VALUES */

const highRiskCount=34;

const expectedAllocation=30;

const currentAllocation=38;

const driftValue=currentAllocation-expectedAllocation;

const stockExposure=28;

const dailyDropPercent=4.2;

/* AI SUMMARY */

if(highRiskCount>30){

document.getElementById("ai-summary").innerHTML=
"AI detected elevated portfolio volatility. Multiple portfolios show high-risk exposure. Immediate diversification is recommended.";

}
else if(highRiskCount>15){

document.getElementById("ai-summary").innerHTML=
"AI detected moderate market exposure. Portfolio diversification can improve stability.";

}
else{

document.getElementById("ai-summary").innerHTML=
"AI analysis indicates healthy portfolio diversification and stable market exposure.";

}

/* HEALTH STATUS */

if(highRiskCount>30){

document.getElementById("health-status").innerHTML=
"Portfolio Health: CRITICAL";

document.getElementById("health-status").style.color="red";

}
else if(highRiskCount>15){

document.getElementById("health-status").innerHTML=
"Portfolio Health: MODERATE";

document.getElementById("health-status").style.color="orange";

}
else{

document.getElementById("health-status").innerHTML=
"Portfolio Health: GOOD";

document.getElementById("health-status").style.color="lime";

}

/* ALLOCATION DRIFT */

if(driftValue>5){

document.getElementById("allocation-drift").innerHTML=
"Allocation Drift Alert: Tech allocation exceeded by "+
driftValue+
"%. Rebalancing recommended.";

document.getElementById("allocation-drift").style.color="red";

}
else{

document.getElementById("allocation-drift").innerHTML=
"Allocation Drift Status: Portfolio allocation is stable.";

document.getElementById("allocation-drift").style.color="lime";

}

/* STOCK EXPOSURE */

if(stockExposure>20){

document.getElementById("stock-exposure").innerHTML=
"Stock Exposure Alert: Single stock exposure exceeded 20%. Diversification is strongly recommended.";

document.getElementById("stock-exposure").style.color="red";

}
else{

document.getElementById("stock-exposure").innerHTML=
"Stock Exposure Status: Diversification levels are healthy.";

document.getElementById("stock-exposure").style.color="lime";

}

/* DAILY DROP */

if(dailyDropPercent>3){

document.getElementById("daily-drop").innerHTML=
"Daily Portfolio Drop Alert: Portfolio declined by "+
dailyDropPercent+
"%. High market volatility detected.";

document.getElementById("daily-drop").style.color="red";

}
else{

document.getElementById("daily-drop").innerHTML=
"Daily Portfolio Drop Status: Market movement is stable.";

document.getElementById("daily-drop").style.color="lime";

}

/* ALERTS */

let alerts="";

if(highRiskCount>30){

alerts+="<li>Critical Risk Alert: High-risk portfolios exceeded threshold.</li>";

}

if(driftValue>5){

alerts+="<li>Allocation Drift Alert: Portfolio rebalancing required.</li>";

}

if(stockExposure>20){

alerts+="<li>Stock Exposure Alert: Concentration risk detected.</li>";

}

if(dailyDropPercent>3){

alerts+="<li>Market Volatility Alert: Significant daily portfolio decline detected.</li>";

}

document.getElementById("alert-list").innerHTML=alerts;

/* TIMESTAMP */

function updateTimestamp(){

const now=new Date();

document.getElementById("last-updated").innerHTML=
"Last Updated: "+
now.toLocaleTimeString('en-US');

}

updateTimestamp();

setInterval(updateTimestamp,1000);

/* HEALTH SCORE */

let healthScore=100;

healthScore-=highRiskCount;

healthScore-=driftValue;

healthScore-=stockExposure/2;

healthScore-=dailyDropPercent*2;

if(healthScore<0){

healthScore=0;

}

document.getElementById("health-score").innerHTML=
"AI Health Score: "+
Math.round(healthScore)+
"/100";

/* RECOMMENDATIONS */

let recommendation="";

if(highRiskCount>30){

recommendation=
"AI Recommendation: Reduce exposure to high-volatility assets and improve diversification.<br><br>";

}

if(driftValue>5){

recommendation+=
"AI Recommendation: Portfolio allocation drift exceeded safe threshold. Rebalancing is advised.<br><br>";

}

if(stockExposure>20){

recommendation+=
"AI Recommendation: Reduce concentration in single-stock holdings to minimize portfolio risk.<br><br>";

}

if(dailyDropPercent>3){

recommendation+=
"AI Recommendation: Market volatility is elevated. Consider defensive investment strategies.";

}

document.getElementById("ai-recommendation").innerHTML=
recommendation;

/* ENGINE STATUS */

const engineMessages=[

"AI Engine Status: Monitoring Risk...",

"AI Engine Status: Detecting Market Volatility...",

"AI Engine Status: Running Portfolio Analysis...",

"AI Engine Status: Evaluating Allocation Drift...",

"AI Engine Status: ACTIVE"

];

let engineIndex=0;

setInterval(()=>{

document.getElementById("ai-engine-status").innerHTML=
engineMessages[engineIndex];

engineIndex++;

if(engineIndex>=engineMessages.length){

engineIndex=0;

}

},3000);

/* RISK METER */

document.querySelector(".risk-fill").style.width=
healthScore+"%";

if(healthScore>70){

document.querySelector(".risk-fill").style.background=
"lime";

}
else if(healthScore>40){

document.querySelector(".risk-fill").style.background=
"orange";

}
else{

document.querySelector(".risk-fill").style.background=
"red";

}

document.getElementById("risk-percentage").innerHTML=
"Risk Score: "+
healthScore+
"%";

/* ALERT TOGGLE */

window.addEventListener("DOMContentLoaded",()=>{

const alertToggle=document.getElementById("alert-toggle");

const alertList=document.getElementById("alert-list");

alertToggle.addEventListener("click",()=>{

alertList.classList.toggle("hide-alerts");

if(alertList.classList.contains("hide-alerts")){

alertToggle.innerHTML="Risk Alerts ▶";

}
else{

alertToggle.innerHTML="Risk Alerts ▼";

}

});

});

/* AI CLIENT INSIGHT */

function showPortfolioInsight(clientName,riskLevel,portfolioValue,event){

document.querySelectorAll(".portfolio-card").forEach(card=>{

card.style.border="1px solid rgba(255,255,255,0.1)";

});

event.currentTarget.style.border="2px solid cyan";

let riskScore=0;

let driftMessage="";

let exposureMessage="";

let dropMessage="";

let alertItems="";

let aiMessage="";

let recommendationMessage="";

let healthStatus="";

/* HIGH RISK */

if(riskLevel==="HIGH"){

riskScore=82;

healthStatus="Portfolio Health: CRITICAL";

document.getElementById("health-status").style.color="red";

aiMessage=
"AI Insight for "+
clientName+
": High portfolio risk detected. Elevated market exposure and concentration risk identified.";

driftMessage=
"Allocation Drift Alert: Portfolio allocation exceeded target model by 9%. Rebalancing strongly recommended.";

document.getElementById("allocation-drift").style.color="red";

exposureMessage=
"Stock Exposure Alert: Technology sector exposure exceeded 25%. Diversification required.";

document.getElementById("stock-exposure").style.color="red";

dropMessage=
"Daily Portfolio Drop Alert: Portfolio declined by 4.8%. High volatility detected.";

document.getElementById("daily-drop").style.color="red";

alertItems=
"<li>Critical Risk Alert: High-risk exposure detected.</li>"+
"<li>Allocation Drift Alert: Portfolio rebalancing required.</li>"+
"<li>Stock Exposure Alert: Concentration risk identified.</li>";

recommendationMessage=
"AI Recommendation: Reduce high-volatility exposure and rebalance portfolio across defensive sectors.";

}

/* MEDIUM RISK */

else if(riskLevel==="MEDIUM"){

riskScore=58;

healthStatus="Portfolio Health: MODERATE";

document.getElementById("health-status").style.color="orange";

aiMessage=
"AI Insight for "+
clientName+
": Portfolio is moderately balanced. Partial allocation adjustments recommended.";

driftMessage=
"Allocation Drift Warning: Allocation drift reached 6%. Minor rebalancing advised.";

document.getElementById("allocation-drift").style.color="orange";

exposureMessage=
"Stock Exposure Warning: Sector exposure slightly exceeds preferred range.";

document.getElementById("stock-exposure").style.color="orange";

dropMessage=
"Daily Portfolio Drop Status: Portfolio declined by 2.1%. Monitoring recommended.";

document.getElementById("daily-drop").style.color="orange";

alertItems=
"<li>Moderate Risk Alert: Portfolio requires monitoring.</li>"+
"<li>Allocation Warning: Minor drift detected.</li>";

recommendationMessage=
"AI Recommendation: Improve diversification across medium-volatility assets.";

}

/* LOW RISK */

else{

riskScore=28;

healthStatus="Portfolio Health: GOOD";

document.getElementById("health-status").style.color="lime";

aiMessage=
"AI Insight for "+
clientName+
": Portfolio risk remains stable with healthy diversification.";

driftMessage=
"Allocation Drift Status: Portfolio allocation remains stable.";

document.getElementById("allocation-drift").style.color="lime";

exposureMessage=
"Stock Exposure Status: Diversification levels are healthy.";

document.getElementById("stock-exposure").style.color="lime";

dropMessage=
"Daily Portfolio Drop Status: Market movement remains stable.";

document.getElementById("daily-drop").style.color="lime";

alertItems=
"<li>Low Risk Status: Portfolio is stable.</li>";

recommendationMessage=
"AI Recommendation: Continue current investment strategy with periodic monitoring.";

}

/* LARGE PORTFOLIO */

if(portfolioValue>400000){

aiMessage+=
" Large portfolio concentration detected. AI recommends periodic allocation review.";

}

/* UPDATE AI PANEL */

document.getElementById("ai-summary").innerHTML=aiMessage;

document.getElementById("allocation-drift").innerHTML=driftMessage;

document.getElementById("stock-exposure").innerHTML=exposureMessage;

document.getElementById("daily-drop").innerHTML=dropMessage;

document.getElementById("alert-list").innerHTML=alertItems;

document.getElementById("health-status").innerHTML=healthStatus;

document.getElementById("ai-recommendation").innerHTML=
recommendationMessage;

document.getElementById("health-score").innerHTML=
"AI Health Score: "+
(100-riskScore)+
"/100";

document.getElementById("risk-percentage").innerHTML=
"Risk Score: "+
riskScore+
"%";

document.querySelector(".risk-fill").style.width=
riskScore+
"%";

/* RISK METER COLORS */

if(riskScore>70){

document.querySelector(".risk-fill").style.background="red";

}
else if(riskScore>40){

document.querySelector(".risk-fill").style.background="orange";

}
else{

document.querySelector(".risk-fill").style.background="lime";

}

}

/* SELECT CLIENT */

function selectClient(clientName,riskLevel,portfolioValue,event){

document.querySelectorAll(".portfolio-card").forEach(card=>{

card.style.border="1px solid rgba(255,255,255,0.1)";

});

event.currentTarget.style.border="2px solid cyan";

let suggestion="";

let icon="";

let riskColor="";

/* HIGH */

if(riskLevel==="HIGH"){

suggestion=
"AI detected high portfolio volatility and concentration risk. Diversification strongly recommended.";

icon="fa-solid fa-triangle-exclamation";

riskColor="red";

}

/* MEDIUM */

else if(riskLevel==="MEDIUM"){

suggestion=
"Portfolio is moderately balanced. Partial reallocation into stable assets recommended.";

icon="fa-solid fa-chart-line";

riskColor="orange";

}

/* LOW */

else{

suggestion=
"Portfolio risk is low. Current investment allocation appears healthy and stable.";

icon="fa-solid fa-shield-halved";

riskColor="lime";

}

/* RISK ANALYSIS CARD */

document.getElementById("risk-data").innerHTML=`

<div class="service-item risk-card">

<div class="risk-top">

<div class="risk-left">

<div class="risk-icon-box">

<i class="${icon}"></i>

</div>

<div class="risk-info">

<h3>${clientName}</h3>

<p>Total Value: ₹${portfolioValue}</p>

</div>

</div>

<div class="risk-level-box">

<p>Risk Level</p>

<span style="color:${riskColor}">
${riskLevel}
</span>

</div>

</div>

<div class="risk-suggestion">

<p>${suggestion}</p>

</div>

</div>

`;

/* UPDATE AI SUMMARY */

showPortfolioInsight(
clientName,
riskLevel,
portfolioValue,
event
);

}