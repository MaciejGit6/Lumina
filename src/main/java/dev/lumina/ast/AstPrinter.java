package dev.lumina.ast;

// Walks the expression tree and produces a parenthesized string — useful
// for debugging. Only covers Expr (statements aren't printed this way).
//
// Example: (1 + 2) * -3  =>  (* (group (+ 1.0 2.0)) (- 3.0))
public class AstPrinter implements Expr.Visitor<String> {

    public String print(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public String visitAssignExpr(Expr.Assign expr) {
        return parenthesize("= " + expr.name.lexeme, expr.value);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitCallExpr(Expr.Call expr) {
        // callee followed by all arguments
        Expr[] args = expr.arguments.toArray(new Expr[0]);
        Expr[] all  = new Expr[args.length + 1];
        all[0] = expr.callee;
        System.arraycopy(args, 0, all, 1, args.length);
        return parenthesize("call", all);
    }

    @Override
    public String visitGetExpr(Expr.Get expr) {
        return parenthesize("get " + expr.name.lexeme, expr.object);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if (expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitLogicalExpr(Expr.Logical expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitSetExpr(Expr.Set expr) {
        return parenthesize("set " + expr.name.lexeme, expr.object, expr.value);
    }

    @Override
    public String visitSuperExpr(Expr.Super expr) {
        return "(super " + expr.method.lexeme + ")";
    }

    @Override
    public String visitThisExpr(Expr.This expr) {
        return "this";
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitVariableExpr(Expr.Variable expr) {
        return expr.name.lexeme;
    }

    // Wraps an operator and its operands in parens: (op a b ...)
    private String parenthesize(String name, Expr... exprs) {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(name);
        for (Expr expr : exprs) {
            sb.append(" ").append(expr.accept(this));
        }
        sb.append(")");
        return sb.toString();
    }
}