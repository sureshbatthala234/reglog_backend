@echo off
echo ===========================================
echo   Starting RegLog Backend Server
echo ===========================================

if not exist bin (
    echo Bin folder not found. Compiling first...
    call build.bat
)

java -cp "bin;lib/*" com.reglog.ReglogServer
