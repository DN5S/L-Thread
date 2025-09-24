# L-Thread Smart Rebuild Script

Write-Host "🔧 L-Thread Smart Rebuild" -ForegroundColor Green
Write-Host "============================" -ForegroundColor Green

Write-Host "`n📦 Stopping and removing containers..." -ForegroundColor Yellow
podman compose down

Write-Host "`n🔨 Building services (using cache)..." -ForegroundColor Cyan
podman compose build

Write-Host "`n🚀 Starting services..." -ForegroundColor Green
podman compose up -d

Write-Host "`n✅ Container Status:" -ForegroundColor Green
podman compose ps