package org.anc.lapps.vocab.dsl

/**
 * @author Keith Suderman
 */
class ScriptDelegate {
    def propertyMissing(String name, value) {
        throw new MissingPropertyException("Unknown property ${name}")
    }

    def propertyMissing(String name) {
        throw new MissingPropertyException("Unknown property ${name}")
    }

    def methodMissing(String name, args) {
        if (args.size() != 2) {
            throw new MissingMethodException("Invalid number of arguments for ${name}")
        }
        // We could test for ClassCastException, but if we did we would just throw
        // ClassCastExceptions...
        //Class theClass = (Class) args[0]
        String description = (String) args[0]
        Closure cl = (Closure) args[1]

        //TODO We should test if theClass is String.class or Integer.class

        ElementDelegate element = new ElementDelegate()
        element.name = name
        element.definition = description
        cl.delegate = element

    }
}
