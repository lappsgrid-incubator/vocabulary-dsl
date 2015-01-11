/*
 * This template is used to generate the index.html file which contains a
 * tree view of all the annotation types defined in the vocabulary DSL.
 *
 */

// Starts an unordered list inside another list item.
list = { name, closure ->
    builder.li {
        span(class:'collapse') {
            img  src:"images/minus.png"
            img  class:"hidden", src:"images/plus.png"
        }
        a(href:"${name}.html", name)
        ul(class:'tree') {
            closure()
        }
    }
}

// Starts a list item with no children.
item = { name ->
    builder.li {
        a(href:"${name}.html", name)
    }
}

// Recursively prints a node and all its children.
printNode = { node ->
    if (node.children.size() == 0) {
//        item(node.name)
        builder.li {
//            def properties
            if (node.properties) {
                mkp.yieldUnescaped "<a href='${node.name}.html'>${node.name}</a>: <span class='property'>${node.properties}</span>"
            }
            else {
                a(href:"${node.name}.html", node.name)
            }
//            a(href:"${node.name}.html", node.name)
//            if (node.properties) {
//                span(class:'property', ": " + node.properties)
//            }
        }
    }
    else {
        list(node.name) {
            node.children.each { printNode it }
        }
    }
}

// The start of the HTML template.
html {
    head {
        title 'LAPPS Type Hierarchy'
        link rel:'stylesheet', type:'text/css', href:'lappsstyle.css'
        //link rel:'stylesheet', type:'text/css', href:'tree.css'
        script(src:'js/jquery-1.11.1.js', type:'text/javascript', language:'javascript', "")
        script (type:'text/javascript', """
            \$(document).ready(function() {
                \$(".collapse").click(function () {
                    \$(this).next().next().toggle();
                    \$(this).children().toggle();
                });
            });
        """)
    }
    body {
        div(id:'container') {
            div(id: 'intro') {
                div(id: 'pageHeader') {
                    h1 "LAPPS Web Service Exchange Vocabulary"
                }
            }
        }
        div(id:'mainContent') {
            /****
            p "These are the annotation types defined in the LAPPS vocabulary."
            p """Note that LAPPS does not define any types of its own.  The LAPPS
                vocabulary simply enumerates the URI of all types used by LAPPS
                services. LAPPS services may use other types with different URI and
                different types.  However, all LAPPS services SHOULD recognize at least
                the annotation types listed here."""
            ****/
            p """The LAPPS Web Service Exchange Vocabulary defines an ontology of
                 terms for a core of linguistic objects and features exchanged among
                 NLP tools that consume and produce linguistically annotated data.
                 It is intended to be used for module description and input/output
                 interchange to support service discovery, composition, and reuse in
                 the natural language processing domain."""

            p """The Exchange Vocabulary is being developed bottom-up on an as-needed
                 basis for use in the development of the LAPPS Grid. The Type
                 Hierarchy below contains all of the elements for which specifications
                 have been finalized so far. Detailed information is available by
                 clicking on the relevant element."""
            //p "TODO: The RFC that defines SHOULD should be listed here."
//                p """Note that LAPPS does not define any types of its own.  The LAPPS
//                    vocabulary simply enumerates the URI of all types used by LAPPS
//                    services. LAPPS services may use other types with different URI and
//                    different types.  However, all LAPPS services SHOULD recognize at least
//                    the annotation types listed here."""
            roots.each { root ->
                ul(class:'tree') {
                    printNode(root)
                }
            }
        }
    }
}