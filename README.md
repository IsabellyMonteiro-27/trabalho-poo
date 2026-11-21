# Conserta Tudo — Sistema de Gerenciamento de Serviços

## 1. Apresentação do projeto

O Conserta Tudo é um sistema desenvolvido em Java 17 como projeto acadêmico da disciplina de Programação Orientada a Objetos.

O sistema tem como objetivo auxiliar no gerenciamento de uma empresa de manutenção e prestação de serviços. Por meio dele, é possível organizar os cadastros, acompanhar as ordens de serviço, registrar pagamentos e consultar informações dos atendimentos realizados.

O desenvolvimento do projeto busca colocar em prática conceitos de programação, organização de classes, métodos, objetos e armazenamento de dados.

## 2. Integrantes

- Isabelly Da Silva Monteiro
- Maria Beatriz Barbosa Lopes

## 3. Tecnologias utilizadas

- **Java 17:** linguagem utilizada no desenvolvimento do sistema.
- **Programação Orientada a Objetos:** organização do código por meio de classes e objetos.
- **Arquivos CSV:** utilizados para armazenar os dados do sistema.
- **JDK 17:** necessário para compilar e executar o projeto.

## 4. Requisitos para instalação

Para abrir e executar o projeto em outro computador, é necessário:

- Computador com Windows 10 ou Windows 11.
- Java JDK 17 ou superior instalado.
- Acesso ao Prompt de Comando (CMD) ou PowerShell.
- Arquivos do projeto extraídos em uma pasta.

O JDK é necessário porque contém o compilador Java (`javac`), utilizado para compilar os arquivos do programa.

**Download do JDK 17:**

https://adoptium.net/temurin/releases/

Na página, selecionar:
- Version: 17
- Operating System: Windows
- Architecture: x64
- Package Type: JDK

## 5. Verificação do Java

Depois da instalação, abra o CMD e execute:

```cmd
java -version
```

Em seguida:

```cmd
javac -version
```

Os dois comandos devem apresentar a versão 17 ou superior.

Caso o comando `javac` não seja reconhecido, será necessário verificar a configuração da variável PATH do Windows.

## 6. Como baixar e executar o projeto

1. Acesse o repositório do projeto no GitHub.
2. Clique no botão **Code**.
3. Selecione **Download ZIP**.
4. Extraia o arquivo baixado para uma pasta do computador.
5. Entre na pasta do projeto.
6. Localize o arquivo `executar.bat`.
7. Execute o arquivo para iniciar o sistema.

O arquivo `executar.bat` automatiza o processo de compilação e inicialização do programa.

### Execução pelo terminal

Também é possível executar o projeto manualmente pelo CMD.

Abra o terminal na pasta principal do projeto e digite:

```cmd
executar.bat
```

## 7. Funcionalidades implementadas

O sistema possui funcionalidades voltadas ao gerenciamento dos serviços e dos clientes:

### Cadastro e gerenciamento
- Cadastro de clientes.
- Cadastro de técnicos.
- Cadastro e gerenciamento de serviços.
- Consulta, alteração e exclusão de registros.

### Ordens de serviço
- Criação de ordens de serviço.
- Organização dos atendimentos.
- Gerenciamento dos serviços solicitados.
- Identificação de atendimentos emergenciais.

### Atendimento emergencial
- Identificação de serviços classificados como emergência.
- Priorização dos atendimentos emergenciais.
- Aplicação de taxa adicional para emergências.

### Programa de fidelidade
- Acúmulo de pontos pelos clientes.
- Concessão de descontos.
- Benefícios para clientes participantes do programa.

### Pagamentos e relatórios
- Registro de pagamentos.
- Controle dos valores dos serviços.
- Consulta e geração de relatórios.

## 8. Armazenamento dos dados

O projeto utiliza arquivos no formato CSV para armazenar as informações cadastradas.

Esse formato permite guardar os registros em arquivos, mantendo os dados disponíveis mesmo depois que o programa é encerrado.

Os arquivos de dados ficam organizados na pasta `dados/`.

**Importante:** não exclua essa pasta durante a utilização do sistema, pois ela pode conter informações cadastradas.

## 9. Testes

O projeto também possui uma opção para executar os testes automatizados.

Para executar, abra o CMD na pasta principal e utilize:

```cmd
executar.bat testes
```

## 10. Observações

- É necessário ter o JDK instalado antes de executar o projeto.
- Não é obrigatório utilizar uma IDE para iniciar o sistema.
- O arquivo `executar.bat` facilita a compilação e a execução no Windows.
- A pasta `dados/` deve ser mantida para preservar os arquivos de armazenamento.

---

**Projeto acadêmico — Conserta Tudo**

Desenvolvido por Isabelly Da Silva Monteiro e Maria Beatriz Barbosa Lopes.
