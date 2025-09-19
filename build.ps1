# Build script using IntelliJ's Java
$env:JAVA_HOME = "C:\Users\XTEN\.jdks\ms-21.0.8"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

Write-Host "🔨 Building L-Thread JAR..." -ForegroundColor Cyan
Write-Host "Using Java: $env:JAVA_HOME" -ForegroundColor Gray

.\gradlew bootJar

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Build successful!" -ForegroundColor Green
    Write-Host "JAR location: build\libs\" -ForegroundColor Gray
} else {
    Write-Host "❌ Build failed!" -ForegroundColor Red
    exit 1
}