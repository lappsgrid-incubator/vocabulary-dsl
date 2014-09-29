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
            a(href:"${node.name}.html", node.name)
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
        title 'LAPPS Vocabulary'
        link rel:'stylesheet', type:'text/css', href:'tree.css'
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
            div(id:'mainContent') {
                h1 "LAPPS Vocabulary"
                p "These are the annotation types defined in the LAPPS vocabulary."
//                p """Note that LAPPS does not define any types of its own.  The LAPPS
//                    vocabulary simply enumerates the URI of all types used by LAPPS
//                    services. LAPPS services may use other types with different URI and
//                    different types.  However, all LAPPS services SHOULD recognize at least
//                    the annotation types listed here."""
                p "TODO: The RFC that defines SHOULD should be listed here."
                roots.each { root ->
                    ul(class:'tree') {
                        printNode(root)
                    }
                }
            }
        }

    }
}