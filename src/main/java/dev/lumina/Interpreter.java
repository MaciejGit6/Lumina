package dev.lumina;

import dev.lumina.ast.Expr;
import dev.lumina.ast.Stmt;
import dev.lumina.error.RuntimeError;

import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    // Entry point — runs a full program (list of statements).
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt stmt : statements) {
                execute(stmt);
            }
        } catch (RuntimeError error) {
            Lumina.runtimeError(error);
        }
    }

    // -------------------------------------------------------------------------
    // Statement visitors
    // -------------------------------------------------------------------------

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    // Stubs for the rest — we'll fill these in over the next commits.
    @Override public Void visitBlockStmt(Stmt.Block stmt)         { throw new UnsupportedOperationException("blocks not yet implemented"); }
    @Override public Void visitClassStmt(Stmt.Class stmt)         { throw new UnsupportedOperationException("classes not yet implemented"); }
    @Override public Void visitFunctionStmt(Stmt.Function stmt)   { throw new UnsupportedOperationException("functions not yet implemented"); }
    @Override public Void visitIfStmt(Stmt.If stmt)               { throw new UnsupportedOperationException("if not yet implemented"); }
    @Override public Void visitReturnStmt(Stmt.Return stmt)       { throw new UnsupportedOperationException("return not yet implemented"); }
    @Override public Void visitVarStmt(Stmt.Var stmt)             { throw new UnsupportedOperationException("var not yet implemented"); }
    @Override public Void visitWhileStmt(Stmt.While stmt)         { throw new UnsupportedOperationException("while not yet implemented"); }

    // -------------------------------------------------------------------------
    // Expression visitors
    // -------------------------------------------------------------------------

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:  return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;
            default:
                break;
        }
        return null;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left  = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            // Arithmetic
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if ((double) right == 0) throw new RuntimeError(expr.operator, "Division by zero.");
                return (double) left / (double) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            case PLUS:
                // + works for both numbers and strings
                if (left instanceof Double && right instanceof Double)
                    return (double) left + (double) right;
                if (left instanceof String && right instanceof String)
                    return (String) left + (String) right;
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");

            // Comparison
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;

            // Equality — works on any type, no coercion
            case BANG_EQUAL:  return !isEqual(left, right);
            case EQUAL_EQUAL: return  isEqual(left, right);

            default: break;
        }
        return null;
    }

    // Stubs for expressions that need the environment (next commit).
    @Override public Object visitAssignExpr(Expr.Assign expr)   { throw new UnsupportedOperationException("assign not yet implemented"); }
    @Override public Object visitCallExpr(Expr.Call expr)       { throw new UnsupportedOperationException("call not yet implemented"); }
    @Override public Object visitGetExpr(Expr.Get expr)         { throw new UnsupportedOperationException("get not yet implemented"); }
    @Override public Object visitLogicalExpr(Expr.Logical expr) { throw new UnsupportedOperationException("logical not yet implemented"); }
    @Override public Object visitSetExpr(Expr.Set expr)         { throw new UnsupportedOperationException("set not yet implemented"); }
    @Override public Object visitSuperExpr(Expr.Super expr)     { throw new UnsupportedOperationException("super not yet implemented"); }
    @Override public Object visitThisExpr(Expr.This expr)       { throw new UnsupportedOperationException("this not yet implemented"); }
    @Override public Object visitVariableExpr(Expr.Variable expr){ throw new UnsupportedOperationException("variable not yet implemented"); }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    // nil and false are falsy; everything else is truthy.
    private boolean isTruthy(Object object) {
        if (object == null)           return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null)              return false;
        return a.equals(b);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    // Converts a runtime value back to a Lumina-style string.
    // Strips the trailing ".0" from whole numbers so "2.0" prints as "2".
    String stringify(Object object) {
        if (object == null) return "nil";
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) text = text.substring(0, text.length() - 2);
            return text;
        }
        return object.toString();
    }
}