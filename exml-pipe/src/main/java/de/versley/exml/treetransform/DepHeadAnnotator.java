package de.versley.exml.treetransform;

import exml.objects.NamedObject;
import exml.tueba.TuebaNodeMarkable;
import exml.tueba.TuebaTerminal;
import exml.tueba.util.SentenceTree;

/**
 * Assigns heads to constituents based on the dependency annotation
 * @author yannick
 *
 */
public class DepHeadAnnotator implements TreeTransformer {
	public TuebaTerminal projectHead(TuebaNodeMarkable nt) {
		//TODO implement find_heads_disc
		return null;
	}

	@Override
	public void transform(SentenceTree t) {
		for (NamedObject n: t.getRoots()) {
			try {
				TuebaNodeMarkable nt = (TuebaNodeMarkable)n;
				projectHead(nt);
			} catch (ClassCastException e) {
				// nothing to do
			}
		}
	}
}
