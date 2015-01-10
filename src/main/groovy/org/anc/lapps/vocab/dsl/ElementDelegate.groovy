package org.anc.lapps.vocab.dsl

/**
 * @author Keith Suderman
 */
class ElementDelegate {
    String name
    String definition
    String parent
    List<String> sameAs = []
    List<String> similarTo = []
    String uri
    Map properties = [:]
    Map metadata = [:]

    def propertyMissing(String name, value) {
        throw new MissingPropertyException("Unknown property ${name}")
    }

    def propertyMissing(String name) {
        throw new MissingPropertyException("Unknown property ${name}")
    }

    def name(String name) {
        this.name = name
    }

    void parent(String parent) {
        this.parent = parent
    }

    void definition(String definition) {
        this.definition = definition
    }

    void sameAs(String... args) {
        args.each { sameAs << it }
    }

    void similarTo(String... args) {
        args.each { similarTo << it }
    }

    void uri(String uri) {
        this.uri = uri
    }

    void metadata(Closure cl) {
        cl.delegate = new MetadataDelegate(metadata)
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
    }

    void properties(Closure cl) {
        cl.delegate = new PropertiesDelegate(properties)
        cl.resolveStrategy = Closure.DELEGATE_FIRST
        cl()
    }

    void print() {
        print(System.out)
    }

    void print(PrintStream writer) {
        writer.println "Element   : $name"
        if (parent) {
            writer.println "Parent    : $parent"
        }
        writer.println "Definition: $definition"
        writer.println "Same as   : ${sameAs.join(", ")}"
        writer.println "URI       : $uri"
        writer.println "Properties"
        properties.each { name, value ->
            writer.println "\t$name {"
            writer.println "\t\ttype: ${value.type}"
            writer.println "\t\tdescription: ${value.description}"
            writer.println "\t}"
        }
        writer.println()
    }
}
