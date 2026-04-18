package dev.lumina;

import dev.lumina.ast.Stmt;
import dev.lumina.error.Return;

import java.util.List;

class LuminaFunction implements LuminaCallable {
    private final Stmt.Function declaration;
    private final Environment closure;
    private final boolean isInitializer;

    LuminaFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
        this.declaration   = declaration;
        this.closure       = closure;
        this.isInitializer = isInitializer;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        // Each call gets its own environment, parented to the closure where
        // the function was defined — this is what makes closures work.
        Environment env = new Environment(closure);
        for (int i = 0; i < declaration.params.size(); i++) {
            env.define(declaration.params.get(i).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, env);
        } catch (Return returnValue) {
            // "return;" inside an init still gives back 'this'
            if (isInitializer) return closure.getAt(0, "this");
            return returnValue.value;
        }

        // Initializers always return 'this' implicitly
        if (isInitializer) return closure.getAt(0, "this");
        return null;
    }

    // Creates a copy of this function bound to a specific instance —
    // used when you call a method on an object.
    LuminaFunction bind(LuminaInstance instance) {
        Environment env = new Environment(closure);
        env.define("this", instance);
        return new LuminaFunction(declaration, env, isInitializer);
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}