import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// A excecao sintatica guarda a linha e a mensagem do erro (assim como pedido), separadamente da excecao lexica. Mantivemos os mesmos moldes para fins de padronização
class SyntacticException extends Exception {
    private int line;

    public SyntacticException(int line, String message) {
        super(message);
        this.line = line;
    }

    public int getLine() {
        return line;
    }
}

public class SyntacticAnalyzer {
    private boolean accepted;
    private static int nextStateNumber;
    private State initialState;
    private List<State> states;
    private Map<Integer, Map<String, ParserTableEntry>> actionTable;
    private Map<Integer, Map<String, Integer>> gotoTable;

    // Implementação global da gramática para uso pelo algoritmo de reconhecimento LR(1).
    private static final Grammar GRAMMAR = createGrammar();

    public boolean isAccepted() {
        return accepted;
    }

    public SyntacticAnalyzer() {
        states = new ArrayList<>();
        actionTable = new LinkedHashMap<>();
        gotoTable = new LinkedHashMap<>();
        initialState = createInitialState();
        buildStateGraph();
        buildParserTable();
    }

    private static Grammar createGrammar() {
        Grammar grammar = new Grammar();

        grammar.add("S'", "Program");
        grammar.add("Program", "program", "identifier", "begin", "DeclListOpt", "StmtList", "end", ".");
        grammar.add("DeclListOpt", "DeclList");
        grammar.add("DeclListOpt");
        grammar.add("DeclList", "Decl", ";");
        grammar.add("DeclList", "DeclList", "Decl", ";");
        grammar.add("Decl", "IdentList", "=", "Type");
        grammar.add("IdentList", "identifier");
        grammar.add("IdentList", "IdentList", ",", "identifier");
        grammar.add("Type", "int");
        grammar.add("Type", "float");
        grammar.add("Type", "char");
        grammar.add("StmtList", "Stmt");
        grammar.add("StmtList", "StmtList", ";", "Stmt");
        grammar.add("Stmt", "AssignStmt");
        grammar.add("Stmt", "IfStmt");
        grammar.add("Stmt", "WhileStmt");
        grammar.add("Stmt", "RepeatStmt");
        grammar.add("Stmt", "ReadStmt");
        grammar.add("Stmt", "WriteStmt");
        grammar.add("AssignStmt", "identifier", "=", "SimpleExpr");
        grammar.add("IfStmt", "if", "Condition", "then", "StmtList", "end");
        grammar.add("IfStmt", "if", "Condition", "then", "StmtList", "else", "StmtList", "end");
        grammar.add("Condition", "Expression");
        grammar.add("RepeatStmt", "repeat", "StmtList", "StmtSuffix");
        grammar.add("StmtSuffix", "until", "Condition");
        grammar.add("WhileStmt", "StmtPrefix", "StmtList", "end");
        grammar.add("StmtPrefix", "while", "Condition", "do");
        grammar.add("ReadStmt", "read", "(", "identifier", ")");
        grammar.add("WriteStmt", "write", "(", "Writable", ")");
        grammar.add("Writable", "SimpleExpr");
        grammar.add("Writable", "literal");
        grammar.add("Expression", "SimpleExpr");
        grammar.add("Expression", "SimpleExpr", "Relop", "SimpleExpr");
        grammar.add("SimpleExpr", "Term");
        grammar.add("SimpleExpr", "SimpleExpr", "Addop", "Term");
        grammar.add("Term", "FactorA");
        grammar.add("Term", "Term", "Mulop", "FactorA");
        grammar.add("FactorA", "Factor");
        grammar.add("FactorA", "!", "Factor");
        grammar.add("FactorA", "-", "Factor");
        grammar.add("Factor", "identifier");
        grammar.add("Factor", "Constant");
        grammar.add("Factor", "(", "Expression", ")");
        grammar.add("Relop", "==");
        grammar.add("Relop", ">");
        grammar.add("Relop", ">=");
        grammar.add("Relop", "<");
        grammar.add("Relop", "<=");
        grammar.add("Relop", "!=");
        grammar.add("Addop", "+");
        grammar.add("Addop", "-");
        grammar.add("Addop", "||");
        grammar.add("Mulop", "*");
        grammar.add("Mulop", "/");
        grammar.add("Mulop", "%");
        grammar.add("Mulop", "&&");
        grammar.add("Constant", "integer_const");
        grammar.add("Constant", "float_const");
        grammar.add("Constant", "char_const");

        grammar.computeFirstSets();
        return grammar;
    }

