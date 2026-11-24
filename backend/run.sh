#!/bin/sh
# Compila y arranca PixelForge en Linux/macOS. Uso: ./run.sh [mongo|file]
cd "$(dirname "$0")" || exit 1
V=5.2.1; mkdir -p lib
for a in mongodb-driver-sync mongodb-driver-core bson bson-record-codec; do
  [ -f "lib/$a-$V.jar" ] || curl -fsSL -o "lib/$a-$V.jar" "https://repo1.maven.org/maven2/org/mongodb/$a/$V/$a-$V.jar"
done
rm -rf out && javac -encoding UTF-8 -cp "lib/*" -d out $(find src/main/java -name "*.java") && java -cp "out:lib/*" com.pixelforge.Main 8080 "${1:-mongo}"
