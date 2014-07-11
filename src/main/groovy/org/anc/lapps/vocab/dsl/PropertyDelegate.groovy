package org.anc.lapps.vocab.dsl

/**
 * @author Keith Suderman
 */
class PropertyDelegate {
    String type
    String description

    void setProperty(String name, value) {
        throw new MissingPropertyException("Unknown property ${name}")
    }

    void type(String type) {
        this.type = type
    }

    void description(String description) {
        this.description = description
    }
}