    private State createInitialState() {
        List<LR1Line> initialProductions = new ArrayList<>();
        initialProductions.add(new LR1Line("S'", "$", "<DOT>", "Program"));
        return createState(initialProductions);
    }

    public State createState(List<LR1Line> productions) {
        return new State(productions);
    }

    public State getInitialState() {
        return initialState;
    }

    public List<State> getStates() {
        return states;
    }

    // O grafo LR(1) e montado antes de ler a entrada. Para cada simbolo apos o ponto, avancamos o ponto nos itens correspondentes, comparamos o kernel incluindo lookaheads e criamos um estado somente quando ainda nao existe um equivalente.
    private void buildStateGraph() {
        states.add(initialState);

        for (int currentIndex = 0; currentIndex < states.size(); currentIndex++) {
            State currentState = states.get(currentIndex);
            Set<String> nextSymbols = new LinkedHashSet<>();

            for (LR1Line item : currentState.productions) {
                int dotIndex = item.right.indexOf("<DOT>");
                if (dotIndex >= 0 && dotIndex + 1 < item.right.size()) {
                    nextSymbols.add(item.right.get(dotIndex + 1));
                }
            }

            for (String symbol : nextSymbols) {
                List<LR1Line> kernel = moveDot(currentState, symbol);
                State nextState = findStateWithKernel(kernel);
                if (nextState == null) {
                    nextState = new State(kernel);
                    states.add(nextState);
                }
                currentState.transitions.put(symbol, nextState);
            }
        }
    }

    private List<LR1Line> moveDot(State state, String symbol) {
        List<LR1Line> movedItems = new ArrayList<>();
        for (LR1Line item : state.productions) {
            int dotIndex = item.right.indexOf("<DOT>");
            if (dotIndex >= 0
                && dotIndex + 1 < item.right.size()
                && item.right.get(dotIndex + 1).equals(symbol)) {
                List<String> movedRight = new ArrayList<>(item.right);
                movedRight.remove(dotIndex);
                movedRight.add(dotIndex + 1, "<DOT>");
                movedItems.add(new LR1Line(item.left, item.lookahead, movedRight));
            }
        }
        return movedItems;
    }

    private State findStateWithKernel(List<LR1Line> kernel) {
        for (State state : states) {
            if (state.hasSameKernel(kernel)) {
                return state;
            }
        }
        return null;
    }

    // ACTION escolhe shift, reduce ou accept usando o estado e o terminal de lookahead; GOTO leva ao estado seguinte depois de uma reducao por nao terminal.
    private void buildParserTable() {
        for (State state : states) {
            actionTable.put(state.number, new LinkedHashMap<>());
            gotoTable.put(state.number, new LinkedHashMap<>());
        }

        for (State state : states) {
            for (Map.Entry<String, State> transition : state.transitions.entrySet()) {
                String symbol = transition.getKey();
                State target = transition.getValue();
                if (GRAMMAR.isNonTerminal(symbol)) {
                    gotoTable.get(state.number).put(symbol, target.number);
                } else {
                    putAction(state.number, symbol, new ParserTableEntry("shift", target.number));
                }
            }

            for (LR1Line item : state.productions) {
                int dotIndex = item.right.indexOf("<DOT>");
                if (dotIndex != item.right.size() - 1) {
                    continue;
                }

                int productionNumber = GRAMMAR.findProductionNumber(item);
                if (productionNumber == 0) {
                    putAction(state.number, item.lookahead, new ParserTableEntry("accept", 0));
                } else {
                    putAction(
                        state.number,
                        item.lookahead,
                        new ParserTableEntry("reduce", productionNumber));
                }
            }
        }
    }

    private void putAction(int stateNumber, String terminal, ParserTableEntry action) {
        Map<String, ParserTableEntry> row = actionTable.get(stateNumber);
        ParserTableEntry existing = row.get(terminal);
        if (existing != null
            && (!existing.operation.equals(action.operation) || existing.value != action.value)) {
            throw new IllegalStateException(
                "Conflito LR(1) no estado " + stateNumber + " para o terminal " + terminal);
        }
        row.put(terminal, action);
    }

