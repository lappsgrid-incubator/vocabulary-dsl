package org.anc.lapps.vocab.dsl

import org.anc.template.HtmlTemplateEngine
import org.anc.template.MarkupBuilderTemplateEngine
import org.apache.jena.ontology.AnnotationProperty
import org.apache.jena.ontology.DatatypeProperty
import org.apache.jena.ontology.OntClass
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Property
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFFormat
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.anc.template.TemplateEngine

/**
 * @author Keith Suderman
 */
class VocabDsl {
    static final String VOCAB = 'http://vocab.lappsgrid.org/'
    static final String EXTENSION = ".vocab"
    String FILE_TEMPLATE = "src/test/resources/template.groovy"
    String INDEX_TEMPLATE = "src/test/resources/index.groovy"

    // Selects the templating engine to use.  Choices are the MarkupBuilderTemplateEngine
    // or HtmlTemplateEngine. The latter uses a template that looks like HTML while the
    // former uses the MarkupBuilder DSL as the template language.
//    static boolean USE_MARKUPBUILDER = true

    Set<String> included = new HashSet<String>()
    File parentDir
    File destination
    Binding bindings = new Binding()
    List<ElementDelegate> elements = []
    Map<String, ElementDelegate> elementIndex = [:]

    // Ontology used when generating RDF/OWL.  Instances are created and
    // destroyed as needed.
    OntModel ontology

    void run(File file, File destination) {
        parentDir = file.parentFile
        run(file.text, destination)
    }

    ClassLoader getLoader() {
        ClassLoader loader = Thread.currentThread().contextClassLoader;
        if (loader == null) {
            loader = this.class.classLoader
        }
        return loader
    }

    CompilerConfiguration getCompilerConfiguration() {
        ImportCustomizer customizer = new ImportCustomizer()
        /*
         * Custom imports can be defined in the ImportCustomizer.
         * For example:
         *   customizer.addImport("org.anc.xml.Parser")
         *   customizer.addStarImports("org.anc.util")
         *
         * The jar files for any packages imported this way must be
         * declared as Maven dependencies so they will be available
         * at runtime.
         */

        CompilerConfiguration configuration = new CompilerConfiguration()
        configuration.addCompilationCustomizers(customizer)
        return configuration
    }

    void run(String scriptString, File destination) {
        this.destination = destination
        compile(scriptString)
        try {
            // Now generate the HTML.
            makeHtml()
            makeIndexHtml()
        }
        catch (Exception e) {
            println()
            println "Script execution threw an exception:"
            e.printStackTrace()
            println()
        }
    }

    void compile(String scriptString) {
        ClassLoader loader = getLoader()
        CompilerConfiguration configuration = getCompilerConfiguration()
        GroovyShell shell = new GroovyShell(loader, bindings, configuration)

        Script script = shell.parse(scriptString)
        script.binding.setVariable("args", [:])
        script.metaClass = getMetaClass(script.class, shell)
        script.run()
    }

    void dump(File file) {
        compile(file.text)
        elements.each { it.print() }
    }

    void makeHtml() {
        // Create the template engine that will generate the HTML.
        TemplateEngine engine = new MarkupBuilderTemplateEngine(new File(FILE_TEMPLATE))
        elements.each { element ->
            // Walk up the hierarchy and record the names of
            // all ancestors.
            List parents = []
            String parent = element.parent
            while (parent) {
                ElementDelegate delegate = elementIndex[parent]
                parents.add(0, delegate)
                parent = delegate.parent
            }
            // params is the data model to be passed to the template
            def params = [ element:element, elements:elementIndex, parents:parents ]
            // file is where the generated HTML will be written.
            File file = new File(destination, "${element.name}.html")
            // Call the template to generate the HTML from the model and
            // write it to the file.
            file.text = engine.generate(params)
            println "Wrote ${file.path}"
        }
    }

    DatatypeProperty makeProperty(String annotation, String name) {
        return ontology.createDatatypeProperty("${VOCAB}${annotation}#${name}")
    }

    AnnotationProperty makeMetadata(String name) {
        return ontology.createAnnotationProperty("${VOCAB}metadata#${name}")
    }

    void makeOwl(File script) {
        makeOwl(script, RDFFormat.JSONLD_PRETTY)
    }

