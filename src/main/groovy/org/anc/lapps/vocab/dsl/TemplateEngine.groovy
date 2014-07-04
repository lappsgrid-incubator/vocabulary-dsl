package org.anc.lapps.vocab.dsl

/**
 * @author Keith Suderman
 */
interface TemplateEngine {
    String generate(Map<String,ElementDelegate> elements, ElementDelegate element)
}