    // O parser consulta ACTION com o estado atual e o proximo token; shift consome o token, reduce troca os estados conforme a producao e GOTO, e accept marca a entrada como reconhecida.
    public void runSyntacticAnalysis(LexicalAnalyzer lexer)
        throws java.io.IOException, LexicalException, SyntacticException {
        accepted = false;
        List<Integer> stateStack = new ArrayList<>();
        stateStack.add(initialState.number);

        Token lookahead = lexer.scan();
        int lookaheadLine = LexicalAnalyzer.line;

        while (true) {
            int stateNumber = stateStack.get(stateStack.size() - 1);
            String terminal = getTerminal(lookahead);
            ParserTableEntry action = actionTable.get(stateNumber).get(terminal);

            if (action == null) {
                String tokenDescription = lookahead == null
                    ? "fim do arquivo"
                    : "'" + lookahead.toString() + "'";
                throw new SyntacticException(
                    lookaheadLine,
                    "token inesperado " + tokenDescription);
            }

            if (action.operation.equals("shift")) {
                stateStack.add(action.value);
                lookahead = lexer.scan();
                lookaheadLine = LexicalAnalyzer.line;
            } else if (action.operation.equals("reduce")) {
                Production production = GRAMMAR.getProduction(action.value);
                for (int i = 0; i < production.right.size(); i++) {
                    stateStack.remove(stateStack.size() - 1);
                }

                int previousState = stateStack.get(stateStack.size() - 1);
                Integer nextState = gotoTable.get(previousState).get(production.left);
                if (nextState == null) {
                    throw new IllegalStateException(
                        "GOTO ausente para o estado " + previousState + " e " + production.left);
                }
                stateStack.add(nextState);
            } else {
                accepted = true;
                return;
            }
        }
    }

    private String getTerminal(Token token) {
        if (token == null) {
            return "$";
        }

        switch (token.tag) {
            case Tag.PROGRAM: return "program";
            case Tag.ID: return "identifier";
            case Tag.BEGIN: return "begin";
            case Tag.END: return "end";
            case Tag.IF: return "if";
            case Tag.THEN: return "then";
            case Tag.ELSE: return "else";
            case Tag.WHILE: return "while";
            case Tag.DO: return "do";
            case Tag.REPEAT: return "repeat";
            case Tag.UNTIL: return "until";
            case Tag.READ: return "read";
            case Tag.WRITE: return "write";
            case Tag.NUM: return "integer_const";
            case Tag.REAL: return "float_const";
            case Tag.CHAR_CONST: return "char_const";
            case Tag.LITERAL: return "literal";
            case Tag.EQ: return "==";
            case Tag.NE: return "!=";
            case Tag.LE: return "<=";
            case Tag.GE: return ">=";
            case Tag.OR: return "||";
            case Tag.AND: return "&&";
            case Tag.BASIC:
                if (token instanceof Word) {
                    return ((Word) token).getLexeme();
                }
                break;
            default:
                return String.valueOf((char) token.tag);
        }
        throw new IllegalArgumentException("Token sem terminal da gramatica: " + token.tag);
    }

    public class LR1Line {
        private String left;
        private List<String> right;
        private String lookahead;

        public LR1Line(String left, String lookahead, String... right) {
            this.left = left;
            this.right = new ArrayList<>(Arrays.asList(right));
            this.lookahead = lookahead;
        }

        private LR1Line(String left, String lookahead, List<String> right) {
            this.left = left;
            this.right = new ArrayList<>(right);
            this.lookahead = lookahead;
        }

        public String getLeft() {
            return left;
        }

        public List<String> getRight() {
            return right;
        }

        public String getLookahead() {
            return lookahead;
        }
    }

    public class State {
        private ArrayList<LR1Line> initialProductions;
        private ArrayList<LR1Line> productions;
        private int number;
        private Map<String, State> transitions;

        private State(List<LR1Line> initialProductions) {
            number = nextStateNumber++;
            this.initialProductions = new ArrayList<>();
            productions = new ArrayList<>();
            transitions = new LinkedHashMap<>();
            for (LR1Line production : initialProductions) {
                addInitialProduction(production);
            }
            computeClosure();
        }

        public ArrayList<LR1Line> getInitialProductions() {
            return initialProductions;
        }

        public ArrayList<LR1Line> getProductions() {
            return productions;
        }

        public int getNumber() {
            return number;
        }

        public Map<String, State> getTransitions() {
            return transitions;
        }

        // Função de comparação do núcleo do estado para evitar criar dois estados com as mesmas produções iniciais.
        public boolean hasSameKernel(List<LR1Line> candidateKernel) {
            if (candidateKernel == null) {
                return false;
            }

            List<LR1Line> uniqueCandidates = new ArrayList<>();
            for (LR1Line candidate : candidateKernel) {
                if (!containsItem(uniqueCandidates, candidate)) {
                    uniqueCandidates.add(candidate);
                }
            }

            if (initialProductions.size() != uniqueCandidates.size()) {
                return false;
            }

            for (LR1Line production : initialProductions) {
                if (!containsItem(uniqueCandidates, production)) {
                    return false;
                }
            }
            return true;
        }

