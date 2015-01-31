package de.versley.exml.annotators;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import de.versley.exml.config.FileReference;
import edu.berkeley.nlp.util.Lists;
import exml.objects.NamedObject;
import exml.tueba.TuebaDocument;
import exml.tueba.TuebaNodeMarkable;
import exml.tueba.TuebaTerminal;
import exml.tueba.TuebaTerminalSchema;
import exml.tueba.util.SentenceTree;

public class DepToConst extends SimpleAnnotator {
	final static List<TuebaTerminal> EMPTY_LIST = Lists.fromArray(new TuebaTerminal[]{});
	public FileReference modelName;
    private Classifier classifier;
    private Instances dataset;
	
	public DepToConst(String filename) {
		modelName = new FileReference(filename);
	}
	
	public DepToConst() {
	}
	
	public void loadModels() {
		try {
			ObjectInputStream f = new ObjectInputStream(new FileInputStream(modelName.toFile()));
			classifier = (Classifier) f.readObject();
			dataset = (Instances) f.readObject();
			f.close();
		} catch (IOException | ClassNotFoundException ex) {
			throw new RuntimeException("Cannot load Weka classifier", ex);
		}
	}
	public static void extractDepChildren(
			List<TuebaTerminal> terms,
			List<List<TuebaTerminal>> left_deps,
			List<List<TuebaTerminal>> right_deps,
			List<List<TuebaTerminal>> left_siblings,
			List<List<TuebaTerminal>> right_siblings) {
		int offset = terms.get(0).getStart();
		for (int i = 0; i < terms.size()+1; i++) {
			left_deps.add(new ArrayList<TuebaTerminal>());
			right_deps.add(new ArrayList<TuebaTerminal>());
			right_siblings.add(new ArrayList<TuebaTerminal>());
			left_siblings.add(new ArrayList<TuebaTerminal>());
		}
		for (TuebaTerminal term: terms) {
			if (term.getSyn_parent() != null) {
				TuebaTerminal parent = (TuebaTerminal) term.getSyn_parent();
				if (parent.getStart() < term.getStart()) {
					// to the RIGHT of the parent
					List<TuebaTerminal> ldeps = left_deps.get(parent.getStart() - offset);
					List<TuebaTerminal> rdeps = right_deps.get(parent.getStart() - offset);
					List<TuebaTerminal> lsibs = left_siblings.get(term.getStart() - offset);
					lsibs.addAll(ldeps);
					lsibs.add(parent);
					lsibs.addAll(rdeps);
					for (TuebaTerminal lsib: ldeps) {
						right_siblings.get(lsib.getStart()-offset).add(term);
					}
					rdeps.add(term);
				} else {
					// to the LEFT of the parent
					List<TuebaTerminal> ldeps = left_deps.get(parent.getStart() - offset);
					List<TuebaTerminal> lsibs = left_siblings.get(term.getStart() - offset);
					lsibs.addAll(ldeps);
					for (TuebaTerminal lsib: ldeps) {
						right_siblings.get(lsib.getStart()-offset).add(term);
					}
					ldeps.add(term);
				}
			} else {
				List<TuebaTerminal> root_deps = right_deps.get(terms.size());
				left_siblings.get(term.getStart() - offset).addAll(root_deps);
				for (TuebaTerminal lsib: root_deps) {
					right_siblings.get(lsib.getStart() - offset).add(term);
				}
				root_deps.add(term);
			}
		}
		for (int i = 0; i < terms.size(); i++) {
			Collections.reverse(left_deps.get(i));
			Collections.reverse(left_siblings.get(i));
		}
	}

	@Override
	public void annotate(TuebaDocument doc) {
		List<SentenceTree> trees = SentenceTree.getTrees(doc);
		for (SentenceTree t: trees) {
			List<TuebaNodeMarkable> nodes = new ArrayList<TuebaNodeMarkable>();
			List<TuebaTerminal> terms = t.getTerminals();
			List<List<TuebaTerminal>> left_chlds = new ArrayList<List<TuebaTerminal>>();
			List<List<TuebaTerminal>> right_chlds = new ArrayList<List<TuebaTerminal>>();
			List<List<TuebaTerminal>> left_siblings = new ArrayList<List<TuebaTerminal>>();
			List<List<TuebaTerminal>> right_siblings = new ArrayList<List<TuebaTerminal>>();
			int offset = terms.get(0).getStart();
			extractDepChildren(terms, left_chlds, right_chlds,
					left_siblings, right_siblings);
			for (TuebaTerminal n: terms) {
				// make nodes
				int node_pos = n.getStart() - offset;
				List<TuebaTerminal> lchlds = left_chlds.get(node_pos);
				List<TuebaTerminal> rchlds = right_chlds.get(node_pos);
				List<TuebaTerminal> lsibs = left_siblings.get(node_pos);
				List<TuebaTerminal> rsibs = right_siblings.get(node_pos);
				TuebaTerminal p = (TuebaTerminal)n.getSyn_parent();
				String[] feats = get_features(n,p,lchlds,rchlds,lsibs,rsibs);
				System.err.println(Arrays.asList(feats));
				String node_label = classify_nodelabel(feats);
				System.err.println(node_label);
				TuebaNodeMarkable m = new TuebaNodeMarkable();
				m.setCat(node_label);
				m.setChildren(new ArrayList<NamedObject>());
				nodes.add(m);
			}
			t.getRoots().clear();
			for (int i=0; i<terms.size(); i++) {
				TuebaTerminal n = terms.get(i);
				TuebaNodeMarkable nn = nodes.get(i);
				// add children
				for (int j = 0; j<terms.size(); j++) {
					TuebaTerminal m = terms.get(j);
					TuebaNodeMarkable mm = nodes.get(j);
					if (m.getSyn_parent() == n) {
						nn.getChildren().add(mm);
					} else if (m == n) {
						nn.getChildren().add(n);
					}
				}
				if (n.getSyn_parent() == null) {
					t.getRoots().add(nn);
				}
			}
			t.reassignParents();
			t.reassignSpans();
			t.replaceNodes();
		}
	}

