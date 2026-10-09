/**
 * Trabalho Prático de Compiladores - Etapa 1: Analisador Léxico
 * 
 * Este script implementa um Analisador Léxico (Lexer) em Java, adaptado
 * diretamente do modelo de orientação a objetos para compiladores ensinado durante as aulas.
 * 
 * Autores: Otávio Andrade, Davi Braga e Hugo Daniel
 */

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;

/**
 * Classificações de Tokens (Tags).
 * 
 * A classe Tag define constantes numéricas para diferenciar tipos de tokens,
 * como palavras reservadas, identificadores, números e operadores compostos.
 * Tokens de caractere único (ex: '+', ';') usam seu próprio valor ASCII
 */
class Tag {
    // Definindo a constante global para o fim do arquivo (End Of File). Caractere utilizado nesse modelo de compilador utilizando Java
    public static final char EOF = (char) 65535;

    public static final int
        AND = 256,
        BASIC = 257,
        DO = 258,
        ELSE  = 259,
        EQ = 260,
        GE = 262,
        ID = 263,
        IF = 264,
        LE = 265,
        NE = 266,
        NUM = 267,
        OR = 268,
        REAL = 269,
        TEMP = 270,
        WHILE = 272,
        PROGRAM = 273,
        BEGIN = 274,
        END = 275,
        READ = 276,
        WRITE = 277,
        THEN = 278,
        REPEAT = 279,
        UNTIL = 280,
        LITERAL = 281,
        CHAR_CONST = 282;
}

/**
 * Representação base de um Token utilizando as especificações explicadas em sala
 */
class Token {
    public final int tag;

    public Token(int t) {
        this.tag = t;
    }

    @Override
    public String toString() {
        return "" + (char) tag;
    }
}

/**
 * Token especializado para números (trabalhando apenas com double pelas especificações técnicas passadas no TP)
 */
class Num extends Token {
    public final double floatValue;

    public Num(double floatValue) {
        super(Tag.NUM);
        this.floatValue = floatValue;
    }

    @Override
    public String toString() {
        return "" + floatValue;
    }
}

/**
 * Token especializado para palavras (qualquer token que possua caracteres em sua composição)
 */
class Word extends Token {
    private String lexeme = "";

    // Instâncias de palavras e operadores pré-definidos na linguagem
    public static final Word and = new Word("&&", Tag.AND);
    public static final Word or = new Word("||", Tag.OR);
    public static final Word eq = new Word("==", Tag.EQ);
    public static final Word ne = new Word("!=", Tag.NE);
    public static final Word le = new Word("<=", Tag.LE);
    public static final Word ge = new Word(">=", Tag.GE);

    public Word(String s, int tag) {
        super(tag);
        this.lexeme = s;
    }

    public String getLexeme() {
        return lexeme;
    }

    @Override
    public String toString() {
        return lexeme;
    }
}

/**
 * Classe principal do analisador léxico
 * 
 * @summary Responsável por ler o arquivo de código fonte caractere por caractere,
 * ignorar espaços e comentários, gerenciar a contagem de linhas e converter
 * os lexemas encontrados em Tokens estruturados (Token, Num, Word)
 */
class Lexer {
    public static int line = 1;
    private char ch = ' ';
    private Reader file;
    private Hashtable<String, Word> words = new Hashtable<String, Word>();
    private List<String> errors = new ArrayList<>();

    /**
     * Associa uma palavra reservada ou token à Tabela de Símbolos.
    */
    private void reserve(Word w) {
        words.put(w.getLexeme(), w);
    }

    private void reportError(String msg) {
        String full = "Erro lexico na linha " + line + ": " + msg;
        errors.add(full);
        System.err.println(full);
    }

    private boolean isLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    public List<String> getErrors() {
        return errors;
    }

    /**
     * Inicializa a leitura do arquivo (sempre em UTF-8, independente do sistema) e reserva todas as palavras-chave da linguagem.
     * Os operadores (&&, ||, ==, ...) não entram na Tabela de Símbolos: são devolvidos diretamente pelo scan().
     *
     * @param fileName Caminho do arquivo de código fonte.
     * @throws FileNotFoundException Caso o arquivo informado não seja encontrado.
     */
    public Lexer(String fileName) throws FileNotFoundException {
        file = new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8);

