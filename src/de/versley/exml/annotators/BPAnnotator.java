package de.versley.exml.annotators;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import de.versley.exml.config.FileReference;
import de.versley.exml.pipe.SentenceTree;
import de.versley.exml.treetransform.TreeTransformer;
import edu.berkeley.nlp.PCFGLA.CoarseToFineMaxRuleParser;
import edu.berkeley.nlp.PCFGLA.Grammar;
import edu.berkeley.nlp.PCFGLA.ParserData;
import edu.berkeley.nlp.PCFGLA.SophisticatedLexicon;
import edu.berkeley.nlp.PCFGLA.TreeAnnotations;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.Numberer;
import exml.objects.NamedObject;
import exml.tueba.TuebaDocument;
import exml.tueba.TuebaNodeMarkable;
import exml.tueba.TuebaTerminal;

public class BPAnnotator extends SimpleAnnotator {
	protected Grammar gram;
	protected SophisticatedLexicon lex;
	protected CoarseToFineMaxRuleParser parser;
	
	public FileReference modelName;
	public List<TreeTransformer> transforms; 
	public BPAnnotator(String model) {
		modelName = new FileReference(model);
		transforms = new ArrayList<TreeTransformer>();
	}
	
	public BPAnnotator() {
	}
	
	public void loadModels() {
		if (gram == null) {
			ParserData pdata = ParserData.Load(modelName.toPath());
			gram = pdata.getGrammar();
			lex = (SophisticatedLexicon)pdata.getLexicon();
			Numberer.setNumberers(pdata.getNumbs());
			parser = new CoarseToFineMaxRuleParser(gram, lex,
					1.0, -1,
					false, false, false, true, false,
					true, true);
		}
	}
	
	public void add_transform(TreeTransformer xform) {
		transforms.add(xform);
	}
	
	public Tree<String> parseWords(List<String> words) {
		//TODO handle unparsed sentences
		//TODO normalize strings
		//TODO handle token replacement
		return TreeAnnotations.unAnnotateTree(
				parser.getBestConstrainedParse(words, null, false), true);
	}

	public NamedObject berkeley2node(Tree<String> bp_node, List<TuebaTerminal> terms, int posn)
	{
		int i = posn;
		int offset = terms.get(0).getStart();
		if (bp_node.isPreTerminal()) {
			TuebaTerminal term = terms.get(i);
			term.setCat(bp_node.getLabel());
			return term;
		} else {
			TuebaNodeMarkable m = new TuebaNodeMarkable();
			m.setStart(i);
			List<NamedObject> chlds = new ArrayList<NamedObject>();
			m.setCat(bp_node.getLabel());
			for (Tree<String> chld: bp_node.getChildren()) {
				NamedObject node = (NamedObject) berkeley2node(chld, terms, i);
				i = node.getEnd() - offset;
				chlds.add(node);
			}
			m.setChildren(chlds);
			m.setEnd(i+offset);
			return m;
		}
	}
	
	@Override
	public void annotate(TuebaDocument doc) {
		List<SentenceTree> trees = SentenceTree.getTrees(doc);
		for (SentenceTree t: trees) {
			List<TuebaTerminal> terms = t.getTerminals();
			List<String> words = new ArrayList<String>();
			for (TuebaTerminal n: terms) {
				words.add(n.getWord());
			}
			Tree<String> result = parseWords(words);
			TuebaNodeMarkable m_root = (TuebaNodeMarkable) berkeley2node(result, terms, 0);
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
}
