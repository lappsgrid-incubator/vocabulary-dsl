/*
 * Copyright (c) 2019 The American National Corpus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.anc.lapps.vocab.dsl

import groovy.xml.MarkupBuilder

/**
 *
 */
class TemplateEngine {
    String template

    public TemplateEngine(File file) {
        this.template = file.text
    }

    public TemplateEngine(URL url) {
        this.template = url.text
    }

    public TemplateEngine(String template) {
        this.template = template
    }

    public String generate(Map params) {
        // Create a list of element's ancestors in reverse order.
        StringWriter writer = new StringWriter()
        def html = new MarkupBuilder(writer)
        Binding binding = new Binding(params)
        binding.setVariable('builder', html)
        Closure closure = new GroovyShell(binding).evaluate( "{ it-> ${template} }" )
        closure.delegate = html
        closure()
        return writer.toString()
    }
}
