package org.anc.lapps.vocab.dsl

/**
 * @author Keith Suderman
 */
class PropertyDelegate {
    String type
    String description
    boolean required = false;
    boolean requiredSet = false;

    void setProperty(String name, value) {
        throw new MissingPropertyException("Unknown property ${name}")
    }

    void type(String type) {
        this.type = type
    }

    void description(String description) {
        this.description = description
    }

    void required(boolean required) {
        if (requiredSet && this.required != required) {
            throw new VocabularyException("The required property has already been set.")
        }
        requiredSet = true
        this.required = required
    }

    void optional(boolean optional) {
        required(!optional)
    }
}
