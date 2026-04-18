package dev.lumina;

import java.util.List;

interface LuminaCallable {
    // How many arguments this callable expects.
    int arity();

    // Execute and return a value (nil if void).
    Object call(Interpreter interpreter, List<Object> arguments);
}