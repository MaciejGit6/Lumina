package dev.lumina;

import dev.lumina.error.RuntimeError;

import java.util.HashMap;
import java.util.Map;

class Environment {
    // The enclosing scope — null only for the global environment.
    final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    // Global environment constructor
    Environment() {
        this.enclosing = null;
    }

    // Local scope constructor — always has a parent
    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    // Define a brand new variable in the current scope.
    // Redefining is allowed at the top level (useful in the REPL).
    void define(String name, Object value) {
        values.put(name, value);
    }

    // Look up a variable, walking up the scope chain if needed.
    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }
        if (enclosing != null) return enclosing.get(name);
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    // Assign to an existing variable — must already be defined somewhere
    // in the scope chain, otherwise it's an error.
    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    // Used by the resolver later — jump directly to the right scope depth
    // instead of walking the chain at runtime.
    Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme, value);
    }

    private Environment ancestor(int distance) {
        Environment env = this;
        for (int i = 0; i < distance; i++) {
            env = env.enclosing;
        }
        return env;
    }
}