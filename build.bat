@echo off
echo ===========================================
echo   Compiling RegLog Java Backend
echo ===========================================

if not exist bin mkdir bin

echo Compiling Java source files...
javac -cp "lib/*" -d bin src/com/reglog/*.java src/com/reglog/config/*.java src/com/reglog/model/*.java src/com/reglog/security/*.java src/com/reglog/service/*.java src/com/reglog/controller/*.java

if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Compilation finished successfully. Class files are in 'bin/'.
) else (
    echo [ERROR] Compilation failed.
)
