package de.versley.exml.annotators;

import java.util.ArrayList;
import java.util.List;

import de.versley.exml.pipe.SentenceTree;
import exml.objects.NamedObject;
import exml.tueba.TuebaDocument;
import exml.tueba.TuebaNodeMarkable;
import exml.tueba.TuebaTerminal;

public class DepToConst implements Annotator {

	@Override
	public void annotate(TuebaDocument doc) {
		List<SentenceTree> trees = SentenceTree.getTrees(doc);
		for (SentenceTree t: trees) {
			List<TuebaNodeMarkable> nodes = new ArrayList<TuebaNodeMarkable>();
			List<TuebaTerminal> terms = t.getTerminals();
			//TODO create list of left/right dependents
			for (TuebaTerminal n: terms) {
				// make nodes
				// TODO feature extraction for node labeling
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

}
