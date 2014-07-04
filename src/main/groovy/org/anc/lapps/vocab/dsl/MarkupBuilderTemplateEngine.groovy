package org.anc.lapps.vocab.dsl

import groovy.xml.MarkupBuilder

/**
 * @author Keith Suderman
 */
class MarkupBuilderTemplateEngine implements TemplateEngine {
    String template

    public MarkupBuilderTemplateEngine(File file) {
        this.template = file.text
    }

    String generate(Map<String,ElementDelegate> index, ElementDelegate element) {
        // Create a list of element's ancestors in reverse order.
        List parents = []
        String parent = element.parent
        while (parent) {
            ElementDelegate e = index[parent]
            parents.add(0, e)
            parent = e.parent
        }
//        println parents.join(" > ")
        StringWriter writer = new StringWriter()
        def html = new MarkupBuilder(writer)
        Binding binding = new Binding()
        binding.setVariable('element', element)
        binding.setVariable('elements', index)
        binding.setVariable('parents', parents)
        Closure closure = new GroovyShell(binding).evaluate( "{ it-> ${template} }" )
        closure.delegate = html
        closure()
        return writer.toString()
    }
}
