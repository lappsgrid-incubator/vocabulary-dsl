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
    static final String FILE_TEMPLATE = "src/test/resources/template.groovy"
    static final String INDEX_TEMPLATE = "src/test/resources/index.groovy"

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
        ClassLoader loader = getLoader()
        CompilerConfiguration configuration = getCompilerConfiguration()
        GroovyShell shell = new GroovyShell(loader, bindings, configuration)

        Script script = shell.parse(scriptString)
//        if (args != null && args.size() > 0) {
//            // Parse any command line arguements into a HashMap that will
//            // be passed in to the user's script.
//            def params = [:]
//            args.each { arg ->
//                String[] parts = arg.split('=')
//                String name = parts[0].startsWith('-') ? parts[0][1..-1] : parts[0]
//                String value = parts.size() > 1 ? parts[1] : Boolean.TRUE
//                params[name] = value
//            }
//            script.binding.setVariable("args", params)
//        }
//        else {
            script.binding.setVariable("args", [:])
//        }

        // Create the template engine that will generate the HTML.
        TemplateEngine engine = new MarkupBuilderTemplateEngine(new File(FILE_TEMPLATE))
        script.metaClass = getMetaClass(script.class, shell)

        try {
            // Running the DSL script creates the data model needed to generate the HTML.
            script.run()

            // Now generate the HTML.
            //makeHtml(engine)
            makeIndexHtml()
        }
        catch (Exception e) {
            println()
            println "Script execution threw an exception:"
            e.printStackTrace()
            println()
        }
    }

    void makeHtml(TemplateEngine template) {
        elements.each { element ->
            List parents = []
            String parent = element.parent
            while (parent) {
                ElementDelegate delegate = elementIndex[parent]
                parents.add(0, delegate)
                parent = delegate.parent
            }
            def params = [ element:element, elements:elementIndex, parents:parents ]
            File file = new File(destination, "${element.name}.html")
            file.text = template.generate(params)
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
        new File(destination, 'index.html').text = html
        println "Wrote index.html"
    }

    List<TreeNode> getTrees() {
        List<TreeNode> roots = []
        Map<String,TreeNode> nodeMap = [:]
        elements.each { ElementDelegate element ->
            TreeNode elementNode = TreeNode.get(element.name)
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
        if (args.size() == 0) {
            println """
USAGE

java -jar vocab-.jar [-groovy] /path/to/script"

Specifying the -groovy flag will cause the GroovyTemplateEngine to be
used. Otherwise the MarkupBuilderTemplateEngine will be used.

"""
            return
        }

        if (args[0] == '-version') {
            println()
            println "LAPPS Vocabulary DSL v" + Version.getVersion()
            println "Copyright 2014 American National Corpus"
            println()
            return
        }
        else {
            File scriptFile = new File(args[0])
            File destination
            if (args.size() == 2) {
                destination = new File(args[1])
            }
            else {
                destination = new File(".")
            }
            new VocabDsl().run(scriptFile, destination)
        }
    }
}
