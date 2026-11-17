import java.io.FileNotFoundException;
import java.io.IOException;

// LER: Compiler controla o fluxo da compilacao: cria o lexer e o parser, entrega o lexer ao analisador sintatico para que este consuma os tokens sob demanda, e deixa as etapas semanticas para uma implementacao futura.
public class Compiler {
    private LexicalAnalyzer lexicalAnalyzer;
    private SyntacticAnalyzer syntacticAnalyzer;

    public Compiler(String sourceFile) throws FileNotFoundException {
        lexicalAnalyzer = new LexicalAnalyzer(sourceFile);
        syntacticAnalyzer = new SyntacticAnalyzer();
    }

    public void compile() throws IOException, LexicalException, SyntacticException {
        try (LexicalAnalyzer lexer = lexicalAnalyzer) {
            syntacticAnalyzer.runSyntacticAnalysis(lexer);
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: java Compiler <arquivo-fonte>");
            System.exit(1);
        }

        try {
            Compiler compiler = new Compiler(args[0]);
            compiler.compile();
            System.out.println("Analise sintatica concluida com sucesso.");
        } catch (FileNotFoundException e) {
            System.err.println("Arquivo nao encontrado. Erro: " + e.getMessage());
            System.exit(1);
        } catch (LexicalException e) {
            System.err.println("Erro lexico na linha " + e.getLine() + ": " + e.getMessage());
            System.exit(1);
        } catch (SyntacticException e) {
            System.err.println("Erro sintatico na linha " + e.getLine() + ": " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Erro de leitura no arquivo. Erro: " + e.getMessage());
            System.exit(1);
        }
    }
}
