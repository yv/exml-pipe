#!/usr/bin/env bash
source setup.sh
JAVA_OPTS="-Xmx12g"
java $JAVA_OPTS de.versley.exml.pipe.TextToEXML $*

