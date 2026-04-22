package dev.lumina;

import dev.lumina.ast.Expr;
import dev.lumina.ast.Stmt;
import dev.lumina.error.ParseError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dev.lumina.TokenType.*;

class Parser {
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    // -------------------------------------------------------------------------
    // Declarations
    // -------------------------------------------------------------------------

    private Stmt declaration() {
        try {
            if (match(CLASS))  return classDeclaration();
            if (match(FUN))    return function("function");
            if (match(VAR))    return varDeclaration();
            return statement();
        } catch (ParseError error) {
            // Panic-mode recovery: skip tokens until we're probably at a
            // statement boundary so we can keep reporting errors.
            synchronize();
            return null;
        }
    }

    private Stmt classDeclaration() {
        Token name = consume(IDENTIFIER, "Expected class name.");

        // Optional superclass: "class Foo < Bar { ... }"
        Expr.Variable superclass = null;
        if (match(LESS)) {
            consume(IDENTIFIER, "Expected superclass name.");
            superclass = new Expr.Variable(previous());
        }

        consume(LEFT_BRACE, "Expected '{' before class body.");

        List<Stmt.Function> methods = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"));
        }

        consume(RIGHT_BRACE, "Expected '}' after class body.");
        return new Stmt.Class(name, superclass, methods);
    }

    private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER, "Expected " + kind + " name.");
        consume(LEFT_PAREN, "Expected '(' after " + kind + " name.");

        List<Token> params = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (params.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }
                params.add(consume(IDENTIFIER, "Expected parameter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expected ')' after parameters.");

        consume(LEFT_BRACE, "Expected '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, params, body);
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expected variable name.");

        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
        }

        consume(SEMICOLON, "Expected ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    // -------------------------------------------------------------------------
    // Statements
    // -------------------------------------------------------------------------

    private Stmt statement() {
        if (match(FOR))    return forStatement();
        if (match(IF))     return ifStatement();
        if (match(PRINT))  return printStatement();
        if (match(RETURN)) return returnStatement();
        if (match(WHILE))  return whileStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        if (match(BREAK)) return breakStatement();
        return expressionStatement();
    }

    private Stmt breakStatement() {
        consume(SEMICOLON, "Expected ';' after 'break'.");
        return new Stmt.Break();
    }

    // for loops are desugared into while loops — simpler interpreter that way
    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expected '(' after 'for'.");

        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expected ';' after loop condition.");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expected ')' after for clauses.");

        Stmt body = statement();

        // Tack the increment onto the end of the body
        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
        }

        // Wrap in a while; missing condition means loop forever
        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        // Wrap in a block so the initializer is scoped to the loop
        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expected '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expected ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        Expr value = expression();
        consume(SEMICOLON, "Expected ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt returnStatement() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }
        consume(SEMICOLON, "Expected ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expected '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expected ')' after condition.");
        Stmt body = statement();
        return new Stmt.While(condition, body);
    }

    private Stmt expressionStatement() {
        Expr expr = expression();
        consume(SEMICOLON, "Expected ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(RIGHT_BRACE, "Expected '}' after block.");
        return statements;
    }

    // -------------------------------------------------------------------------
    // Expressions  (precedence climbing, lowest → highest)
    // -------------------------------------------------------------------------

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = or();

        // Plain assignment
        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            } else if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get) expr;
                return new Expr.Set(get.object, get.name, value);
            }
            error(equals, "Invalid assignment target.");
        }

        // Compound assignment — desugar to x = x OP rhs
        if (match(PLUS_EQUAL, MINUS_EQUAL, STAR_EQUAL, SLASH_EQUAL)) {
            Token op = previous();
            Expr rhs = assignment();

            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;

                // Map += to +, -= to -, etc.
                TokenType binaryOp = compoundToBinary(op.type);
                Token syntheticOp  = new Token(binaryOp, op.lexeme, null, op.line);

                Expr expanded = new Expr.Binary(expr, syntheticOp, rhs);
                return new Expr.Assign(name, expanded);
            }
            error(op, "Invalid compound assignment target.");
        }

        return expr;
    }

    // Helper used only by the compound-assignment branch above
    private TokenType compoundToBinary(TokenType compound) {
        switch (compound) {
            case PLUS_EQUAL:  return TokenType.PLUS;
            case MINUS_EQUAL: return TokenType.MINUS;
            case STAR_EQUAL:  return TokenType.STAR;
            case SLASH_EQUAL: return TokenType.SLASH;
            default: throw new IllegalArgumentException("Not a compound op: " + compound);
        }
    }

    private Expr or() {
        Expr expr = and();
        while (match(OR)) {
            Token op = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, op, right);
        }
        return expr;
    }

    private Expr and() {
        Expr expr = equality();
        while (match(AND)) {
            Token op = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, op, right);
        }
        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token op = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token op = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(MINUS, PLUS)) {
            Token op = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
        while (match(SLASH, STAR)) {
            Token op = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, op, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token op = previous();
            Expr right = unary();
            return new Expr.Unary(op, right);
        }
        return call();
    }

    private Expr call() {
        Expr expr = primary();

        // Keep consuming calls/gets as long as we see '(' or '.'
        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if (match(DOT)) {
                Token name = consume(IDENTIFIER, "Expected property name after '.'.");
                expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }

        return expr;
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments.");
                }
                arguments.add(expression());
            } while (match(COMMA));
        }

        Token paren = consume(RIGHT_PAREN, "Expected ')' after arguments.");
        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE))  return new Expr.Literal(true);
        if (match(NIL))   return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(SUPER)) {
            Token keyword = previous();
            consume(DOT, "Expected '.' after 'super'.");
            Token method = consume(IDENTIFIER, "Expected superclass method name.");
            return new Expr.Super(keyword, method);
        }

        if (match(THIS)) return new Expr.This(previous());

        if (match(IDENTIFIER)) return new Expr.Variable(previous());

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expected ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expected expression.");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() { return peek().type == EOF; }
    private Token peek()      { return tokens.get(current); }
    private Token previous()  { return tokens.get(current - 1); }

    private ParseError error(Token token, String message) {
        Lumina.error(token, message);
        return new ParseError();
    }

    // Skip tokens until we're probably at the start of the next statement.
    // This lets us report multiple errors in one pass.
    private void synchronize() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;
            switch (peek().type) {
                case CLASS: case FUN: case VAR: case FOR:
                case IF: case WHILE: case PRINT: case RETURN:
                    return;
                default:
                    break;
            }
            advance();
        }
    }
}