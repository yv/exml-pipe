package de.versley.exml.treetransform;

import de.versley.exml.pipe.SentenceTree;
import exml.objects.NamedObject;
import exml.tueba.TuebaNodeMarkable;
import exml.tueba.TuebaTerminal;

public class NodeToFunction implements TreeTransformer {

	@Override
	public void transform(SentenceTree t) {
		for (NamedObject n: t.getRoots()) {
			try {
				transformNT((TuebaNodeMarkable) n);
			} catch (ClassCastException ex) {
				transformTerm((TuebaTerminal) n);
			}
		}
	}

	private void transformTerm(TuebaTerminal n) {
		String s = n.getCat();
		int posn = s.indexOf(":");
		if (posn != -1) {
			String ncat = s.substring(0, posn);
			ncat = ncat.replace("_", "-");
			String nfunc = s.substring(posn+1);
			nfunc = nfunc.replace("_", "-");
			n.setCat(ncat);
			n.setEdge_label(nfunc);
		}
	}

	private void transformNT(TuebaNodeMarkable n) {
		String s = n.getCat();
		int posn = s.indexOf(":");
		if (posn != -1) {
			String ncat = s.substring(0, posn);
			ncat = ncat.replace("_", "-");
			String nfunc = s.substring(posn+1);
			nfunc = nfunc.replace("_", "-");
			n.setCat(ncat);
			n.setFunc(nfunc);
		}
		for (NamedObject chld: n.getChildren()) {
			try {
				transformNT((TuebaNodeMarkable) chld);
			} catch (ClassCastException ex) {
				transformTerm((TuebaTerminal) chld);
			}
		}
	}
}
