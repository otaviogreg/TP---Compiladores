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

    public boolean isAccepted() {
        return accepted;
    }

    // Criação do protótipo para incremento futuro. Fizemos apenas o estado inicial do parser.
    public SyntacticAnalyzer() {
        initialState = createInitialState();
    }

    private List<LR1Line> createInitialState() {
        List<LR1Line> state = new ArrayList<>();
        state.add(new LR1Line("S'", "$", ".", "Program"));
        state.add(new LR1Line(
            "Program",
            "$",
            ".",
            "PROGRAM",
            "ID",
            "BEGIN",
            "DeclListOpt",
            "StmtList",
            "END",
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
}
