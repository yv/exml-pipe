package de.versley.exml.annotators;

import java.util.ArrayList;
import java.util.List;

import de.versley.exml.config.FileReference;
import de.versley.exml.treetransform.TreeTransformer;
import edu.berkeley.nlp.PCFGLA.CoarseToFineMaxRuleParser;
import edu.berkeley.nlp.PCFGLA.Grammar;
import edu.berkeley.nlp.PCFGLA.ParserData;
import edu.berkeley.nlp.PCFGLA.SophisticatedLexicon;
import edu.berkeley.nlp.PCFGLA.TreeAnnotations;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.Numberer;
import exml.objects.NamedObject;
import exml.tueba.TuebaNodeMarkable;
import exml.tueba.TuebaTerminal;

public class BPAnnotator extends AbstractParserAnnotator {
	protected transient Grammar gram;
	protected transient SophisticatedLexicon lex;
	protected transient CoarseToFineMaxRuleParser parser;
	
	public FileReference modelName;
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
	
	public static NamedObject berkeley2node(Tree<String> bp_node, List<TuebaTerminal> terms, int posn)
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
	protected TuebaNodeMarkable do_parse(List<String> words,
			List<TuebaTerminal> terms) {
		Tree<String> result = TreeAnnotations.unAnnotateTree(
				parser.getBestConstrainedParse(words, null, false), true);
		TuebaNodeMarkable m_root = (TuebaNodeMarkable) berkeley2node(result, terms, 0);
		return m_root;
	}
}
