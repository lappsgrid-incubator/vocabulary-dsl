package org.anc.lapps.vocab.dsl

import org.anc.template.HtmlTemplateEngine
import org.anc.template.MarkupBuilderTemplateEngine
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.anc.template.TemplateEngine

/**
 * @author Keith Suderman
 */
class VocabDsl {
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
        cli.i(longOpt:'index', args:1, 'template used to generate the index.html page.')
        cli.d(longOpt:'dsl', args:1, 'this input DSL specification.')
        cli.o(longOpt:'output', args:1, 'output directory.')
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
        File scriptFile = new File(params.d)
        File destination = new File(params.o)
        if (!destination.exists()) {
            if (!destination.mkdirs()) {
                println "Unable to create output directory ${destination.path}"
                return
            }
        }
        VocabDsl dsl = new VocabDsl()
        dsl.INDEX_TEMPLATE = params.i
        dsl.FILE_TEMPLATE = params.h
        dsl.run(scriptFile, destination)
    }
}
