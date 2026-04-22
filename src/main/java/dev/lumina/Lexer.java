package dev.lumina;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.lumina.TokenType.*;

class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private int start   = 0;
    private int current = 0;
    private int line    = 1;

    private void blockComment() {
        while (!isAtEnd()) {
            if (peek() == '\n') line++;
            if (peek() == '*' && peekNext() == '/') {
                advance(); // consume '*'
                advance(); // consume '/'
                return;
            }
            advance();
        }
        // If we fall through here, the comment was never closed
        Lumina.error(line, "Unterminated block comment.");
    }

    // All reserved words. Anything not here is just a plain identifier.
    private static final Map<String, TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("and",    AND);
        keywords.put("class",  CLASS);
        keywords.put("else",   ELSE);
        keywords.put("false",  FALSE);
        keywords.put("for",    FOR);
        keywords.put("fun",    FUN);
        keywords.put("if",     IF);
        keywords.put("nil",    NIL);
        keywords.put("or",     OR);
        keywords.put("print",  PRINT);
        keywords.put("return", RETURN);
        keywords.put("super",  SUPER);
        keywords.put("this",   THIS);
        keywords.put("true",   TRUE);
        keywords.put("var",    VAR);
        keywords.put("while",  WHILE);
        keywords.put("break", BREAK);
    }

    Lexer(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
            // Single-char tokens
            case '(': addToken(LEFT_PAREN);  break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE);  break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA);       break;
            case '.': addToken(DOT);         break;
            case '-': addToken(MINUS);       break;
            case '+': addToken(PLUS);        break;
            case ';': addToken(SEMICOLON);   break;
            case '*': addToken(STAR);        break;
            case '%': addToken(PERCENT); break;

            // One or two char tokens
            case '!': addToken(match('=') ? BANG_EQUAL    : BANG);    break;
            case '=': addToken(match('=') ? EQUAL_EQUAL   : EQUAL);   break;
            case '<': addToken(match('=') ? LESS_EQUAL    : LESS);    break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;

           
            case '/':
                if (match('/')) {
                    // Single-line comment; eat until end of line
                    while (peek() != '\n' && !isAtEnd()) advance();
                } else if (match('*')) {
                    // Block comment — scan until we find */
                    blockComment();
                } else {
                    addToken(SLASH);
                }
                break;
        
            case ' ':
            case '\r':
            case '\t':
                break;

            case '\n':
                line++;
                break;

            // String literals
            case '"': string(); break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    // We still keep scanning after an error so we catch
                    // all problems in one pass rather than stopping at the first one
                    Lumina.error((int) line, "Unexpected character: " + c);
                }
                break;
        }
    }


    private void string() {
        StringBuilder value = new StringBuilder();

        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;

            if (peek() == '\\') {
                advance(); // consume the backslash
                switch (peek()) {
                    case 'n':  value.append('\n'); break;
                    case 't':  value.append('\t'); break;
                    case 'r':  value.append('\r'); break;
                    case '\\': value.append('\\'); break;
                    case '"':  value.append('"');  break;
                    default:
                        Lumina.error(line, "Unknown escape sequence: \\" + peek());
                        value.append(peek()); // recover gracefully
                }
                advance(); // consume the escaped char
            } else {
                value.append(advance());
            }
        }

        if (isAtEnd()) {
            Lumina.error(line, "Unterminated string.");
            return;
        }

        advance(); // closing "
        addToken(STRING, value.toString());
    }

    private void number() {
        while (isDigit(peek())) advance();

  
        if (peek() == '.' && isDigit(peekNext())) {
            advance(); // consume the '.'
            while (isDigit(peek())) advance();
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.getOrDefault(text, IDENTIFIER);
        addToken(type);
    }

    private char advance() {
        return source.charAt(current++);
    }

    
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        return true;
    }

   
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    // Two characters of lookahead — only needed for numbers 1.5 vs 1
    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAtEnd()            { return current >= source.length(); }
    private boolean isDigit(char c)      { return c >= '0' && c <= '9'; }
    private boolean isAlpha(char c)      { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'; }
    private boolean isAlphaNumeric(char c){ return isAlpha(c) || isDigit(c); }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}