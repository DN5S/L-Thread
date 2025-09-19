# L-Thread Podman Compose Rebuild Script
# Completely rebuilds the application containers

Write-Host "🔧 L-Thread Container Rebuild" -ForegroundColor Green
Write-Host "================================" -ForegroundColor Green

# Stop and remove containers
Write-Host "`n📦 Stopping containers..." -ForegroundColor Yellow
podman compose down

# Remove the old images to force rebuild
Write-Host "`n🗑️ Removing old images..." -ForegroundColor Yellow
podman rmi lthread:latest -f 2>$null
podman rmi redis:7-alpine -f 2>$null

# Clean up any dangling images
Write-Host "`n🧹 Cleaning dangling images..." -ForegroundColor Yellow
podman image prune -f

# Build fresh
Write-Host "`n🔨 Building fresh containers..." -ForegroundColor Cyan
podman compose build --no-cache

# Start the services
Write-Host "`n🚀 Starting services..." -ForegroundColor Green
podman compose up -d

# Show status
Write-Host "`n✅ Container Status:" -ForegroundColor Green
podman compose ps

Write-Host "`n🌐 Application available at: http://localhost:8080" -ForegroundColor Cyan
Write-Host "📊 Redis available at: localhost:6379" -ForegroundColor Cyan