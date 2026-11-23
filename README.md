# Conserta Tudo — Sistema de Gestão de Serviços de Manutenção Residencial

**Trabalho acadêmico — Programação Orientada a Objetos (Java)**

## Integrantes do grupo

1. **Isabelly Da Silva Monteiro**
2. **Maria Beatriz Barbosa Lopes**

## Sobre o projeto

O Conserta Tudo é um sistema de console para gerenciamento de serviços de manutenção residencial, incluindo cadastro de clientes e técnicos, ordens de serviço, atendimento de emergências, pagamentos, programa de fidelidade e relatórios.

Os dados do sistema são armazenados localmente em arquivos CSV.

## Requisitos para executar no computador

Para abrir e executar o projeto, o computador precisa ter:

- **Sistema operacional:** Windows 10 ou Windows 11 (o projeto também contém um script para Linux/macOS).
- **Java JDK 17 ou superior**, instalado e configurado no PATH.
  - É necessário o **JDK**, não apenas o JRE, pois o projeto precisa do compilador `javac`.
- **Memória e espaço em disco:** espaço livre suficiente para extrair a pasta do projeto e executar o Java.
- **Terminal:** Prompt de Comando (CMD) ou PowerShell no Windows.

Não é necessário instalar IDE (Eclipse, IntelliJ ou NetBeans), Maven, Gradle ou bibliotecas externas.

### Como verificar se o Java está instalado

Abra o CMD ou PowerShell e execute, separadamente:

```cmd
java -version
javac -version
```

Os dois comandos devem apresentar a versão 17 ou superior. Se `java` ou `javac` não for reconhecido, será necessário instalar o **JDK 17 ou superior** e configurar a variável `PATH` do Windows.

## Como abrir e executar no Windows

1. Baixe o arquivo ZIP do projeto.
2. Extraia todo o conteúdo do ZIP para uma pasta do computador.
3. Abra a pasta extraída `conserta-tudo`.
4. Dê dois cliques em `executar.bat`.

   **Alternativa pelo terminal:**
   - Abra o CMD.
   - Entre na pasta `conserta-tudo` usando o comando `cd`. Exemplo:

     ```cmd
     cd C:\caminho\da\pasta\conserta-tudo
     ```

   - Execute:

     ```cmd
     executar.bat
     ```

O arquivo `executar.bat` compila os arquivos Java automaticamente e inicia o sistema. Na primeira execução, o Windows poderá levar alguns instantes para compilar o projeto.

### Como executar os testes automatizados

Na mesma pasta do projeto, abra o CMD e execute:

```cmd
executar.bat testes
```

## Como executar no Linux ou macOS

Com o JDK 17 ou superior instalado, abra o terminal na pasta do projeto e execute:

```bash
chmod +x executar.sh
./executar.sh
```

Para executar os testes:

```bash
./executar.sh testes
```

## Funcionalidades principais

- Cadastro, consulta, edição e remoção de clientes e técnicos.
- Catálogo de serviços de elétrica, hidráulica e reparos gerais.
- Criação e gerenciamento de ordens de serviço.
- Alocação de técnicos conforme disponibilidade e tipo de serviço.
- Atendimento prioritário de ordens emergenciais.
- Taxa adicional para serviços emergenciais.
- Programa de fidelidade com descontos e pontos.
- Registro de pagamentos.
- Relatórios e histórico de pagamentos.
- Persistência dos dados em arquivos CSV.

## Armazenamento dos dados

O sistema cria automaticamente a pasta `dados/` para armazenar os arquivos CSV. Essa pasta é utilizada para manter os cadastros, ordens e pagamentos entre as execuções.

**Importante:** para preservar os dados, não exclua a pasta `dados/` depois de utilizar o sistema. Para transferir o projeto para outro computador levando os registros já cadastrados, copie também essa pasta.

## Estrutura do projeto

```text
conserta-tudo/
├── src/
│   ├── main/java/       Código-fonte da aplicação
│   └── test/java/       Testes automatizados
├── executar.bat         Inicialização no Windows
├── executar.sh          Inicialização no Linux/macOS
└── README.md            Instruções e documentação
```

## Observações para avaliação

- O projeto é uma aplicação Java executada pelo terminal, com interface por menus de texto.
- O código-fonte está incluído na pasta `src/`.
- O script `executar.bat` realiza a compilação e inicia o programa.
- O requisito indispensável para executar é ter o **JDK 17 ou superior** instalado e acessível pelo terminal.
