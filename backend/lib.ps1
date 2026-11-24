# Descarga el driver oficial de MongoDB para Java (4 jars) en la carpeta lib\, solo la primera vez.
$MongoVersion = "5.2.1"
function Get-MongoDriver {
  New-Item -ItemType Directory -Force lib | Out-Null
  foreach ($artifact in @("mongodb-driver-sync", "mongodb-driver-core", "bson", "bson-record-codec")) {
    $jar = "lib\$artifact-$MongoVersion.jar"
    if (-not (Test-Path $jar)) {
      Write-Host "Descargando $artifact $MongoVersion..."
      $url = "https://repo1.maven.org/maven2/org/mongodb/$artifact/$MongoVersion/$artifact-$MongoVersion.jar"
      Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $jar
    }
  }
}