        // Reserva das palavras-chave da linguagem
        reserve(new Word("if", Tag.IF));
        reserve(new Word("else", Tag.ELSE));
        reserve(new Word("while", Tag.WHILE));
        reserve(new Word("do", Tag.DO));
        reserve(new Word("program", Tag.PROGRAM));
        reserve(new Word("begin", Tag.BEGIN));
        reserve(new Word("end", Tag.END));
        reserve(new Word("read", Tag.READ));
        reserve(new Word("write", Tag.WRITE));
        reserve(new Word("int", Tag.BASIC));
        reserve(new Word("float", Tag.BASIC));
        reserve(new Word("char", Tag.BASIC));
        reserve(new Word("then", Tag.THEN));
        reserve(new Word("repeat", Tag.REPEAT));
        reserve(new Word("until", Tag.UNTIL));
    }

    /**
     * Lê o próximo caractere do arquivo fonte e atualiza a variável 'ch'
     * 
     * @throws IOException
     */
    private void readch() throws IOException {
        int r = file.read();

        if (r == -1) {
            ch = Tag.EOF; // Fim do arquivo
        } else {
            ch = (char) r;
        }
    }

    /**
     * Avança a leitura e verifica se o próximo caractere coincide com o caractere passado no parâmetro
     * Utilizado, principalmente, para reconhecimento de palavras com mais de um caractere (por exemplo '==', '<=')
     * 
     * @param c Caractere esperado a seguir
     * @return true se o próximo caractere for igual ao parâmetro 'c', false caso contrário
     */
    private boolean readch(char c) throws IOException {
        readch();

        if (this.ch != c) {
            return false;
        }

        this.ch = ' '; // Reseta o caractere para evitar leitura dupla
        return true;
    }

    /**
     * Lê o código fonte sob demanda e retorna o próximo Token identificado. É a pricipal função do analisador léxico.
     * 
     * @return Próximo Token reconhecido, na etapa 2 do TP será utilizado para enviar valores ao analisador sintático
     * @throws IOException
     */
    public Token scan() throws IOException {
        // Ignora espaços em branco, tabulações, novas linhas e comentários
        for (;; readch()) {

            if (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\uFEFF') { // '\uFEFF' = BOM de arquivos UTF-8
                continue;
            } else if (ch == '\n') {
                line++;

            } else if (ch == '{') {
                readch(); 
                
                if (ch == '*') {
                    readch();
                    boolean closed = false;

                    while (ch != Tag.EOF) {
                        if (ch == '\n') {
                            line++;
                        }
                        
                        // Se encontrar '*', verificamos se o próximo é '}' para fechar
                        if (ch == '*') {
                            readch();
                            if (ch == '}') {
                                closed = true; // O '}' é consumido pelo readch() do for, que também lê o caractere seguinte
                                break;
                            } else continue; // Considera o asterisco como parte do comentário e continua a leitura
                        }

                        readch();
                    }
                    
                    if (!closed) {
                        reportError("Comentário não fechado");
                    }
                } else {
                    // '{' sem '*' não inicia comentário: é um símbolo comum. 'ch' já guarda o próximo caractere, que ainda será analisado
                    return new Token('{');
                }
            } else {
                break;
            }
        }

        // Reconhecimento de Operadores Compostos
        switch (ch) {
            case '&':
                if (readch('&')) return Word.and; else return new Token('&');
            case '|':
                if (readch('|')) return Word.or; else return new Token('|');
            case '=':
                if (readch('=')) return Word.eq; else return new Token('=');
            case '!':
                if (readch('=')) return Word.ne; else return new Token('!');
            case '<':
                if (readch('=')) return Word.le; else return new Token('<');
            case '>':
                if (readch('=')) return Word.ge; else return new Token('>');
        }

        // Reconhecimento de Números (inteiros e floats)
        if (isDigit(ch)) {
            StringBuilder sb = new StringBuilder();
            boolean erro = false;

            // Parte inteira
            do {
                sb.append(ch);
                readch();
            } while (isDigit(ch));

            // Parte decimal: float_const ::= digit+ "." digit+
            if (ch == '.') {
                sb.append(ch);
                readch(); // consome o ponto

                if (isDigit(ch)) {
                    do {
                        sb.append(ch);
                        readch();
                    } while (isDigit(ch));
                } else {
                    erro = true;
                    reportError("float mal formado '" + sb + "' (esperado digito apos o ponto)");
                }
            }

            // Número colado em letras (ex: 1c): não é número nem identificador válido
            if (isLetter(ch) || ch == '_') {
                while (isLetter(ch) || isDigit(ch) || ch == '_') {
                    sb.append(ch);
                    readch();
                }
                if (!erro) {
                    erro = true;
                    reportError("token invalido '" + sb + "' (identificador nao pode comecar com digito)");
                }
            }

            // Em caso de erro, descarta o lexema e segue para o próximo token
            if (erro) {
                return scan();
            }

            return new Num(Double.parseDouble(sb.toString()));
        }

        // Reconhecimento de Identificadores e Palavras Reservadas
        if (isLetter(ch) || ch == '_') {

            StringBuilder sb = new StringBuilder();
            
            do {
                sb.append(ch);
                readch();
            } while (isLetter(ch) || isDigit(ch) || ch == '_');

            String s = sb.toString();
            Word w = words.get(s);

            // Se a palavra já estiver na TS, retorna ela mesma
            if (w != null) {
                return w;
            } else {
                // Caso contrário, se for um novo identificador, insere na TS e a retora
                w = new Word(s, Tag.ID);
                words.put(s, w);
                return w;
            }
        }

        // Reconhecimento de cadeias de String
        if (ch == '"') {
            StringBuilder sb = new StringBuilder();
            readch();

            while (ch != '"' && ch != '\n' && ch != Tag.EOF) {
                sb.append(ch);
                readch();
            }

            if (ch == '"') {
                readch();
                return new Word(sb.toString(), Tag.LITERAL);
            }

            reportError("string nao fechada (literal nao pode conter quebra de linha)");
            return scan();
        }

        // Reconhecimento de Constantes de Caractere
        if (ch == '\'') {
            readch();
            char characterValue = ch;
            readch(); // Lê o caractere interno
            
            if (ch == '\'') {
                readch();
            } else {
                reportError("Constante de caractere mal formatada");
            }
            
            return new Word(String.valueOf(characterValue), Tag.CHAR_CONST);
        }

        // Fim de Arquivo
        if (ch == Tag.EOF) {
            return null;
        }

        if (ch > 127) {
            reportError("caractere invalido '" + ch + "' (fora do alfabeto da linguagem)");
            ch = ' ';
            return scan();
        }
        // Reconhecimento default de tokens de caractere único que não possuem nenhuma das especificações tratadas acima (exemplo '+', ';', etc)
        Token tok = new Token(ch);
        ch = ' ';
        return tok;
    }

    /**
     * Retorna a TD para consulta de forma pública, permitindo que o analisador sintático acesse os tokens reconhecidos e armazenados
     * 
     * @return Hashtable com todas as palavras reservadas e identificadores.
     */
    public Hashtable<String, Word> getWords() {
        return words;
    }
}

