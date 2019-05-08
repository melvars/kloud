.PHONY: all

all: clean build

clean:
	$(RM) -rv build/ out/ *.class

build:
	gradle build
	@echo Success! The .jar file should be in build/libs/. You may want to use \'sudo make install\' now.

install:
ifdef OS
	@echo Kloud can't be installed on Windows currently, please execute the jar file manually
else
    ifeq ($(shell uname), Linux)
	mkdir -p /usr/share/kloud/
	userdel kloud || true
	useradd -r -d /usr/share/kloud kloud || true
	chown -R kloud /usr/share/kloud
	cp build/libs/kloud-*-all.jar /usr/share/kloud/
	echo -e "#!/bin/sh\nsudo -u kloud java -jar /usr/share/kloud/kloud-*-all.jar \$$@" > /usr/bin/kloud
	chmod +x /usr/bin/kloud
    else
	@echo This OS doesn't support out automatic installation, please execute the jar file manually
    endif
endif
