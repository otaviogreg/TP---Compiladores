import java.io.FileNotFoundException;

// Recebe o caminho do programa-fonte e instancia os analisadores em objetos separados (ainda nao encaminha tokens: isso sera ligado quando SyntacticAnalyzer oferecer um metodo de entrada para o parser).
public class Compiler {
    private LexicalAnalyzer lexicalAnalyzer;
    private SyntacticAnalyzer syntacticAnalyzer;

    public Compiler(String sourceFile) throws FileNotFoundException {
        lexicalAnalyzer = new LexicalAnalyzer(sourceFile);
        syntacticAnalyzer = new SyntacticAnalyzer();
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: java Compiler <arquivo-fonte>");
            System.exit(1);
        }

        try {
            new Compiler(args[0]);
            System.out.println(
                "Analisadores inicializados; o encaminhamento de tokens para o parser sera conectado na proxima etapa.");
        } catch (FileNotFoundException e) {
            System.err.println("Arquivo nao encontrado. Erro: " + e.getMessage());
            System.exit(1);
        }
    }
}
