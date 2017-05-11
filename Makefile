VERSION=$(shell cat VERSION)
NAME=vocab-$(VERSION)
JAR=target/$(NAME).jar
RES=src/test/resources
HTML=html
DIST=target/vocab

help:
	@echo
	@echo "Available goals are:"
	@echo
	@echo "      clean : Removes all artifacts from previous builds."
	@echo "        jar : Creates the vocab-$(VERSION).jar file."
	@echo "    release : Creates and zips the startup script and jar file."
	@echo "    install : Copies the jar and startup script to the user's bin directory."
	@echo "     upload : Uploads the zip files to the ANC web server."
	@echo "       help : Displays this help message."
	@echo

jar:
	mvn package

clean:
	mvn clean

install:
	cp $(JAR) $(HOME)/bin
	cat $(RES)/vocab | sed 's|__JAR__|vocab-$(VERSION).jar|' > $(HOME)/bin/vocab

debug:
	@echo "Current version is $(VERSION)"

release:
	if [ ! -f $(JAR) ] ; then mvn clean package ; fi
	if [ -d $(DIST) ] ; then rm -rf $(DIST) ; fi
	if [ -f target/$(NAME).zip ] ; then rm target/$(NAME).zip ; fi
	if [ -f target/vocab-latest.zip ] ; then rm target/vocab-latest.zip ; fi
	mkdir $(DIST)
	cat $(RES)/vocab | sed 's|__JAR__|vocab-$(VERSION).jar|' > $(DIST)/vocab
	chmod u+x $(DIST)/vocab
	cp $(JAR) $(DIST)
	cd target ; zip -r vocab vocab ; cp vocab.zip $(NAME).zip ; mv vocab.zip vocab-latest.zip
	echo "Release ready."

ifeq ($(TOKEN),)
upload:
	@echo "Please set TOKEN with your GitHub token."
else
upload:
	if [ -e target/$(NAME).zip ] ; then scp -P 22022 target/$(NAME).zip anc.org:/home/www/anc/downloads ; fi
	if [ -e target/vocab-latest.zip ] ; then scp -P 22022 target/vocab-latest.zip anc.org:/home/www/anc/downloads ; fi
	ghc -f vocabulary.commit -t $(TOKEN)
	echo "Upload complete."
endif