    void makeOwl(File script, RDFFormat format) {
        compile(script.text)

//        Property similarTo = makeProperty('similarTo')

        Map<String,OntClass> classes = [:]
        Map<String,Property> properties = [:]
        ontology = ModelFactory.createOntologyModel()
        elements.each { ElementDelegate element ->
            println "Processing ${element.name}"
            OntClass theClass = classes[element.name]
            if (theClass) {
                throw new VocabularyException("Duplicate element : ${element.name}")
            }
            theClass = ontology.createClass(VOCAB + element.name)
            classes[element.name] = theClass
            theClass.addComment(ontology.createLiteral(element.definition))

            // Set the parent node.
            // TODO: It should likely be an error if the element does not have a
            // parent. This means 'Thing' would have to have something like
            // OWL.TOP as its parent.
            if (element.parent) {
                OntClass parent = classes[element.parent]
                if (!parent) {
                    throw new VocabularyException("Undefined parent class: ${element.parent}")
                }
                theClass.setSuperClass(parent)
            }
            element.sameAs.each { resource ->
                println "Same as $resource"
                theClass.addSameAs(ontology.createResource(resource))
            }
            element.metadata.each { String key, PropertyDelegate value ->
                println "metadata $key -> ${value.type}"
                Property property = makeMetadata(key)
                property.addComment(ontology.createLiteral(value.description))
                theClass.addProperty(property, value.type)
            }
            element.properties.each { String name, PropertyDelegate value ->
                println "property $name -> ${value.type}"
                Property property = makeProperty(element.name, name)
                property.addComment(ontology.createLiteral(value.description))
                theClass.addProperty(property, value.type)
            }
//            element.similarTo.each { resource ->
//            }

        }

        String ext = format.lang.fileExtensions[0] ?: 'xml'
        File file = new File("target/lapps-vocabulary.${ext}")
        RDFDataMgr.write(new FileOutputStream(file), ontology, format)
        println "Wrote ${file.path}"
    }

    MetaClass getMetaClass(Class<?> theClass, GroovyShell shell) {
        ExpandoMetaClass meta = new ExpandoMetaClass(theClass, false)
        meta.include = { String filename ->
            // Make sure we can find the file. The default behaviour is to
            // look in the same directory as the source script.
            // TODO Allow an absolute path to be specified.

            def filemaker
            if (parentDir != null) {
                filemaker = { String name ->
                    return new File(parentDir, name)
                }
            }
            else {
                filemaker = { String name ->
                    new File(name)
                }
            }

            File file = filemaker(filename)
            if (!file.exists() || file.isDirectory()) {
                file = filemaker(filename + EXTENSION)
                if (!file.exists()) {
                    throw new FileNotFoundException(filename)
                }
            }
            // Don't include the same file multiple times.
            if (included.contains(filename)) {
                return
            }
            included.add(filename)


            // Parse and run the script.
            Script included = shell.parse(file)
            included.metaClass = getMetaClass(included.class, shell)
            included.run()
        }

        meta.element = { Closure cl ->
            ElementDelegate element = new ElementDelegate()
            cl.delegate = element
            cl.resolveStrategy = Closure.DELEGATE_FIRST
            cl()
            elements << element
            elementIndex[element.name] = element
        }

        meta.initialize()
        return meta
    }


    void makeIndexHtml() {
        File file = new File(INDEX_TEMPLATE)
        if (!file.exists()) {
            throw new FileNotFoundException("Unable to find the index.groovy template.")
        }
        TemplateEngine template = new MarkupBuilderTemplateEngine(file)
        String html = template.generate(roots: getTrees())
        File destination = new File(destination, 'index.html')
        destination.text = html
        println "Wrote ${destination.path}"
    }

    List<TreeNode> getTrees() {
        List<TreeNode> roots = []
        elements.each { ElementDelegate element ->
            TreeNode elementNode = TreeNode.get(element)
            if (element.parent) {
                TreeNode parent = TreeNode.get(element.parent)
                parent.children << elementNode
            }
            else {
                roots << elementNode
            }
        }
        return roots
    }

    void makeJava(File scriptFile, String packageName, String className) {
        compile(scriptFile.text)

        File outFile = new File("target/${className}.java")
        outFile.withPrintWriter { PrintWriter out ->
            out.println """
/*
 * DO NOT EDIT THIS FILE.
 *
 * This file is machine generated and any edits will be lost the next
 * time the file is generated. Use the https://github.com/lapps/vocabulary-pages
 * project to make changes.
 */
package ${packageName};

class ${className} {
    private ${className}() { }
"""
            elements.each { ElementDelegate e ->
                out.println "\tpublic static final String ${e.name} = \"http://vocab.lappsgrid.org/${e.name}\";"
                e.print(System.out)
            }
            out.println "}"
        }
        println "Wrote ${outFile.path}"
    }