        private void computeClosure() {
            for (int i = 0; i < productions.size(); i++) {
                LR1Line item = productions.get(i);
                int dotIndex = item.right.indexOf("<DOT>");
                if (dotIndex < 0) {
                    throw new IllegalArgumentException("Item LR(1) sem ponto: " + item.left);
                }

                int symbolIndex = dotIndex + 1;
                if (symbolIndex >= item.right.size()) {
                    continue;
                }

                String symbol = item.right.get(symbolIndex);
                if (!GRAMMAR.isNonTerminal(symbol)) {
                    continue;
                }

                List<String> suffix = item.right.subList(symbolIndex + 1, item.right.size());
                Set<String> lookaheads = GRAMMAR.firstOfSequence(suffix, item.lookahead);
                for (Production production : GRAMMAR.getProductionsFor(symbol)) {
                    List<String> dottedRight = new ArrayList<>();
                    dottedRight.add("<DOT>");
                    dottedRight.addAll(production.right);

                    for (String lookahead : lookaheads) {
                        addIfMissing(new LR1Line(production.left, lookahead, dottedRight));
                    }
                }
            }
        }

        private void addInitialProduction(LR1Line candidate) {
            if (!containsItem(initialProductions, candidate)) {
                initialProductions.add(candidate);
                addIfMissing(candidate);
            }
        }

        private void addIfMissing(LR1Line candidate) {
            if (!containsItem(productions, candidate)) {
                productions.add(candidate);
            }
        }

        private boolean containsItem(List<LR1Line> items, LR1Line candidate) {
            for (LR1Line existing : items) {
                if (existing.left.equals(candidate.left)
                    && existing.right.equals(candidate.right)
                    && existing.lookahead.equals(candidate.lookahead)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class Grammar {
        private List<Production> productions = new ArrayList<>();
        private Map<String, Set<String>> firstSets = new LinkedHashMap<>();
        private Set<String> nullable = new HashSet<>();

        private void add(String left, String... right) {
            productions.add(new Production(left, right));
        }

        private void computeFirstSets() {
            for (Production production : productions) {
                firstSets.putIfAbsent(production.left, new LinkedHashSet<>());
            }

            boolean changed;
            do {
                changed = false;
                for (Production production : productions) {
                    boolean allNullable = true;
                    for (String symbol : production.right) {
                        if (isNonTerminal(symbol)) {
                            changed |= firstSets.get(production.left).addAll(firstSets.get(symbol));
                            if (!nullable.contains(symbol)) {
                                allNullable = false;
                                break;
                            }
                        } else {
                            changed |= firstSets.get(production.left).add(symbol);
                            allNullable = false;
                            break;
                        }
                    }

                    if (allNullable) {
                        changed |= nullable.add(production.left);
                    }
                }
            } while (changed);
        }

        private boolean isNonTerminal(String symbol) {
            return firstSets.containsKey(symbol);
        }

        private List<Production> getProductionsFor(String left) {
            List<Production> matchingProductions = new ArrayList<>();
            for (Production production : productions) {
                if (production.left.equals(left)) {
                    matchingProductions.add(production);
                }
            }
            return matchingProductions;
        }

        private Production getProduction(int number) {
            return productions.get(number);
        }

        private int findProductionNumber(LR1Line item) {
            List<String> rightWithoutDot = new ArrayList<>(item.right);
            rightWithoutDot.remove("<DOT>");

            for (int i = 0; i < productions.size(); i++) {
                Production production = productions.get(i);
                if (production.left.equals(item.left) && production.right.equals(rightWithoutDot)) {
                    return i;
                }
            }
            throw new IllegalArgumentException("Producao nao encontrada para o item " + item.left);
        }

        private Set<String> firstOfSequence(List<String> sequence, String trailingLookahead) {
            Set<String> result = new LinkedHashSet<>();
            boolean allNullable = true;

            for (String symbol : sequence) {
                if (isNonTerminal(symbol)) {
                    result.addAll(firstSets.get(symbol));
                    if (!nullable.contains(symbol)) {
                        allNullable = false;
                        break;
                    }
                } else {
                    result.add(symbol);
                    allNullable = false;
                    break;
                }
            }

            if (allNullable) {
                result.add(trailingLookahead);
            }
            return result;
        }
    }

    private static class Production {
        private String left;
        private List<String> right;

        private Production(String left, String... right) {
            this.left = left;
            this.right = new ArrayList<>(Arrays.asList(right));
        }
    }

    private static class ParserTableEntry {
        private String operation;
        private int value;

        private ParserTableEntry(String operation, int value) {
            this.operation = operation;
            this.value = value;
        }
    }
}
