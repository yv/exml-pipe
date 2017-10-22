package de.versley.exml.annotators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.versley.exml.config.FileReference;
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
import exml.tueba.TuebaNEMarkable;
import exml.tueba.TuebaNodeMarkable;
import exml.tueba.TuebaTerminal;
import exml.tueba.util.SentenceTree;

public class CoreNLPAnnotator extends SimpleAnnotator {
	public List<String> annotators;
	public Map<String, String> properties;
	public FileReference posModel;
	public FileReference nerModel;
	public FileReference parserModel;

	@JsonIgnore
	protected transient StanfordCoreNLP pipeline = null;

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
				if (!annotators.contains("pos")) {
					tt.set(CoreAnnotations.PartOfSpeechAnnotation.class, tok.getCat());
				}
				tokens.add(tt);
			}
			sentence.set(CoreAnnotations.TokensAnnotation.class, tokens);
			sentences.add(sentence);
		}
		annotation.set(CoreAnnotations.SentencesAnnotation.class, sentences);
		pipeline.annotate(annotation);
		boolean want_pos = annotators.contains("pos");
		boolean want_lemma = annotators.contains("lemma");
		List<SentenceTree> sent_trees = SentenceTree.getTrees(doc);
		int i = 0;
		for (SentenceTree t: sent_trees) {
			CoreMap sentence = sentences.get(i);
			List<CoreLabel> tokens = sentence.get(CoreAnnotations.TokensAnnotation.class);
			List<TuebaTerminal> exml_tokens = t.getTerminals();
			// POS, Lemma
			for (int j = 0; j< exml_tokens.size(); j++) {
				TuebaTerminal exml_tok = exml_tokens.get(j);
				CoreLabel std_tok = tokens.get(j);
				if (want_pos) {
					exml_tok.setCat(std_tok.getString(CoreAnnotations.PartOfSpeechAnnotation.class));
				}
				if (want_lemma) {
					exml_tok.setLemma(std_tok.getString(CoreAnnotations.LemmaAnnotation.class));
				}
			}
			// NER
			List<TuebaNEMarkable> ne_list = new ArrayList<TuebaNEMarkable>();
			TuebaNEMarkable last_ne = null;
			for (int j = 0; j< exml_tokens.size(); j++) {
				TuebaTerminal exml_tok = exml_tokens.get(j);
				CoreLabel std_tok = tokens.get(j);
				// This should work for raw class labels (English) as well
				// as for BIO/IOB/BILOU
				String raw_label = std_tok.getString(CoreAnnotations.NamedEntityTagAnnotation.class);
				String ne_label = null;
				if (raw_label!=null) ne_label = raw_label.replaceAll("^[BILU]-", "");
				//System.err.format("%s: %s => %s", exml_tok.getWord(), raw_label, ne_label);
				if (ne_label != null && !"O".equals(raw_label)) {
					if (last_ne != null && (last_ne.getKind().equals(ne_label) &&
							!raw_label.startsWith("B-"))) {
						last_ne.setEnd(exml_tok.getStart()+1);
					} else {
						if (last_ne != null) {
							ne_list.add(last_ne);
						}
						last_ne = new TuebaNEMarkable();
						last_ne.setStart(exml_tok.getStart());
						last_ne.setKind(ne_label);
					}
				} else {
					// O or no NE label
					if (last_ne != null) {
						ne_list.add(last_ne);
						last_ne = null;
					}
				}
			}
			if (last_ne != null) {
				ne_list.add(last_ne);
				last_ne = null;
			}
			MarkableLevel<TuebaNEMarkable> level = doc.nes;
			for (TuebaNEMarkable ne: ne_list) {
				level.addMarkable(ne);
			}
			// parse tree
			if (annotators.contains("parse")) {
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
			}
			i++;
		}
		// Coreference
		if (annotators.contains("dcoref")) {
			Map<Integer, CorefChain> corefChains = annotation.get(
					CorefCoreAnnotations.CorefChainAnnotation.class);
			if (corefChains != null) {
				ObjectSchema<CorefMarkable> coref_schema = 
						BeanAccessors.getInstance().schemaForClass(CorefMarkable.class);
				MarkableLevel<CorefMarkable> coref_level = doc.markableLevelForClass(CorefMarkable.class, "coref");
				for (Map.Entry<Integer, CorefChain> entry: corefChains.entrySet()) {
					int chainId = entry.getKey();
					CorefChain chain = entry.getValue();
					for (CorefMention m: chain.getMentionsInTextualOrder()) {
						CorefMarkable mm = coref_schema.createMarkable();
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
		}
	}

	@Override
	public void loadModels() {
		Properties props = new Properties();
		if (properties != null) {
			for (String key: properties.keySet()) {
				String val = properties.get(key);
				props.setProperty(key, val);
			}
		}
		props.setProperty("annotators", StringUtils.join(annotators, ","));
		props.setProperty("enforceRequirements", "false");
		if (posModel != null) {
			props.setProperty("pos.model", posModel.toPath());
		}
		if (nerModel != null) {
			props.setProperty("ner.model", nerModel.toPath());
		}
		if (parserModel != null) {
			props.setProperty("parser.model", parserModel.toPath());
		}
		pipeline = new StanfordCoreNLP(props);
	}

	@Override
	public void close() {
		pipeline = null;
	}
}
