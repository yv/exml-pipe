package de.versley.exml.annotators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import de.versley.exml.schemas.CorefMarkable;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.LabelFactory;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.BasicDependenciesAnnotation;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;
import exml.MarkableLevel;
import exml.objects.BeanAccessors;
import exml.objects.NamedObject;
import exml.objects.ObjectSchema;
import exml.tueba.TuebaDocument;
import exml.tueba.TuebaNodeMarkable;
import exml.tueba.TuebaTerminal;
import exml.tueba.util.SentenceTree;

public class CoreNLPAnnotator implements Annotator {
	public List<String> annotators;
	
	protected StanfordCoreNLP pipeline = null;

	public NamedObject stanford2node(Tree bp_node, List<TuebaTerminal> terms, int posn)
	{
		int i = posn;
		int offset = terms.get(0).getStart();
		if (bp_node.isPreTerminal()) {
			TuebaTerminal term = terms.get(i);
			term.setCat(bp_node.value());
			return term;
		} else {
			TuebaNodeMarkable m = new TuebaNodeMarkable();
			m.setStart(i);
			List<NamedObject> chlds = new ArrayList<NamedObject>();
			m.setCat(bp_node.value());
			for (Tree chld: bp_node.children()) {
				NamedObject node = (NamedObject) stanford2node(chld, terms, i);
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
		// transfer TokensAnnotations
		Annotation annotation = new Annotation();
		LabelFactory factory = CoreLabel.factory();
		List<CoreMap> sentences = new ArrayList<CoreMap>();
		for (SentenceTree t: SentenceTree.getTrees(doc)) {
			List<CoreLabel> tokens = new ArrayList<CoreLabel>();
			Annotation sentence = new Annotation();
			for (TuebaTerminal tok: t.getTerminals()) {
				CoreLabel tt = (CoreLabel) factory.newLabel(tok.getWord());
				tt.setWord(tok.getWord());
				tokens.add(tt);
			}
			sentence.set(CoreAnnotations.TokensAnnotation.class, tokens);
			sentences.add(sentence);
		}
		annotation.set(CoreAnnotations.SentencesAnnotation.class, sentences);
		pipeline.annotate(annotation);
		List<SentenceTree> sent_trees = SentenceTree.getTrees(doc);
		int i = 0;
		for (SentenceTree t: sent_trees) {
			CoreMap sentence = sentences.get(i);
			List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
			List<TuebaTerminal> exml_tokens = t.getTerminals();
			for (int j = 0; j< exml_tokens.size(); j++) {
				TuebaTerminal exml_tok = exml_tokens.get(j);
				CoreLabel std_tok = tokens.get(j);
				exml_tok.setCat(std_tok.getString(CoreAnnotations.PartOfSpeechAnnotation.class));
				exml_tok.setLemma(std_tok.getString(CoreAnnotations.LemmaAnnotation.class));
			}
			// parse tree
			Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
			TuebaNodeMarkable m_root = (TuebaNodeMarkable) stanford2node(tree, exml_tokens, 0);
			t.getRoots().clear();
			t.getRoots().addAll(m_root.getChildren());
			t.reassignParents();
			t.reassignSpans();
			t.replaceNodes();
			SemanticGraph uncollapsedDeps = sentence.get(BasicDependenciesAnnotation.class);
			for (SemanticGraphEdge e: uncollapsedDeps.edgeListSorted()) {
				TuebaTerminal n_gov = exml_tokens.get(e.getGovernor().index()-1);
				TuebaTerminal n_dep = exml_tokens.get(e.getDependent().index()-1);
				n_dep.setSyn_parent(n_gov);
				n_dep.setSyn_label(e.getRelation().getShortName());
			}
			i++;
		}
		// TODO transfer NE information
		// Coreference
		Map<Integer, CorefChain> corefChains = annotation.get(
				CorefCoreAnnotations.CorefChainAnnotation.class);
		ObjectSchema<CorefMarkable> coref_schema = 
				BeanAccessors.getInstance().schemaForClass(CorefMarkable.class);
		MarkableLevel<CorefMarkable> coref_level = doc.markableLevelForClass(CorefMarkable.class, "coref");
		for (Map.Entry<Integer, CorefChain> entry: corefChains.entrySet()) {
			int chainId = entry.getKey();
			CorefChain chain = entry.getValue();
			for (CorefMention m: chain.getMentionsInTextualOrder()) {
				CorefMarkable mm = coref_level.schema.createMarkable();
				int sent_offset = sent_trees.get(m.sentNum-1).getTerminals().get(0).getStart();
				mm.chainId = ""+chainId;
				mm.setStart(m.startIndex-1+sent_offset);
				mm.setEnd(m.endIndex-1+sent_offset);
				mm.setXMLId(String.format("coref_%d", m.mentionID));
				// System.err.println(m.toString());
				// System.err.println(mm.getWords(doc));
				coref_level.addMarkable(mm);
			}
		}
	}

	@Override
	public void loadModels() {
		Properties props = new Properties();
		props.setProperty("annotators", StringUtils.join(annotators, ","));
		props.setProperty("enforceRequirements", "false");
		pipeline = new StanfordCoreNLP(props);
	}

	@Override
	public void unloadModels() {
		// TODO Auto-generated method stub
		
	}

}
