# ExmlPipe: easy(-ish) linguistic pipelines

ExmlPipe is a library for building linguistic pipelines based on the ExportXMLv2
format (used, e.g. by the TüBa-D/Z treebank from Tübingen University), which allows
one to layer different annotations in a clean fashion. The concept is similar to
DKPro in the UIMA ecosystem or GATE.

The basic usage is as follows:
```
shell$ gradle shadowJar
shell$ java -jar build/libs/exml-pipe-all.jar -lang en -pipeline corenlp input-dir output-dir
```
which would take a collection of text files in `input-dir`, annotate it with
the CoreNLP pipeline and put the result files in `output-dir`

Most of the pipelines use components that need model files. The configuration file
`exmlpipe_config.yaml` specifies the general directory where those can be found
(the default is `$HOME/data/pynlp/data` for historical reasons), and where the
individual model files are located.

Currently, there are usable pipelines for English, German
and French.

## The ExportXMLv2 annotation format

An ExportXML file consists of two parts:
 
  * A schema part, which contains information about the attributes and
    annotation levels that are part of the body.
  * a body part, which contains the actual corpus data.
  
The data in an EXML file consists of two kinds:

 * Terminal nodes, of which there is one per corpus position (i.e., tokens).
   Tokens can have a multitude of attributes, including string or enumeration
   attributes as well as references to other annotated nodes (e.g. in the case
   of dependency syntax)
 * Markable nodes, which cover a (potentially discontinuous) span in the text,
   and can, like token nodes, have attributes of their own.
   
A minimal example for this could be the following:
```
<exml-doc>
<schema>
<tnode name="word">
<text-attr name="form"/>
</tnode>
</schema>
<body>
<word form="Two"/>
<word form="words"/>
</body>
</exml-doc>
```

This document declares the "word" terminal node with the "form" string attribute, and the
body contains two of terminals.

Software libraries for reading the ExportXMLv2 format are currently
available for Java and Python:

 * Java library (used by ExmlPipe) https://github.com/yv/ExportXMLv2
 * Python library: https://github.com/yv/exmldoc

## The NLP Components in ExmlPipe

ExmlPipe includes a selection of components that fulfill the following criteria

 * GPL-compatible open source (Apache-licensed tools are nice but hard to find)
 * Runs on consumer-grade hardware (currently: within 8GB of main memory)
 * Close to state of the art accuracy
 * Not exceedingly slow
 
 If you use ExmlPipe in research or other academic work, please cite (the appropriate
 ones among) the following papers:
 
 **Berkeley parser**
 
 "Learning Accurate, Compact, and Interpretable Tree Annotation"
 Slav Petrov, Leon Barrett, Romain Thibaux and Dan Klein 
 in COLING-ACL 2006  
 
 and
 
 "Improved Inference for Unlexicalized Parsing"
 Slav Petrov and Dan Klein 
 in HLT-NAACL 2007

**MATE parser**

Bernd Bohnet, Joakim Nivre, Igor Boguslavsky, Richárd Farkas, Filip Ginter, Jan Hajic:
Joint Morphological and Syntactic Analysis for Richly Inflected Languages.
TACL 1: 415-428 (2013)

*French models:*
Candito M.-H., Crabbé B., and Denis P., 2010.
Statistical French dependency parsing: treebank conversion and first results,
Proceedings of LREC'2010, La Valletta, Malta

**CoreNLP and components**

The CoreNLP pipeline architecture is described in:

Manning, Christopher D., Mihai Surdeanu, John Bauer, Jenny Finkel, Steven J. Bethard, and David McClosky.
The Stanford CoreNLP Natural Language Processing Toolkit
In Proceedings of the 52nd Annual Meeting of the Association for Computational Linguistics 2014: System Demonstrations

All the components in CoreNLP normally have their
own references, which are linked from [the CoreNLP web page](https://stanfordnlp.github.io/CoreNLP/#citing-stanford-corenlp-in-papers).

*German NER model:*
M. Faruqui and S. Pado. Training and Evaluating a German Named Entity Recognizer with Semantic Generalization.
Proceedings of Konvens 2010, Saarbrücken, Germany.