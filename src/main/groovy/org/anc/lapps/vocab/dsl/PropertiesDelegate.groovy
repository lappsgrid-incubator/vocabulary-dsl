package org.anc.lapps.vocab.dsl

/**
 * @author Keith Suderman
 */
class PropertiesDelegate {
    //Map<String,PropertyDelegate> properties = [:]
    Map properties

    public PropertiesDelegate(Map properties) {
        this.properties = properties
    }

    def propertyMissing(String name, value) {
        throw new MissingPropertyException("Unknown property ${name}")
    }

    def propertyMissing(String name) {
        throw new MissingPropertyException("Unknown property ${name}")
    }

    def methodMissing(String name, args) {
        if (args.size() != 1) {
            throw MissingMethodException("Invalid arguments for property $name")
        }
        if (!(args[0] instanceof Closure)) {
            throw UnsupportedOperationException("Missing closure for properties definition")
        }

        Closure cl = (Closure) args[0]
        cl.delegate = new PropertyDelegate()
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
        properties[name] = cl.delegate
    }
}
