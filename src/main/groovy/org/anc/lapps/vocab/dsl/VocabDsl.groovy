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

    void makeOwl(File script, File destination, String ext) {
        makeOwl(script, RDFFormat.JSONLD_PRETTY, destination, ext)
    }

    void makeOwl(File script, RDFFormat format, File destination, String ext) {
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
//                if (element == parent) {
//                    println "Skipping parent: element == parent"
//                }
//                else if (element.equals(parent)) {
//                    println "Skipping parent: element.equals(parent)"
//                }
//                else {
//                    println "Superclass for ${element.name} is ${element.parent}"
                    theClass.setSuperClass(parent)
//                }
            }
            element.sameAs.each { resource ->
//                println "Same as $resource"
                theClass.addSameAs(ontology.createResource(resource))
            }
            element.metadata.each { String key, PropertyDelegate value ->
//                println "metadata $key -> ${value.type}"
                Property property = makeMetadata(key)
                property.addComment(ontology.createLiteral(value.description))
                theClass.addProperty(property, value.type)
            }
            element.properties.each { String name, PropertyDelegate value ->
//                println "property $name -> ${value.type}"
                Property property = makeProperty(element.name, name)
                property.addComment(ontology.createLiteral(value.description))
                theClass.addProperty(property, value.type)
            }
//            element.similarTo.each { resource ->
//            }

        }

        File file = new File(destination, "lapps-vocabulary.${ext}")
        OutputStream os = new FileOutputStream(file)
        RDFDataMgr.write(os, ontology, format)
        os.flush()
        os.close()
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

    void makeDiscriminators(File script, File destination) {
        List<String> order = ['annotation',
                              'chunk',
                              'paragraph',
                              'sentence',
                              'token',
                              'pos',
                              'coref',
                              'ne',
                              'person',
                              'location',
                              'date',
                              'organization',
                              'nchunk',
                              'vchunk',
                              'lemma',
                              'lookup',
                              'matches',
                              'markable',
                              'dependency-structure',
                              'phrase-structure',
                              'constituent',
                              'dependency']

        compile(script.text)
        elements << new ElementDelegate(name:'Chunk', discriminator:'chunk', definition: "Any type of annotations that segments the primary data into chunks.", parent: 'annotation')
        elements << new ElementDelegate(name:'Token#pos', discriminator: 'pos', definition: "Part-Of-Speech tag. The tagset used should be specified in metadata.", parent: 'annotation')
        elements << new ElementDelegate(name:'Token#lemma', discriminator: 'lemma', definition: 'Base form of a word or token.', parent: 'annotation')
        elements << new ElementDelegate(name:'Lookup', discriminator: 'lookup', definition: 'Dictionary based annotations. Used in GATE.', parent: 'annotation')
        elements << new ElementDelegate(name:'Matches', discriminator: 'matches', definition: 'Definition needed.', parent: 'annotation')

        File outputFile = new File(destination, "discriminators.txt")
        outputFile.withWriter { writer ->
            writer.println("bank(2) {")

            order.each { String name ->
                ElementDelegate element = elements.find { it.discriminator == name }
                if (element) {
                    writeDiscriminator(writer, element)
                }
                else {
                    println "Missing element ${name}"
                }
            }
            elements.each { ElementDelegate element ->
                if (!order.contains(element.discriminator)) {
                    writeDiscriminator(writer, element)
                }
                else {
                    println "Order list contains ${element.name}"
                }
            }
            writer.println("}")
        }
        println "Wrote ${outputFile.path}"
    }

    void writeDiscriminator(BufferedWriter writer, ElementDelegate element) {
        if (!element.discriminator) {
            println "Skipping ${element.name}"
            return
        }

        println "Generating discriminator info for ${element.name}"
        if (element.discriminator.contains('-')) {
            writer.println "\t\"${element.discriminator}\" {"
        }
        else {
            writer.println("\t${element.discriminator} {")
        }
        if (element.parent) {
            writer.println("\t\tparents ${element.parent}")
        }
        writer.println("\t\turi vocab('${element.name}')")
        writer.println("\t\tdescription \"${element.definition}\"")
        writer.println("\t}")
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

    void makeJava(File scriptFile, String packageName, String className, File destination) {
        println "Generating ${className}.java"
        compile(scriptFile.text)

        File outFile = new File(destination, "${className}.java")
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

public class ${className} {
    private ${className}() { }
"""
            elements.sort { a,b -> a.name.compareTo(b.name) }.each { ElementDelegate e ->
                out.println "\tpublic static final String ${e.name.toUpperCase()} = \"http://vocab.lappsgrid.org/${e.name}\";"
//                e.print(System.out)
            }
            out.println "}"
        }
        println "Wrote ${outFile.path}"
    }

    void makeFeaturesJava(File scriptFile, String packageName, File destination)
    {
        println "Generating Features.java"
        compile(scriptFile.text)
        File outFile = new File(destination, "Features.java")
        outFile.withPrintWriter { PrintWriter out ->
            out.println """/*
 * DO NOT EDIT THIS FILE.
 *
 * This file is machine generated and any edits will be lost the next
 * time the file is generated. Use the https://github.com/lapps/vocabulary-pages
 * project to make changes.
 */

package ${packageName};

public class Features {
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
        cli.c(longOpt: 'discriminators', 'generated Discriminator DSL fragment.')
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

        File destination
        if (params.o) {
            destination = new File(params.o)
        }
        else {
            destination = new File('target')
        }

        if (!destination.exists()) {
            if (!destination.mkdirs()) {
                println "Unable to create output directory ${destination.path}"
                return
            }
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
            dsl.makeJava(scriptFile, packageName, params.j, destination)
            return
        }
        if (params.f) {
            String packageName = "org.lappsgrid.vocabulary"
            if (params.p) {
                    packageName = params.p
            }
            dsl.makeFeaturesJava(scriptFile, packageName, destination)
            return
        }
        if (params.c) {
            dsl.makeDiscriminators(scriptFile, destination)
            return
        }
        if (params.x) {
            dsl.dump(scriptFile)
            return
        }
        if (params.r) {
            String ext = params.r
            RDFFormat format = RDFFormat.RDFXML_PRETTY
            switch (params.r) {
                case 'owl':
                    format = RDFFormat.RDFXML_PRETTY
                    break
                case 'rdf':
                    format = RDFFormat.RDFXML_PLAIN
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
            dsl.makeOwl(scriptFile, format, destination, ext)
            return
        }
        dsl.INDEX_TEMPLATE = params.i
        dsl.FILE_TEMPLATE = params.h
        dsl.run(scriptFile, destination)
    }
}
