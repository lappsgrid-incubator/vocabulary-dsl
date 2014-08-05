package org.anc.lapps.vocab.dsl

import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer

/**
 * @author Keith Suderman
 */
class VocabDsl {
    static final String EXTENSION = ".vocab"
    static final String MKB_TEMPLATE = "src/test/resources/template.groovy"
    static final String HTML_TEMPLATE = "src/test/resources/template.html"

    // Selects the templating engine to use.  Choices are the MarkupBuilderTemplateEngine
    // or HtmlTemplateEngine. The former uses a template that looks like HTML while the
    // latter uses the MarkupBuilder DSL as the template language.
    static boolean USE_MARKUPBUILDER = true

    Set<String> included = new HashSet<String>()
    File parentDir
    Binding bindings = new Binding()
    List<ElementDelegate> elements = []
    Map<String, ElementDelegate> elementIndex = [:]

    void run(File file, args) {
        parentDir = file.parentFile
        run(file.text, args)
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

    void interactiveMode(args) {
        TextDevice io = TextDevice.create()
        ClassLoader loader = getLoader()
        CompilerConfiguration configuration = getCompilerConfiguration()
        GroovyShell shell = new GroovyShell(loader, bindings, configuration)
        def params = [:]
        if (args != null && args.size() > 0) {
            // Parse any command line arguements into a HashMap that will
            // be passed in to the user's script.
            args.each { arg ->
                String[] parts = arg.split('=')
                String name = parts[0].startsWith('-') ? parts[0][1..-1] : parts[0]
                String value = parts.size() > 1 ? parts[1] : Boolean.TRUE
                params[name] = value
            }
        }
        boolean running = true
        while (running) {
            io.printf("> ")
            String input = io.readLine()
            if (input == "exit") {
                running = false
            }
            else {
                Script script = shell.parse(input)
                script.binding.setVariable("args", params)
                script.metaClass = getMetaClass(script.class, shell)
                try {
                    script.run()
                }
                catch (Exception e) {
                    io.println()
                    io.println "Script execution threw an exception:"
                    e.printStackTrace()
                    io.println()
                }
            }
        }
        io.println("Good-bye.")
    }

    void run(String scriptString, args) {
        ClassLoader loader = getLoader()
        CompilerConfiguration configuration = getCompilerConfiguration()
        GroovyShell shell = new GroovyShell(loader, bindings, configuration)

        Script script = shell.parse(scriptString)
        if (args != null && args.size() > 0) {
            // Parse any command line arguements into a HashMap that will
            // be passed in to the user's script.
            def params = [:]
            args.each { arg ->
                String[] parts = arg.split('=')
                String name = parts[0].startsWith('-') ? parts[0][1..-1] : parts[0]
                String value = parts.size() > 1 ? parts[1] : Boolean.TRUE
                params[name] = value
            }
            script.binding.setVariable("args", params)
        }
        else {
            script.binding.setVariable("args", [:])
        }

        // Create the template engine that will generate the HTML.
        TemplateEngine engine
        if (USE_MARKUPBUILDER) {
            println "Using the MarkupBuilderTemplateEngine with the MKB_TEMPLATE"
            File templateFile = new File(MKB_TEMPLATE)
            if (!templateFile.exists()) {
                throw new FileNotFoundException("Unable to load the template file.")
            }
            engine = new MarkupBuilderTemplateEngine(templateFile)
        }
        else {
            println "Using the HtmlTemplateEngine with the HTML_TEMPLATE"
            File templateFile = new File(HTML_TEMPLATE)
            if (!templateFile.exists()) {
                throw new FileNotFoundException("Unable to load the template file.")
            }
            engine = new HtmlTemplateEngine(templateFile)
        }
        script.metaClass = getMetaClass(script.class, shell)
        try {
            // Running the DSL script creates the objects needed to generate the HTML
            script.run()
            makeHtml(engine)
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
            File file = new File("${element.name}.html")
            file.text = template.generate(elementIndex, element)
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
            println "LAPPS Vocabulary DSL v"
            println "Copyright 2014 American National Corpus"
            println()
            return
        }
        else if (args[0] == '-i' || args[0] == "--interactive") {
            new VocabDsl().interactiveMode(args)
        }
        else if (args[0] == "-groovy") {
            VocabDsl.USE_MARKUPBUILDER = false
            def argv = args[1..-1]
            new VocabDsl().run(new File(args[1]), argv)
        }
        else {
            def argv = null
            if (args.size() > 1) {
                argv = args[1..-1]
            }
            new VocabDsl().run(new File(args[0]), argv)
        }
    }
}
