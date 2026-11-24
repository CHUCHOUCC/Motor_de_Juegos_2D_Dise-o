# Compila y arranca PixelForge en Windows (PowerShell). Necesita JDK 21 e internet la primera vez.
#   .\run.ps1                 usa MongoDB en mongodb://localhost:27017
#   .\run.ps1 -Storage file   guarda en data\pixelforge.dat (sin MongoDB)
param([string]$Storage = "mongo", [int]$Port = 8080)
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
. .\lib.ps1
Get-MongoDriver
if (Test-Path out) { Remove-Item -Recurse -Force out }
$sources = Get-ChildItem -Recurse src\main\java -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -cp "lib/*" -d out $sources
if ($LASTEXITCODE -ne 0) { throw "Error de compilación" }
Write-Host "Abre http://localhost:$Port en el navegador (Ctrl+C para detener)"
java "-Dstdout.encoding=UTF-8" -cp "out;lib/*" com.pixelforge.Main $Port $Storage
