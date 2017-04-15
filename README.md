# LAPPS Vocabulary DSL

### Build status 
[![master status](http://grid.anc.org:9080/travis/svg/lappsgrid-incubator/vocabulary-dsl?branch=master)](https://travis-ci.org/lappsgrid-incubator/vocabulary-dsl)
[![develop status](http://grid.anc.org:9080/travis/svg/lappsgrid-incubator/vocabulary-dsl?branch=develop)](https://travis-ci.org/lappsgrid-incubator/vocabulary-dsl)

Templating system to generate LAPPS vocabulary web pages.

The LAPPS Vocabulary is expressed as a Groovy DSL that is then processed to generate the http://vocab.lappsgrid.org site as well as Java classes and input to the [Discriminator DSL](https://github.com/lappsgrid-incubator/org.lappsgrid.discriminator.dsl).

## Usage

```
java -jar vocab.jar [options] vocabulary-file
```
where `vocabulary-file` is the path to the dsl file to be processed.

## Options

* **-h --html** &lt;path&gt;<br/>specifies the template used to generate the HTML page for each term in the vocabulary
* **-i --index** &lt;path&gt;<br/>specifies the template used to generate the vocabulary index.html page
* **-d --discriminators**<br/>generate the discriminator dsl fragement
* **-r --rdf** &lt;string&gt;<br/>generates one of the RDF formats owl, rdf, ttl, jsonld, or n3
* **-f --features**<br/>generates the Features.java source file
* **-j --java** &lt;string&gt;<br/>generates the Java source file with annotation definitons. The parameter is the name of the class/file that will be generated.
* **-p --package** &lt;string&gt;<br/>specify the package name to use for the Java source files.
* **-o --output** &lt;path&gt;<br/>output directory

### Examples

```
vocab -h pages.template -i index.template -o /tmp lapps.vocab
vocab -j Annotations -p org.lappsgrid.vocabulary -o /tmp lapps.vocab
```

## Makefile

While the build and dependency management is done with Maven a Makefile is also included to simplify common tasks.

### Common Goals

* **jar**<br/>
Creates an executable jar file. Runs `mvn package`.
* **clean**<br/>
Removes artifacts from previous builds. Runs `mvn clean`.
* **install**<br/>
Copies the jar and startup script to `$HOME/bin`.
* **release**<br/>
Creates a zip file with the jar files, startup script, and example templates.
* **upload**<br/>
Uploads the zip file to the downloads area on the ANC web server. This goal will need to be modified before it can be used by other users and also assumes the user has a valid SSH key installed on the ANC server.

## Discriminators

Currently the vocabulary files and the discriminator files are genereated by two separate DSL processors.  Currently the output from the vocabulary DSL must be copied and pasted into the discriminator.config file before the disciminator web pages can be generated.