package dev.lumina;

import java.util.List;
import java.util.Map;

class LuminaClass implements LuminaCallable {
    final String name;
    private final LuminaClass superclass;
    private final Map<String, LuminaFunction> methods;

    LuminaClass(String name, LuminaClass superclass, Map<String, LuminaFunction> methods) {
        this.name       = name;
        this.superclass = superclass;
        this.methods    = methods;
    }

    // Walk up the class hierarchy looking for a method by name.
    LuminaFunction findMethod(String name) {
        if (methods.containsKey(name)) return methods.get(name);
        if (superclass != null) return superclass.findMethod(name);
        return null;
    }

    // Calling a class constructs an instance and runs init() if it exists.
    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        LuminaInstance instance = new LuminaInstance(this);
        LuminaFunction initializer = findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }
        return instance;
    }

    // Arity comes from init() — zero if there's no constructor.
    @Override
    public int arity() {
        LuminaFunction initializer = findMethod("init");
        if (initializer == null) return 0;
        return initializer.arity();
    }

    @Override
    public String toString() {
        return "<class " + name + ">";
    }
}