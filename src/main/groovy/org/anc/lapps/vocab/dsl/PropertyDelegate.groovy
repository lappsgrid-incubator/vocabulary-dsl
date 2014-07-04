package org.anc.lapps.vocab.dsl

/**
 * @author Keith Suderman
 */
class PropertyDelegate {
    String type
    String description

    def propertyMissing(String name, value) {
        throw new MissingPropertyException("Unknown property ${name}")
    }

    def propertyMissing(String name) {
        throw new MissingPropertyException("Unknown property ${name}")
    }

    void type(String type) {
        this.type = type
    }

    void description(String description) {
        this.description = description
    }
}
