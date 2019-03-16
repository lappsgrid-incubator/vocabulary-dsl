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

/**
 *
 */
class DatatypeDelegate {

    Node root

    DatatypeDelegate() {
        root = new Node(null, "xs:schema")
        root.attributes().with {
            put("xmlns:xs", "http://www.w3.org/2001/XMLSchema")
            put("attributeFormDefault", "unqualified")
            put("elementFormDefault", "qualified")
        }
    }

    void simple(String name, Closure body) {
        XmlDelegate delegate = new XmlDelegate(root,"xs:simpleType")
        body.delegate = delegate
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body()
        Node node = delegate.node
        node.attributes().with {
            put("name", name)
            put("id", name)
        }
    }

    void complex(String name, Closure body) {
        XmlDelegate delegate = new XmlDelegate(root, "xs:complexType")
        body.delegate = delegate
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body()
        Node node = delegate.node
        node.attributes().with {
            put("name", name)
            put("id", name)
        }
    }
}

class XmlDelegate {
    Node node

    XmlDelegate(Node parent, String name) {
        node = new Node(parent, name)
    }

    void methodMissing(String name, args) {
        String qName = "xs:$name"
        if (args[0] instanceof Closure) {
            Closure cl = (Closure) args[0]
            cl.delegate = new XmlDelegate(node, qName)
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
        }
        else if (args[0] instanceof Map) {
            Node child = new Node(node, qName)
            Map atts = child.attributes()
            ((Map)args[0]).each { k,v ->
                atts[k] = v
            }
        }
        else {
            println "XmlDelegate method : $name args: ${args}"

        }
    }
}
