import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Ponto de entrada e coordenador do processo de compilacao.
 *
 * Recebe o arquivo-fonte, instancia os analisadores e encaminha o lexer ao
 * analisador sintatico. A analise semantica ainda nao faz parte do compilador.
 */
public class Compiler {
    private LexicalAnalyzer lexicalAnalyzer;
    private SyntacticAnalyzer syntacticAnalyzer;

    /**
     * Cria os analisadores usados para compilar o arquivo informado.
     *
     * @param sourceFile caminho do programa-fonte
     * @throws FileNotFoundException se o arquivo nao puder ser encontrado
     */
    public Compiler(String sourceFile) throws FileNotFoundException {
        lexicalAnalyzer = new LexicalAnalyzer(sourceFile);
        syntacticAnalyzer = new SyntacticAnalyzer();
    }

    /**
     * Executa a analise sintatica; o parser solicita tokens ao lexer sob demanda.
     * O arquivo-fonte e fechado ao final, inclusive quando ocorre um erro.
     *
     * @throws IOException se ocorrer um erro de leitura ou ao fechar o arquivo
     * @throws LexicalException se o lexer encontrar um erro
     * @throws SyntacticException se o parser encontrar um erro
     */
    public void compile() throws IOException, LexicalException, SyntacticException {
        try (LexicalAnalyzer lexer = lexicalAnalyzer) {
            syntacticAnalyzer.runSyntacticAnalysis(lexer);
        }
    }

    /**
     * Valida os argumentos, executa a compilacao e apresenta o primeiro erro
     * encontrado ou uma mensagem de sucesso.
     *
     * @param args deve conter o caminho de um arquivo-fonte
     */
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
