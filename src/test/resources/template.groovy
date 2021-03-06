/*
 * This template expects three parameters to be passed to it:
 *
 * 1. element : the ElementDelegate for the page being generated.
 * 2. elements: a HashMap used to map element names to their ElementDelegate object.
 * 3. parents: the names all parents of this element.
 * 4. version: the current vocabulary version.
 */
html {
    head {
        title element.name
        link rel:'stylesheet', type:'text/css', href:'lappsstyle.css'
    }
    body {
        div(id:'container') {
            div(id:'intro') {
                div(id:'pageHeader') {
                    h1 "LAPPS Web Service Exchange Vocabulary"
                    h2 "Version $version"
                }
            }
            div(id:'mainContent') {
                div(id:'sectionbar') {
                    p {
                        a(href:'index.html', 'Home')
                    }
                }
                p(class:'head') {
                    parents.each { parent ->
                        a(href:"${parent.name}.html", parent.name)
                        span ' > '
                    }
                    span element.name
                }
                br()
                table {
                    tr {
                        td(class:'fixed') { b 'Definition' }
                        td element.definition
                    }
                    if (element.sameAs.size() > 0) {
                        tr {
                            td { b "Same as" }
                            td {
                                element.sameAs.collect { a(href:it, it) }.join(", ")
                            }
                        }
                    }
                    if (element.similarTo.size() > 0) {
                        tr {
                            td { b "Similar to" }
                            td {
                                element.similarTo.collect { a(href:it, it) }.join(", ")
                            }
                        }
                    }
                    tr {
                        td { b "URI" }
                        td element.uri
                    }
                    if (element.deprecated) {
                        tr {
                            td { b "Deprecated"}
                            td element.deprecated
                        }
                    }
                }

                // Print the metadata properties for this and all parent classes.
                def node = element
                boolean headline = true
                while (node) {
                    if (node.metadata.size() > 0) {
                        if (headline) {
                            // The headline only gets printed if there are metadata attributes defined.
                            h1 "Metadata"
                            headline = false
                        }
                        if (node.name != element.name) {
                            h2 {
                                span "Metadata from "
                                a(href: "${node.name}.html", node.name)
                            }
                        }
                        // Put the metadata properties in a table.
                        table(class: 'definition-table') {
                            tr {
                                th class:'fixed', "Properties"
                                th class:'fixed', "Type"
                                th "Description"
                            }
                            List names = node.metadata.keySet().asList()
                            names.each { name ->
                                tr {
                                    def property = node.metadata[name]
                                    td name
                                    td property.type
                                    td {
                                        mkp.yieldUnescaped property.description
                                    }
                                }
                            }
                        }
                    }
                    node = elements[node.parent]
                }

                // Print the properties for this and all parent classes.
                headline = true
                node = element
                while (node) {
                    if (node.properties.size() > 0) {
                        if (headline) {
                            h1 'Properties'
                            headline = false
                        }
                        if (node.name != element.name) {
                            h2 {
                                span "Properties from "
                                a(href:"${node.name}.html", node.name)
                            }
                        }
                        table(class: 'definition-table') {
                            tr {
                                th class:'fixed', "Properties"
                                th class:'fixed', "Type"
                                th "Description"
                            }
                            List names = node.properties.keySet().asList()
                            names.each { name ->
                                tr {
                                    def property = node.properties[name]
                                    td name
                                    td property.type
                                    td {
                                        mkp.yieldUnescaped property.description
                                    }
                                }
                            }
                        }
                    }
                    node = elements[node.parent]
                }
                br()
                div(class:'index') {
                    span "Back to the "
                    a(href:'index.html', 'hierarchy')
                }
            }
        }
        div(id:'footer', 'Copyright 2015 The Language Application Grid.')
    }
}
/**
 * Created by Nathan on 8/1/2014.
 */
