@echo off
rem Compila o projeto e executa. Uso: executar.bat [testes]
chcp 65001 >nul
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > fontes.txt
javac --release 17 -encoding UTF-8 -d out @fontes.txt
if errorlevel 1 (
    del fontes.txt
    exit /b 1
)
del fontes.txt
if "%1"=="testes" (
    java -cp out consertatudo.TestesSistema
) else (
    java -cp out consertatudo.Main
)
