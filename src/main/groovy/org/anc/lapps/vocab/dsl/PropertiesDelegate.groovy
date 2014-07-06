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
        if (args.size() != 2) {
            throw new MissingPropertyException("Property definitions require two parameters, the Class and a description.")
        }
        def theClass = args[0]
        String description = args[1].toString()

        String className
        if (theClass instanceof Class) {
            className = theClass.canonicalName
        }
        else if (theClass instanceof String) {
            className = theClass
        }
        else {
            throw new MissingMethodException("The first parameter must be a Java class name.")
        }

        properties[name] = new PropertyDelegate(type:className, description:description)
    }

//    def methodMissing(String name, args) {
//        if (args.size() != 1) {
//            throw MissingMethodException("Invalid arguments for property $name")
//        }
//        if (!(args[0] instanceof Closure)) {
//            throw UnsupportedOperationException("Missing closure for properties definition")
//        }
//
//        Closure cl = (Closure) args[0]
//        cl.delegate = new PropertyDelegate()
//        cl.resolveStrategy = Closure.DELEGATE_FIRST
//        cl()
//        properties[name] = cl.delegate
//    }
}
