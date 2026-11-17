import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private List<LR1Line> initialState;

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

        return grammar;
    }

    private List<LR1Line> createInitialState() {
        List<LR1Line> state = new ArrayList<>();
        state.add(new LR1Line("S'", "$", ".", "Program"));
        state.add(new LR1Line(
            "Program",
            "$",
            ".",
            "program",
            "identifier",
            "begin",
            "DeclListOpt",
            "StmtList",
            "end",
            "."));
        return state;
    }

    public List<LR1Line> getInitialState() {
        return initialState;
    }

    public class LR1Line {
        private String left;
        private List<String> right;
        private String lookahead;

        private LR1Line(String left, String lookahead, String... right) {
            this.left = left;
            this.right = new ArrayList<>(Arrays.asList(right));
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

    private static final class Grammar {
        private List<Production> productions = new ArrayList<>();

        private void add(String left, String... right) {
            productions.add(new Production(left, right));
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
