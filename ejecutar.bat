@echo off
rem Ejecuta la Biblioteca Escolar. Debe estar junto a db.properties (copiado desde db.properties.example).
rem Primero genere el ejecutable con Maven:  mvn clean package   (crea target\biblioteca-escolar.jar)
if not exist "target\biblioteca-escolar.jar" (
    echo No existe target\biblioteca-escolar.jar. Ejecute primero:  mvn clean package
    pause
    exit /b 1
)
java -jar target\biblioteca-escolar.jar
