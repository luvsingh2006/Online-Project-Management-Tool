# Online Project Management Tool - Enterprise Server Launcher
Write-Host "=========================================================================" -ForegroundColor Cyan
Write-Host "  Online Project Management Tool - Enterprise Server Launcher" -ForegroundColor Cyan
Write-Host "=========================================================================" -ForegroundColor Cyan
Write-Host ""

$javaHome = "C:\Users\luvsi\.jdks\ms-21.0.12.1"
if (Test-Path "$javaHome\bin\javac.exe") {
    $env:PATH = "$javaHome\bin;" + $env:PATH
}

if (-not (Test-Path "target\classes")) {
    New-Item -ItemType Directory -Path "target\classes" -Force | Out-Null
}

Write-Host "[1/3] Compiling Java source files..." -ForegroundColor Yellow
$javaFiles = Get-ChildItem -Path "src/main/java" -Filter "*.java" -Recurse | ForEach-Object { '"' + ($_.FullName -replace '\', '/') + '"' }
$javaFiles | Set-Content -Path "sources.txt" -Encoding utf8

& javac -encoding UTF-8 -d target/classes @sources.txt
if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Compilation failed. Please review error output above." -ForegroundColor Red
    Remove-Item "sources.txt" -ErrorAction SilentlyContinue
    exit $LASTEXITCODE
}
Remove-Item "sources.txt" -ErrorAction SilentlyContinue
Write-Host "[OK] Compilation successful." -ForegroundColor Green

Write-Host "[2/3] Syncing schema and configuration resources..." -ForegroundColor Yellow
Copy-Item "src/main/resources/*" "target/classes/" -Force -ErrorAction SilentlyContinue

Write-Host "[3/3] Launching HTTP Application Server on http://localhost:8080..." -ForegroundColor Green
Start-Process "http://localhost:8080"

& java -cp target/classes com.pmtool.AppServer
