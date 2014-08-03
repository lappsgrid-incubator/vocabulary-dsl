package org.anc.lapps.vocab.dsl

import groovy.xml.MarkupBuilder

/**
 * The MarkupBuilderTemplateEngine uses Groovy's MarkupBuilder class to generate
 * HTML.  The template is actually the MarkupBuilder DSL. The template is loaded
 * as a String and parsed by the GroovyShell class to create a closure that is run
 * to generate the HTML.
 *
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
