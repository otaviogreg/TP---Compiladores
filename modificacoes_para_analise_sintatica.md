# Modificações para a análise sintática

## Decisões de arquitetura

- A classe `Compiler`, em `Compiler.java`, é o ponto de entrada do compilador e contém a `main` global. Ela recebe o caminho do programa-fonte, instancia os analisadores léxico e sintático separadamente e passa o lexer ao parser, que consome os tokens sob demanda. A análise semântica poderá ser integrada posteriormente.
- O analisador léxico é a classe `LexicalAnalyzer`, em `LexicalAnalyzer.java`, e produz um token por chamada a `scan()`. O recurso do arquivo-fonte é fechado pelo `Compiler` ao fim da análise.
- O analisador sintático será implementado como LR(1). A gramática terá uma produção aumentada `S' → Program`; seus itens carregam lookahead, que deve ser considerado na comparação de núcleos de estados.
- A comparação de núcleos de estados considera lado esquerdo, lado direito e lookahead de cada item, pois lookaheads diferentes representam itens LR(1) diferentes.

## Ajustes lexicais necessários à integração

- Inteiros (`integer_const`) e reais (`float_const`) produzem tokens distintos: `Tag.NUM` e `Tag.REAL`, respectivamente.
- A linguagem é case-sensitive; lexemas são consultados e armazenados preservando a caixa original.
- Comentários começam com `{*` e terminam com `*}`; podem ocupar mais de uma linha.
- Erros léxicos interrompem a análise no primeiro erro e informam a linha de ocorrência.
- A sequência de tokens não é mais impressa na saída; os tokens são produzidos para consumo pelas etapas seguintes.
- A classe e o arquivo do analisador léxico foram renomeados para `LexicalAnalyzer`.