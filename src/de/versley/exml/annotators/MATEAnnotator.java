package de.versley.exml.annotators;

import is2.data.SentenceData09;
import is2.lemmatizer.Lemmatizer;
import is2.transitionS2a.Parser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.versley.exml.config.FileReference;
import de.versley.exml.pipe.SentenceTree;
import exml.tueba.TuebaDocument;
import exml.tueba.TuebaTerminal;

public class MATEAnnotator extends SimpleAnnotator {
	protected Lemmatizer _lemmatizer;
	protected Parser _parser;
	public FileReference lemma_fname;
	public FileReference parser_fname;

	public MATEAnnotator(String dirname) {
		lemma_fname = new FileReference(dirname + "lemma-ger-3.6.model");
		parser_fname = new FileReference(dirname + "pet-ger-S2a-40-0.25-0.1-2-2-ht4-hm4-kk0");
	}
	
	public MATEAnnotator() {
	}
	
	public void loadModels() {
		if (_lemmatizer == null) {
			_lemmatizer = new Lemmatizer(lemma_fname.toPath());
			_parser = new Parser(parser_fname.toPath());
		}
	}
	
	public void annotate(TuebaDocument doc) {
		for (SentenceTree tree: SentenceTree.getTrees(doc)) {
			List<String> tokens = new ArrayList<String>();
			List<TuebaTerminal> terms = tree.getTerminals();
			tokens.add("<root>");
			for (TuebaTerminal tok: tree.getTerminals()) {
				tokens.add(tok.getWord());
			}
			SentenceData09 data = new SentenceData09();
			data.init(tokens.toArray(new String[tokens.size()]));
			data = _lemmatizer.apply(data);
			_parser.apply(data);
			for (int k=0; k < terms.size(); k++) {
				TuebaTerminal tok = tree.getTerminals().get(k);
				tok.setLemma(data.plemmas[k+1]);
				tok.setCat(data.ppos[k+1]);
				tok.setMorph(data.pfeats[k+1]);
				tok.setSyn_label(data.plabels[k+1]);
				if (data.pheads[k+1] == 0 || data.pheads[k+1] == -1) {
					tok.setSyn_parent(null);
				} else {
					tok.setSyn_parent(terms.get(data.pheads[k+1]-1));
				}
			}
		}
	}
}
