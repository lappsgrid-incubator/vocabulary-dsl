package org.anc.lapps.vocab.dsl

import groovy.text.SimpleTemplateEngine
import groovy.text.Template

/**
 * @author Keith Suderman
 */
class GroovyTemplateEngine implements TemplateEngine {
    Template template

    public GroovyTemplateEngine(File file) {
        template = new SimpleTemplateEngine().createTemplate(file)
    }

    String generate(Map<String,ElementDelegate> index, ElementDelegate element) {
        // Create a list of element's ancestors in reverse order.
        List parents = []
        String parent = element.parent
        while (parent) {
            ElementDelegate parentElement = index[parent]
            parents.add(0, parentElement)
            parent = parentElement.parent
        }
        def binding = [elements:index, element:element, parents:parents]
        template.make(binding).toString()
    }
}
