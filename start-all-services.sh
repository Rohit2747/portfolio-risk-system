#!/bin/bash
# ================================================================
# Start All Portfolio Risk System Services
# Run this from the project root directory
# ================================================================

echo ""
echo "╔══════════════════════════════════════════════════════════╗"
echo "║     AI Portfolio Risk Monitoring Platform — Startup      ║"
echo "╠══════════════════════════════════════════════════════════╣"
echo "║  Portfolio Service    → http://localhost:8080            ║"
echo "║  Market Data Service  → http://localhost:8081            ║"
echo "║  Risk Analysis Service → http://localhost:8082           ║"
echo "║  AI Insight Service   → http://localhost:8083            ║"
echo "╚══════════════════════════════════════════════════════════╝"
echo ""
echo "Starting all services... (each in background)"
echo ""

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$PROJECT_DIR/backend"

# Function to build and start a service
start_service() {
    local SERVICE_NAME=$1
    local SERVICE_DIR="$BACKEND_DIR/$SERVICE_NAME"
    local LOG_FILE="$PROJECT_DIR/logs/${SERVICE_NAME}.log"

    mkdir -p "$PROJECT_DIR/logs"

    echo "► Starting $SERVICE_NAME..."
    cd "$SERVICE_DIR"
    mvn spring-boot:run -q > "$LOG_FILE" 2>&1 &
    echo "  PID: $! | Log: logs/${SERVICE_NAME}.log"
}

# Start services in order (Portfolio and Market Data first,
# then Risk Analysis, then AI Insight)
start_service "portfolio-service"
sleep 5

start_service "market-data-service"
sleep 5

start_service "risk-analysis-service"
sleep 5

start_service "ai-insight-service"

echo ""
echo "All services starting..."
echo ""
echo "Wait ~30 seconds, then open: frontend/index.html in your browser"
echo ""
echo "To verify services are running:"
echo "  curl http://localhost:8080/hello"
echo "  curl http://localhost:8081/hello"
echo "  curl http://localhost:8082/hello"
echo "  curl http://localhost:8083/hello"
echo ""
echo "To stop all services:"
echo "  pkill -f 'spring-boot:run'"
echo ""