	private String classify_nodelabel(String[] feats) {
		Instance inst = new Instance(dataset.numAttributes());
		inst.setDataset(dataset);
		for (int i=0; i<feats.length; i++) {
			try {
				inst.setValue(i, feats[i]);
			} catch (IllegalArgumentException ex) {
				System.err.println(
					String.format("Value %s not defined for attribute %s",
							feats[i], dataset.attribute(i).name()));
				return "X";
			}
		}
		try {
			double result = classifier.classifyInstance(inst);
			Attribute result_att = dataset.attribute(dataset.numAttributes()-1);
			return result_att.value((int)result);
		} catch (Exception e) {
			throw new RuntimeException("WEKA barfed", e);
		}
	}

	public static String[] get_features(
			TuebaTerminal n, TuebaTerminal p,
			List<TuebaTerminal> ldeps, List<TuebaTerminal> rdeps,
			List<TuebaTerminal> lsibs, List<TuebaTerminal> rsibs) {
		System.err.println(ldeps);
		System.err.println(rdeps);
		System.err.println(lsibs);
		System.err.println(rsibs);
		String any_deps = "y";
		String any_kon = "n";
		if (ldeps.isEmpty() && rdeps.isEmpty()) {
			any_deps = "n";
		} else {
			for (TuebaTerminal c: ldeps) {
				if ("KON".equals(c.getCat())) {
					any_kon = "y";
				}
			}
			for (TuebaTerminal c: rdeps) {
				if ("KON".equals(c.getCat())) {
					any_kon = "y";
				}
			}
		}
		String p_cat, p_ccat;
		if (p==null) {
			p_cat = "ROOT";
			p_ccat = "ROOT";
		} else {
			p_cat = p.getCat();
			p_ccat = coarse_cat(p_cat);
		}
		exml.objects.Attribute<TuebaTerminal, String> att_word = 
				(exml.objects.Attribute<TuebaTerminal,String>) TuebaTerminalSchema.instance.attrs.get("word");
		return new String[]{
				n.getCat(),
				coarse_cat(n.getCat()),
				p_cat,
				p_ccat,
				any_deps, any_kon,
				get_attr_idx(lsibs, 0, att_word),
				get_attr_idx(rsibs, 0, att_word),
				get_attr_idx(ldeps, 0, att_word),
				get_attr_idx(rdeps, 0, att_word)
		};
	}

	private static String coarse_cat(String cat) {
		// returns the coarse category
		//TODO make some language-independent mechanism for tagset mapping
		char c1 = cat.charAt(0);
		if (c1 == 'N') return "N";
		if (c1 == 'V') {
			if (cat.endsWith("FIN") || cat.endsWith("IMP")) {
				return "Vfin";
			} else {
				return "V";
			}
		}
		if (cat.startsWith("ADJ")) {
			return "A";
		} else if (cat.matches("PPER|PDS|PWS|PRF")) {
			return "PRO";
		} else if (cat.matches("ART|PIAT|PDAT|PPOSAT")) {
			return "DET";
		} else if (cat.startsWith("APP")) {
			return "ADP";
		}
		return cat;
	}

	private static String get_attr_idx(List<TuebaTerminal> lst, int i, exml.objects.Attribute<TuebaTerminal,String> idxCat) {
		if (lst.size() <= i || i < 0) {
			return "_";
		} else {
			return idxCat.accessor.get(lst.get(i));
		}
	}
	
	public static void main(String[] argv) {
		try {
			TuebaDocument doc = TuebaDocument.loadDocument(argv[0]);
			List<SentenceTree> trees = SentenceTree.getTrees(doc);
			for (SentenceTree t: trees) {
				List<TuebaTerminal> terms = t.getTerminals();
				List<List<TuebaTerminal>> left_chlds = new ArrayList<List<TuebaTerminal>>();
				List<List<TuebaTerminal>> right_chlds = new ArrayList<List<TuebaTerminal>>();
				List<List<TuebaTerminal>> left_siblings = new ArrayList<List<TuebaTerminal>>();
				List<List<TuebaTerminal>> right_siblings = new ArrayList<List<TuebaTerminal>>();
				extractDepChildren(terms, left_chlds, right_chlds,
						left_siblings, right_siblings);
				for (int node_pos = 0; node_pos < terms.size(); node_pos++) {
					TuebaTerminal n = terms.get(node_pos);
					List<TuebaTerminal> lchlds = left_chlds.get(node_pos);
					List<TuebaTerminal> rchlds = right_chlds.get(node_pos);
					List<TuebaTerminal> lsibs = left_siblings.get(node_pos);
					List<TuebaTerminal> rsibs = right_siblings.get(node_pos);
					TuebaTerminal p = (TuebaTerminal)n.getSyn_parent();
					String[] feats = get_features(n,p,lchlds,rchlds,lsibs,rsibs);
					String target;
					if (n.getParent() == null) {
						target = "_";
					} else {
						if (n.getSlotByName("head") == p.getSlotByName("head")) {
							// TODO perform head projection
							target = p.getCat();
						} else {
							target = "_";
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
