# Compila y ejecuta las 14 pruebas JUnit (no necesitan MongoDB encendido).
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
.\mvnw.cmd test
