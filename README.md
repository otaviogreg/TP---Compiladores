# Compilador em Java

Trabalho prático da disciplina de Compiladores. O projeto realiza a análise léxica e a análise sintática do programa-fonte em um único fluxo, coordenado pela classe `Compiler`. A análise semântica ainda não está implementada.

> Detalhes completos da implementação e dos testes estão no relatório do trabalho.

## Requisitos

- JDK 9 ou superior instalado.

## Como usar

O compilador é executado pela linha de comando e recebe o caminho do programa-fonte como argumento:

```bash
javac *.java
java Compiler testes_corrigidos/teste1_corrigido.txt
```

Caso esteja utilizando o JAR, execute-o assim:

```bash
java -jar TP-Compiladores.jar testes_corrigidos/teste1_corrigido.txt
```

O programa informa sucesso quando a análise sintática aceita a entrada. Se encontrar um erro léxico ou sintático, exibe o primeiro erro e sua linha.

## Como funciona

- `Compiler` contém a `main`, recebe o caminho do arquivo e coordena as etapas da compilação.
- `LexicalAnalyzer` lê o código-fonte, reconhece lexemas e fornece tokens sob demanda ao analisador sintático. A lista de tokens não é impressa durante a execução.
- `SyntacticAnalyzer` usa uma gramática LR(1) aumentada com `S' → Program`. Os itens LR(1) incluem lookahead, que é considerado na comparação dos núcleos dos estados.
- Antes de analisar a entrada, o analisador sintático constrói os estados e suas transições, calcula os fechamentos e monta as tabelas `ACTION` e `GOTO`.
- Durante a análise, `ACTION` determina operações de shift, reduce e accept; `GOTO` determina a transição após uma redução. O parser consome os tokens do lexer até aceitar o programa ou encontrar o primeiro erro sintático.
- A análise semântica, como verificação de declarações e compatibilidade de tipos, ainda não está implementada.

## Testes

- `testes_originais/`: programas de teste na forma original (`teste1_original.txt`, ...).
- `testes_corrigidos/`: versões corrigidas dos programas de teste (`teste1_corrigido.txt`, ...).
- `logs/`: saídas registradas para os arquivos de teste.
- Os testes 6a e 6b são os dois programas autorais pedidos no enunciado.

## Autores

- Davi da Silva Braga
- Hugo Daniel Amaral Oliveira
- Otávio Andrade e Ferreira