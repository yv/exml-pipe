# ExmlPipe: easy(-ish) linguistic pipelines

Basic usage:

java -cp ... de.versley.exml.pipe.TextToEXML sourceText.txt result.exml.xml

You can select different pipelines (currently "mate" and "pcfgla") with the "-pipeline" flag.

You need to add the file "transition-1.30.jar" from mate-tools (which is not available via MavenCentral).