/*
 * This template is used to generate the index.html file which contains a
 * tree view of all the annotation types defined in the vocabulary DSL.
 *
 */

// Starts an unordered list.
list = { name, closure ->
    builder.li {
        span {
            img  alt:"", class:"expand", src:"images/minus.png"
            img  alt:"", class:"collapse", src:"images/plus.png"
        }
        span {
            a(href:"${name}.html", name)
        }
        ul { closure() }
    }
}

// Starts a list item with no children.
item = { name ->
    builder.li {
        span {
            a(href:"${name}.html", name)
        }
    }
}

// Recursively prints a node and all its children.
printNode = { node ->
    if (node.children.size() == 0) {
        item(node.name)
    }
    else {
        list(node.name) {
            node.children.sort{ it.name }.each { printNode it }
        }
    }
}

// The start of the HTML template.
html {
    head {
        title 'LAPPS Vocabulary'
        link rel:'stylesheet', type:'text/css', href:'lappsstyle.css'
        script src:'jquery-1.10.2.min.js'
        script {
            """
                \$(".expand").click(function () {
                    \$(this).toggle();
                    \$(this).next().toggle();
                    \$(this).parent().parent().children().last().toggle();
                });
                \$(".collapse").click(function () {
                    \$(this).toggle();
                    \$(this).next().toggle();
                    \$(this).parent().parent().children().last().toggle();
                });
"""
        }
    }
    body {
        div(id:'container') {
            div(id:'mainContent') {
                h1 "LAPPS Vocabulary"
                roots.each { root ->
                    ul {
                        printNode(root)
                    }
                }
            }
        }

    }
}