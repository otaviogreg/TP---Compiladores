# Compilador em Java — Etapa 1: Análise Léxica e Tabela de Símbolos

Trabalho prático da disciplina de Compiladores. Esta etapa implementa as duas fases iniciais da compilação: o **analisador léxico** e a **tabela de símbolos**, em Java, com modelagem orientada a objetos.

> Detalhes completos da implementação e dos testes estão no relatório da etapa.

## Requisitos

- JDK (Java Development Kit) instalado

## Como usar

O programa roda apenas por linha de comando e exige o arquivo com o código-fonte como parâmetro:

```bash
javac *.java
java lexicalAnalyzer teste1.txt
```

A saída mostra, no terminal, a sequência de tokens identificados e, ao final, os registros da tabela de símbolos.

## Como funciona

- Lê o arquivo caractere por caractere, descartando espaços, tabulações, quebras de linha e comentários válidos.
- Agrupa os caracteres em lexemas, valida-os contra os padrões léxicos e gera o token correspondente.
- A **tabela de símbolos** usa `Hashtable` e é pré-carregada com as palavras reservadas (`program`, `begin`, `if`, `while`, `int`, `float`, etc.).
- Ao reconhecer um identificador, o analisador consulta a tabela: se já existir, retorna o token mapeado; se não, insere uma nova entrada.

## Autores

- Davi da Silva Braga
- Hugo Daniel Amaral Oliveira
- Otávio Andrade e Ferreira
