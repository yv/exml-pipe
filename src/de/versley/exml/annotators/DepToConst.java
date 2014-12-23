package de.versley.exml.annotators;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import weka.classifiers.Classifier;
import weka.core.Instances;
import de.versley.exml.pipe.SentenceTree;
import edu.berkeley.nlp.util.Lists;
import exml.objects.NamedObject;
import exml.tueba.TuebaDocument;
import exml.tueba.TuebaNodeMarkable;
import exml.tueba.TuebaTerminal;
import exml.tueba.TuebaTerminalSchema;

public class DepToConst implements Annotator {
	final static List<TuebaTerminal> EMPTY_LIST = Lists.fromArray(new TuebaTerminal[]{});
    private final Classifier classifier;
    private final Instances dataset;
	
	public DepToConst(String filename) {
		try {
			ObjectInputStream f = new ObjectInputStream(new FileInputStream(filename));
			classifier = (Classifier) f.readObject();
			dataset = (Instances) f.readObject();
		} catch (IOException | ClassNotFoundException ex) {
			throw new RuntimeException("Cannot load Weka classifier", ex);
		}
	}
	public static void extractDepChildren(
			List<TuebaTerminal> terms,
			List<List<TuebaTerminal>> left_deps,
			List<List<TuebaTerminal>> right_deps) {
		int offset = terms.get(0).getStart();
		for (int i = 0; i < terms.size(); i++) {
			left_deps.add(new ArrayList<TuebaTerminal>());
			right_deps.add(new ArrayList<TuebaTerminal>());
		}
		for (TuebaTerminal term: terms) {
			if (term.getSyn_parent() != null) {
				TuebaTerminal parent = (TuebaTerminal) term.getSyn_parent();
				if (parent.getStart() < term.getStart()) {
					left_deps.get(parent.getStart() - offset).add(term);
				} else {
					right_deps.get(parent.getStart() - offset).add(term);
				}
			}
		}
		for (int i = 0; i < terms.size(); i++) {
			Collections.reverse(left_deps.get(i));
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
			int offset = terms.get(0).getStart();
			extractDepChildren(terms, left_chlds, right_chlds);
			for (TuebaTerminal n: terms) {
				// make nodes
				// TODO feature extraction for node labeling
				List<TuebaTerminal> lchlds = left_chlds.get(n.getStart()-offset);
				List<TuebaTerminal> rchlds = right_chlds.get(n.getStart()-offset);
				List<TuebaTerminal> lsibs;
				List<TuebaTerminal> rsibs;
				TuebaTerminal p = (TuebaTerminal)n.getSyn_parent();
				if (p == null) {
					lsibs = rsibs = EMPTY_LIST;
				} else {
				lsibs = left_chlds.get(p.getStart()-offset);
				rsibs = right_chlds.get(p.getStart()-offset);
				}
				String[] feats = get_features(n,p,lchlds,rchlds,lsibs,rsibs);
				String node_label = classify_nodelabel(feats);
				TuebaNodeMarkable m = new TuebaNodeMarkable();
				m.setCat("X");
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
		// TODO actually use a WEKA classifier
		return "X";
	}

	private String[] get_features(TuebaTerminal n, TuebaTerminal p,
			List<TuebaTerminal> ldeps, List<TuebaTerminal> rdeps,
			List<TuebaTerminal> lsibs, List<TuebaTerminal> rsibs) {
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
		return new String[]{
				any_deps, any_kon,
				get_attr_idx(lsibs, 0, TuebaTerminalSchema.IDX_cat),
				get_attr_idx(rsibs, 0, TuebaTerminalSchema.IDX_cat),
				get_attr_idx(ldeps, 0, TuebaTerminalSchema.IDX_cat),
				get_attr_idx(rdeps, 0, TuebaTerminalSchema.IDX_cat)
		};
	}

	private String get_attr_idx(List<TuebaTerminal> lst, int i, int idxCat) {
		if (lst.size() <= i || i < 0) {
			return "_";
		} else {
			return (lst.get(i).getSlot(idxCat)).toString();
		}
	}

}
