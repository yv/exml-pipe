package de.versley.exml.annotators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.versley.exml.annotators.preprocess.LineProcessor;
import de.versley.exml.async.Consumer;
import de.versley.exml.async.NullConsumer;
import de.versley.exml.async.Piggyback;
import de.versley.exml.treetransform.TreeTransformer;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.syntax.Trees;
import exml.tueba.TuebaDocument;
import exml.tueba.TuebaNodeMarkable;
import exml.tueba.TuebaTerminal;
import exml.tueba.util.SentenceTree;

/**
 * annotator for parsers that are used via a pipeline
 * @author yannick
 *
 */
public class AsyncParserAnnotator implements Annotator {

	public List<TreeTransformer> transforms;
	public LineProcessor preprocess;
	public LineProcessor parser;

	public void add_transform(TreeTransformer xform) {
		transforms.add(xform);
	}
	
	public void loadModels() {
		if (preprocess != null) {
			preprocess.loadModels();
		}
		parser.loadModels();
	}
	
	public void close() {
		if (preprocess != null) {
			preprocess.close();
		}
		parser.close();
	}
	@Override
	public void process(final TuebaDocument doc, final Consumer<TuebaDocument> output) {
		List<SentenceTree> trees = SentenceTree.getTrees(doc);
		if (preprocess != null) {
			preprocess.loadModels();
		}
		for (int i=0; i<trees.size();i++) {
			final SentenceTree t = trees.get(i);
			final List<TuebaTerminal> terms = t.getTerminals();
			List<String> words = new ArrayList<String>();
			for (TuebaTerminal n: terms) {
				words.add(n.getWord());
			}
			Consumer<String> gotParse = new Consumer<String>() {
				@Override
				public void consume(String line) {
					Tree<String> tree = Trees.PennTreeReader.parseEasy(line);
					TuebaNodeMarkable m_root = (TuebaNodeMarkable)BPAnnotator.berkeley2node(tree, terms, 0);
					t.getRoots().clear();
					t.getRoots().addAll(m_root.getChildren());
					for (TreeTransformer xform: transforms) {
						xform.transform(t);
					}
					t.reassignParents();
					t.reassignSpans();
					t.replaceNodes();
				}
			};
			if (i == trees.size()-1) {
				gotParse = new Piggyback<String>(gotParse, new Runnable() {
					@Override
					public void run() {
						output.consume(doc);
					}
				});
			}
			final Consumer<String> gp_actual = gotParse;
			String line = StringUtils.join(words, " ");
			if (preprocess == null) {
				parser.process(line, gp_actual);
			} else {
				preprocess.process(line,
					new Consumer<String>() {
						@Override
						public void consume(String line) {
							List<String> replacements = Arrays.asList(line.split(" "));
							if (terms.size() != replacements.size()) {
								throw new RuntimeException("Weirdness in preprocessing; old:"+terms+"/new:"+replacements);
							}
							parser.process(line, gp_actual);
						}
				});
			}
		}
	}

	@Override
	public void annotate(TuebaDocument doc) {
		// we assume that we have to close and reload the parser ...
		//TODO: wait for an appropriate time to see if we can just read
		//  the line synchronously
		process(doc, new NullConsumer<TuebaDocument>());
		close();
		loadModels();
	}
		
}
