const portfolioData = [
{
name: "Tech Portfolio",
value: 250000,
risk: "MEDIUM"
},
{
name: "Banking Portfolio",
value: 500000,
risk: "HIGH"
}
];

const marketData = [
{
stockName: "Apple",
price: 210,
change: 2.5
},
{
stockName: "Tesla",
price: 185,
change: -1.8
}
];

const riskData = [
{
portfolioName: "Tech Portfolio",
totalValue: 250000,
riskLevel: "MEDIUM",
suggestion: "Portfolio moderately balanced."
},
{
portfolioName: "Banking Portfolio",
totalValue: 500000,
riskLevel: "HIGH",
suggestion: "High portfolio risk detected."
}
];

function loadPortfolioData(){

let output="";

portfolioData.forEach(portfolio=>{

output+=`
<div class="service-item">
<h3>${portfolio.name}</h3>
<p>Total Value: ₹${portfolio.value}</p>
<p>Risk Level:
<span class="${portfolio.risk.toLowerCase()}">
${portfolio.risk}
</span>
</p>
</div>
`;

});

document.getElementById("portfolio-data").innerHTML=output;

}

function loadMarketData(){

let output="";

marketData.forEach(market=>{

output+=`
<div class="service-item market-card">

<div>
<h3>${market.stockName}</h3>
<p>Price: ₹${market.price}</p>
</div>

<div>
<p class="${market.change > 0 ? 'positive' : 'negative'}">
${market.change}%
</p>
</div>

</div>
`;

});

document.getElementById("market-data").innerHTML=output;

}

function loadRiskData(){

let output="";

riskData.forEach(risk=>{

output+=`
<div class="service-item">

<h3>${risk.portfolioName}</h3>

<p>Total Value: ₹${risk.totalValue}</p>

<p>
Risk Level:
<span class="${risk.riskLevel.toLowerCase()}">
${risk.riskLevel}
</span>
</p>

<p>${risk.suggestion}</p>

</div>
`;

});

document.getElementById("risk-data").innerHTML=output;

}

loadPortfolioData();
loadMarketData();
loadRiskData();

document.getElementById("ai-summary").innerHTML=
"AI detected elevated portfolio volatility. Multiple portfolios show high-risk exposure.";

document.getElementById("health-status").innerHTML=
"Portfolio Health: MODERATE";

document.getElementById("health-score").innerHTML=
"AI Health Score: 72/100";

document.getElementById("risk-percentage").innerHTML=
"Risk Score: 72%";