    void makeFeaturesJava(File scriptFile, String packageName)
    {
        compile(scriptFile.text)
        File outFile = new File("Features.java")
        outFile.withPrintWriter { PrintWriter out ->
            out.println """/*
* DO NOT EDIT THIS FILE.
*
* This file is machine generated and any edits will be lost the next
* time the file is generated. Use the https://github.com/lapps/vocabulary-pages
* project to make changes.
*/

package ${packageName};

class Features {
\tprivate Features() { }
"""
            elements.each { ElementDelegate e ->
                String superClass = ""
                if (e.parent) {
                    superClass = " extends ${e.parent}"
                }
                out.println "\tpublic static class ${e.name}${superClass} {"
                e.properties.each { String name, ignored ->
                    String snakeCase = toSnakeCase(name)
                    if (name == "pos") {
                        // Hack-around since the POS tag name changed.
                        out.println "\t\tpublic static final String PART_OF_SPEECH = \"pos\";"
                    }
                    out.println "\t\tpublic static final String ${snakeCase} = \"${name}\";"
                }
                out.println "\t}"
                out.println()
            }
            out.println("}")
        }
        println "Wrote ${outFile.path}"
    }

    protected String toSnakeCase(String name) {
        if (name.endsWith("Type")) {
            return "TYPE"
        }
        return name.replaceAll(/\B[A-Z]/){'_'+it}.toUpperCase()
    }

    static void main(args) {
//        if (args.size() == 0) {
//            println """
//USAGE
//
//java -jar vocab-${Version.version}.jar [-groovy] /path/to/script"
//
//Specifying the -groovy flag will cause the GroovyTemplateEngine to be
//used. Otherwise the MarkupBuilderTemplateEngine will be used.
//
//"""
//            return
//        }
        CliBuilder cli = new CliBuilder()
        cli.usage = "vocab [-?|-v] -d <dsl> -i <template> -h <template> -o <directory>"
        cli.header = "Generates LAPPS Vocabulary web site a LAPPS Vocab DSL file."
        cli.v(longOpt:'version', 'displays current application version number.')
        cli.h(longOpt:'html', args:1,'template used to generate html pages for vocabulary items.')
        cli.r(longOpt:'rdf', args:1, 'generates RDF/OWL ontology in the specifed format')
        cli.i(longOpt:'index', args:1, 'template used to generate the index.html page.')
        cli.j(longOpt:'java', args:1, 'generates a Java class containing URI defintions.')
        cli.f(longOpt:'features', 'generates the Features.java with element property names.')
        cli.p(longOpt:'package', args:1, 'package name for the Java class.')
        cli.d(longOpt:'dsl', args:1, 'the input DSL specification.')
        cli.o(longOpt:'output', args:1, 'output directory.')
        cli.x(longOpt:'debug', 'prints a data dump rather than generating anything.')
        cli.'?'(longOpt:'help', 'displays this usage messages.')

        def params = cli.parse(args)
        if (!params) {
            return
        }

        if (params.'?') {
            cli.usage()
            return
        }
        if (params.v) {
            println()
            println "LAPPS Vocabulary DSL v" + Version.getVersion()
            println "Copyright 2014 American National Corpus"
            println()
            return
        }

//        else {
//            File scriptFile = new File(args[0])
//            File destination
//            if (args.size() == 2) {
//                destination = new File(args[1])
//            }
//            else {
//                destination = new File(".")
//            }
//            new VocabDsl().run(scriptFile, destination)
//        }
        VocabDsl dsl = new VocabDsl()
        File scriptFile = new File(params.d)
        if (params.j) {
            String packageName = "org.lappsgrid.discrimintor"
            if (params.p) {
                packageName = params.p
            }
            dsl.makeJava(scriptFile, packageName, params.j)
            return
        }
        if (params.f) {
            String packageName = "org.lappsgrid.vocabulary"
            if (params.p) {
                    packageName = params.p
            }
            dsl.makeFeaturesJava(scriptFile, packageName)
            return
        }
        if (params.x) {
            dsl.dump(scriptFile)
            return
        }
        if (params.r) {
            RDFFormat format = RDFFormat.RDFXML_PRETTY
            switch (params.r) {
                case 'owl':
                    format = RDFFormat.RDFXML_PRETTY
                    break
                case 'nq':
                    format = RDFFormat.NQ
                    break
                case 'n3':
                    format = RDFFormat.NTRIPLES_UTF8
                    break
                case 'jsonld':
                    format = RDFFormat.NQ.JSONLD_PRETTY
                    break
                case 'ttl':
                    format = RDFFormat.TTL
                    break
                default:
                    println "ERROR: Unknown output format ${params.r}"
                    println "Use one of 'owl', 'nq', 'n3', or 'jsonld'."
                    return
            }
            dsl.makeOwl(scriptFile, format)
            return
        }
        File destination = new File(params.o)
        if (!destination.exists()) {
            if (!destination.mkdirs()) {
                println "Unable to create output directory ${destination.path}"
                return
            }
        }
        dsl.INDEX_TEMPLATE = params.i
        dsl.FILE_TEMPLATE = params.h
        dsl.run(scriptFile, destination)
    }
}
