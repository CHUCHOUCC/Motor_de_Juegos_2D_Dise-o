# Compila y arranca PixelForge (Spring Boot) en Windows. Necesita JDK 21; Maven lo descarga el wrapper.
#   .\run.ps1                 usa MongoDB en mongodb://localhost:27017
#   .\run.ps1 -Storage file   guarda en data\pixelforge.dat (sin MongoDB)
param([string]$Storage = "mongo", [int]$Port = 8080)
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
Write-Host "Cuando diga 'Started PixelForgeApplication', abre http://localhost:$Port (Ctrl+C para detener)"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--pixelforge.storage=$Storage --server.port=$Port"
