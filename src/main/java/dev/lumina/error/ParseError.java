package dev.lumina.error;

public class ParseError extends RuntimeException {
    // Used as a sentinel to unwind the parser call stack on a syntax error.
    // We catch it in declaration() and synchronize from there.
}