/**
 * Classe principal para execução e teste do Analisador Léxico.
 */
public class lexicalAnalyzer {

    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("Uso: java lexicalAnalyzer <arquivo-fonte>   (ou: java -jar TP1-Compiladores.jar <arquivo-fonte>)");
            System.exit(1);
        }

        String fileName = args[0]; // Primeiro argumento do CLI contendo o caminho do arquivo

        try {
            Lexer lexer = new Lexer(fileName);

            Token t;
            int tokenCount = 0;

            // Executa a análise token por token até o fim do arquivo
            while ((t = lexer.scan()) != null) {
                tokenCount++;
                System.out.println("Token de número " + tokenCount + ", linha " + Lexer.line + ", tag: " + t.tag + " e representacao: " + t.toString());
            }

            // Exibição da Tabela de Símbolos para verificação dos valores inseridos pelo analisador léxico
            System.out.println("\nTabela de Simbolos:\n");

            Hashtable<String, Word> table = lexer.getWords();

            for (String key : table.keySet()) {
                Word tableWord = table.get(key);
                System.out.println("Lexema: " + tableWord.getLexeme() + " e tag: " + tableWord.tag);
            }

            System.out.println("\nResumo de erros lexicos:\n");
            if (lexer.getErrors().isEmpty()) {
                System.out.println("Nenhum erro lexico encontrado. Analise lexica concluida com sucesso.");
            } else {
                for (String e : lexer.getErrors()) {
                    System.out.println(e);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("Arquivo nao encontrado. Erro: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Erro de leitura no arquivo. Erro: " + e.getMessage());
        }
    }
}