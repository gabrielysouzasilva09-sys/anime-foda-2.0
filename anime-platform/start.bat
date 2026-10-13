@echo off
REM ═══════════════════════════════════════════════════════
REM  AniReview — Script de arranque do servidor Spring Boot
REM  Acesso: http://localhost:8080
REM ═══════════════════════════════════════════════════════

set JAVA_HOME=C:\Users\gabriely.silva\Downloads\OpenJDK21U-jdk_x64_windows_hotspot_21.0.12.1_1\jdk-21.0.12.1+1
set PATH=%JAVA_HOME%\bin;%PATH%
set MVN=C:\Users\gabriely.silva\.m2\wrapper\dists\apache-maven-3.9.6-bin\3311e1d4\apache-maven-3.9.6\bin\mvn.cmd

echo.
echo  =====================================================
echo   AniReview — Iniciando servidor em localhost:8080
echo  =====================================================
echo.

java -version
echo.

cd /d "%~dp0"
call "%MVN%" spring-boot:run

pause
