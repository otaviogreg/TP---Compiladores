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
 * Token especializado para números.
 */
class Num extends Token {
    public final double floatValue;

    // O tipo número possui a mesma especificação de classe para números inteiros e reais. A diferenciação é feita pelo envio da tag para o construtor.
    public Num(double floatValue, int tag) {
        super(tag);
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

// Representa o primeiro erro encontrado pelo analisador lexico e guarda a linha para que o programa possa exibir o diagnostico e interromper a analise sem acumular erros posteriores.
class LexicalException extends Exception {
    private final int line;

    public LexicalException(int line, String message) {
        super(message);
        this.line = line;
    }

    public int getLine() {
        return line;
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

    /**
     * Associa uma palavra reservada ou token à Tabela de Símbolos.
    */
    private void reserve(Word w) {
        words.put(w.getLexeme(), w);
    }

    // Um dos requisitos pedidos é a exibição do primeiro erro, apenas. Modificamos para atender ao enunciado.
    private void reportError(String msg) throws LexicalException {
        throw new LexicalException(line, msg);
    }

    private boolean isLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
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
    public Token scan() throws IOException, LexicalException {
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
            boolean isReal = false;

            // Parte inteira
            do {
                sb.append(ch);
                readch();
            } while (isDigit(ch));

            // Parte decimal: float_const ::= digit+ "." digit+
            if (ch == '.') {
                isReal = true;
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

            return new Num(Double.parseDouble(sb.toString()), isReal ? Tag.REAL : Tag.NUM);
        }

        // Reconhecimento de Identificadores e Palavras Reservadas
        if (isLetter(ch) || ch == '_') {

            StringBuilder sb = new StringBuilder();
            
            do {
                sb.append(ch);
                readch();
            } while (isLetter(ch) || isDigit(ch) || ch == '_');

            // Corrigimos o commit anterior para voltar o case-sensitive. Estávamos olhando a especificação do trabalho de 2026-1 =)
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

            // Executa a análise léxica até o fim do arquivo, ignorando os tokens retornados. O objetivo é apenas validar o código fonte e gerar erros léxicos caso existam.
            while (lexer.scan() != null) {
            }

            System.out.println("Analise concluida com sucesso.");
        } catch (LexicalException e) {
            System.err.println("Erro lexico na linha " + e.getLine() + ": " + e.getMessage());
            System.exit(1);
        } catch (FileNotFoundException e) {
            System.err.println("Arquivo nao encontrado. Erro: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Erro de leitura no arquivo. Erro: " + e.getMessage());
        }
    }
}