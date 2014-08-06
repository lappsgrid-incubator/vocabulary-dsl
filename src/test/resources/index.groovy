/*
 * This template is used to generate the index.html file which contains a
 * tree view of all the annotation types defined in the vocabulary DSL.
 *
 */

/*
                <div>
                    <img alt="" class="expand" src="Images/Minus.png" />
                    <img alt="" class="collapse" src="Images/Plus.png" />
                </div>

 */
// Starts an unordered list.
list = { name, closure ->
    builder.li {
        div {
            img  alt:"", class:"expand", src:"images/minus.png"
            img  alt:"", class:"collapse", src:"images/plus.png"
        }
        div {
            a(href:"${name}.html", name)
        }
        ul { closure() }
    }
}

// Starts a list item with no children.
item = { name ->
    builder.li {
        div {
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
        link rel:'stylesheet', type:'text/css', href:'tree.css'
        script src:'jquery-1.10.2.min.js'
        script {
            """
                var toggle = function(node) {
                    node.toggle();
                    node.next().toggle()
                    node.parent().parent().children().last().toggle()
                }
                \$(".expand").click(function () {
                    toggle(\$(this))
                });
                \$(".collapse").click(function () {
                    toggle(\$(this))
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