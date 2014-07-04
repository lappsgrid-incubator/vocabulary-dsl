VERSION=$(shell cat VERSION)
JAR=target/vocab-$(VERSION).jar

help:
	@echo
	@echo "Available goals are:"
	@echo
	@echo "    clean : Clean removes all artifacts from previous builds"
	@echo "      jar : Creates the vocab.jar file."
	@echo "  install : Copies the jar to the user's bin directory."
	@echo "  release : Zips executables and uploads to the ANC web server."
	@echo "     help : Displays this help message."
	@echo
	
jar:
	mvn package
	
clean:
	mvn clean
	
install:
	#cp target/lsd-$(VERSION).jar $(HOME)/bin
	cp $(JAR) $(HOME)/bin
	cat src/test/resources/vocab | sed 's/__VERSION__/$(VERSION)/' > $(HOME)/bin/vocab
	
debug:
	@echo "Current version is $(VERSION)"
	
release:
	#mvn clean package
	if [ ! -f $(JAR) ] ; then mvn clean package ; fi
	cat src/test/resources/vocab | sed 's/__VERSION__/$(VERSION)/' > target/vocab
	cd target ; zip vocab-$(VERSION).zip vocab-$(VERSION).jar vocab ; cp vocab-$(VERSION).zip vocab-latest.zip
	scp -P 22022 target/vocab-$(VERSION).zip suderman@anc.org:/home/www/anc/downloads
	scp -P 22022 target/vocab-latest.zip suderman@anc.org:/home/www/anc/downloads
	echo "Release complete."

