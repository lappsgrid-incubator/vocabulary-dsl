package org.anc.lapps.vocab.dsl

import groovy.text.SimpleTemplateEngine
import groovy.text.Template

/**
 * The HtmlTemplateEngine uses Groovy's SimpleTemplateEngine class which uses
 * plain text templates with code included in <% ... %> blocks.  Groovy's
 * GString interpolation can also be used to insert values into the output.
 *
 * @author Keith Suderman
 */
class HtmlTemplateEngine implements TemplateEngine {
    Template template

    public HtmlTemplateEngine(File file) {
        template = new SimpleTemplateEngine().createTemplate(file)
    }

    String generate(Map<String,ElementDelegate> index, ElementDelegate element) {
        // Create a list of element's ancestors in reverse order. This is used to
        // generate the header at the top of the page.
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
