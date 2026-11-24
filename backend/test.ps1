# Compila el código y las pruebas, y las ejecuta (no necesita MongoDB encendido).
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
. .\lib.ps1
Get-MongoDriver
if (Test-Path out) { Remove-Item -Recurse -Force out }
$sources = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -cp "lib/*" -d out $sources
if ($LASTEXITCODE -ne 0) { throw "Error de compilación" }
java "-Dstdout.encoding=UTF-8" -cp "out;lib/*" com.pixelforge.EngineTests
