# Modificações no analisador léxico para o funcionamento correto da análise sintática

## Decisões de arquitetura

- Foi criada a classe `Compiler`, em `Compiler.java`, como ponto de entrada do compilador. Ela recebe o caminho do programa-fonte, instancia o analisador léxico e o sintático, e coordena o fluxo de tokens entre eles.
- A `main`, antes contida em `LexicalAnalyzer`, foi movida para `Compiler`. A classe `LexicalAnalyzer` ficou responsável pelo reconhecimento e fornecimento de tokens.
- O analisador sintático usa a gramática LR(1) aumentada com `S' → Program`. Os itens incluem lookahead, que é considerado na comparação dos núcleos dos estados.
- Antes da análise da entrada, o analisador sintático constrói os estados e suas transições, calcula os fechamentos e monta as tabelas `ACTION` e `GOTO`.
- O parser consome os tokens sob demanda do lexer. A análise semântica poderá ser integrada posteriormente.

## Modificações no analisador léxico

- Inteiros (`integer_const`) e reais (`float_const`) passaram a produzir tokens distintos: `Tag.NUM` e `Tag.REAL`, respectivamente.
- Erros léxicos interrompem a análise no primeiro erro e informam a linha de ocorrência.
- A sequência de tokens reconhecidos deixou de ser impressa na saída; os tokens são fornecidos ao analisador sintático.
- A classe e o arquivo do analisador léxico foram renomeados para `LexicalAnalyzer` e `LexicalAnalyzer.java`.
