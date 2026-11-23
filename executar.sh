#!/usr/bin/env bash
# Compila o projeto e executa. Uso: ./executar.sh [testes]
set -e
cd "$(dirname "$0")"
rm -rf out && mkdir out
javac --release 17 -encoding UTF-8 -d out $(find src -name '*.java')
if [ "$1" = "testes" ]; then
    java -cp out consertatudo.TestesSistema
else
    java -cp out consertatudo.Main
fi
