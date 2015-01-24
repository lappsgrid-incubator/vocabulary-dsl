/*
 * This template is used to generate the index.html file which contains a
 * tree view of all the annotation types defined in the vocabulary DSL.
 *
 */

printTable = { node ->
    builder.table(class:'h') {
        tr {
            td(class:'tc', colspan:4) {
                if (node.properties) {
                    mkp.yieldUnescaped "<a href='${node.name}.html'>${node.name}</a>: <span>${node.properties}</span>"
                }
                else {
                    a(href:"${node.name}.html", node.name)
                }
            }
        }
        if (node.children.size() > 0) {
            node.children.each { child ->
                tr {
                    td(class:'space','')
                    td(class:'bar','')
                    td(class:'space','')
                    td {
                        printTable(child)
                    }
                }
            }
        }
    }
}

// The start of the HTML template.
html {
    head {
        title 'LAPPS Type Hierarchy'
        link rel:'stylesheet', type:'text/css', href:'lappsstyle.css'
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
            br()
            h1 "LAPPS Exchange Vocabulary Type Hierarchy"
            roots.each { root ->
                br()
                printTable(root)
            }
            br()
            br()
        }
    }
}