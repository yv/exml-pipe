package de.versley.exml.annotators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import de.versley.exml.annotators.preprocess.LineProcessor;
import de.versley.exml.async.Consumer;
import de.versley.exml.treetransform.TreeTransformer;
import exml.tueba.TuebaDocument;
import exml.tueba.TuebaNodeMarkable;
import exml.tueba.TuebaTerminal;
import exml.tueba.util.SentenceTree;

/**
 * Annotator component for in-process parsers that can be called
 * synchronously.
 * @author yannick
 *
 */
public abstract class AbstractParserAnnotator extends SimpleAnnotator {

	public List<TreeTransformer> transforms;
	public LineProcessor preprocess;

	public void add_transform(TreeTransformer xform) {
		transforms.add(xform);
	}
	

	@Override
	public void annotate(TuebaDocument doc) {
		List<SentenceTree> trees = SentenceTree.getTrees(doc);
		if (preprocess != null) {
			preprocess.loadModels();
		}
		for (SentenceTree t: trees) {
			final SentenceTree tt = t;
			final List<TuebaTerminal> terms = t.getTerminals();
			List<String> words = new ArrayList<String>();
			for (TuebaTerminal n: terms) {
				words.add(n.getWord());
			}
			if (preprocess == null) {
				add_parse_from(t, terms, words);
			} else {
				String line = StringUtils.join(words, " ");
				preprocess.process(line,
					new Consumer<String>() {
	
						@Override
						public void consume(String line) {
							List<String> replacements = Arrays.asList(line.split(" "));
							if (terms.size() != replacements.size()) {
								throw new RuntimeException("Weirdness in preprocessing; old:"+terms+"/new:"+replacements);
							}
							add_parse_from(tt, terms, replacements);
						}
				});
			}
		}
		if (preprocess != null) {
			preprocess.close();
		}
	}

	protected abstract TuebaNodeMarkable do_parse(List<String> words, List<TuebaTerminal> terms);
	
	protected void add_parse_from(SentenceTree t,
			List<TuebaTerminal> terms,
			List<String> words) {
		TuebaNodeMarkable m_root = do_parse(words, terms); 
		t.getRoots().clear();
		t.getRoots().addAll(m_root.getChildren());
		for (TreeTransformer xform: transforms) {
			xform.transform(t);
		}
		t.reassignParents();
		t.reassignSpans();
		t.replaceNodes();
	}

}
