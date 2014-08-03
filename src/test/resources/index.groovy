/*
 * This template is used to generate the index.html file which contains a
 * tree view of all the annotation types defined in the vocabulary DSL.
 *
 */

// Define two closures that are used to simplify the list generation.
item = { name ->
    builder.li {
        span(class:'collapse') {
            a(href:"${name}.html", name)
        }
    }
}

list = { name, closure ->
    builder.li {
        span(class:'collapse') {
            a(href:"${name}.html", name)
        }
        ul { closure() }
    }
}

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

html {
    head {
        title 'LAPPS Vocabulary'
        //link rel:'stylesheet', type:'text/css', href:'lappsstyle.css'
        //script type:'text/javascript', src:'jquery-1.10.2.min.js'
    }
    body {
        h1 "LAPPS Vocabulary"
        roots.each { root ->
            ul {
                printNode(root)
            }
        }
        script(type:'text/javascript') {
            """
            \$(".collapse").click(function () {
                \$(this).parent().children().toggle();
                \$(this).toggle();

            });
"""
        }
    }
}