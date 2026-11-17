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

    // Implementação global da gramática para uso pelo algoritmo de reconhecimento LR(1).
    private static final Grammar GRAMMAR = createGrammar();

    public boolean isAccepted() {
        return accepted;
    }

    public SyntacticAnalyzer() {
        initialState = createInitialState();
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

        private State(List<LR1Line> initialProductions) {
            number = nextStateNumber++;
            this.initialProductions = new ArrayList<>();
            productions = new ArrayList<>();
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
}
