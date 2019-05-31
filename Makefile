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
	@echo "     upload : Uploads the zip files to downloads.lappsgrid.org"
	@echo "     commit : Commits the jar to GitHub and creates a PR."
	@echo "        all : Does all of the above."
	@echo "       help : Displays this help message."
	@echo

jar:
	awk '{printf("%d\n", $$1+1)}' build > build.tmp && mv build.tmp build
	cp build src/main/resources
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
	cd target/vocab ; tar czf $(NAME).tgz vocab *.jar ; cp $(NAME).tgz ../ ; mv $(NAME).tgz ../vocab-latest.tgz
	echo "Release ready."

upload:
	#if [ -e target/$(NAME).zip ] ; then scp -P 22022 target/$(NAME).zip anc.org:/home/www/anc/downloads ; fi
	#if [ -e target/vocab-latest.zip ] ; then scp -P 22022 target/vocab-latest.zip anc.org:/home/www/anc/downloads ; fi
	#if [ -e target/$(NAME).tgz ] ; then scp -P 22022 target/$(NAME).tgz anc.org:/home/www/anc/downloads ; fi
	#if [ -e target/vocab-latest.tgz ] ; then scp -P 22022 target/vocab-latest.tgz anc.org:/home/www/anc/downloads ; fi
	if [ -e target/$(NAME).tgz ] ; then scp -i ~/.ssh/lappsgrid-shared-key.pem target/$(NAME).tgz root@downloads.lappsgrid.org:/var/lib/downloads ; fi
	if [ -e target/vocab-latest.tgz ] ; then scp -i ~/.ssh/lappsgrid-shared-key.pem target/vocab-latest.tgz root@downloads.lappsgrid.org:/var/lib/downloads ; fi
	echo "Upload complete."

ifeq ($(TOKEN),)
commit:
	@echo "Please set TOKEN with your GitHub token."
	@echo
	@echo "NOTE: if you just ran 'make all' then you only have to run 'make commit'"
	@echo "after setting TOKEN"
	@echo
else
commit:
	ghc -f vocabulary.commit -t $(TOKEN)
endif

all: clean jar install release upload commit
