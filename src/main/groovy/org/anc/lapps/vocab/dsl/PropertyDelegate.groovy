package org.anc.lapps.vocab.dsl

/**
 * @author Keith Suderman
 */
class PropertyDelegate {
    String type
    String description

    def setProperty(String name, value) {
        throw new MissingPropertyException("Unknown property ${name}")
    }

    def setProperty(String name) {
        throw new MissingPropertyException("Unknown property ${name}")
    }

    void type(String type) {
        this.type = type
    }

    void description(String description) {
        this.description = description
    }
}
