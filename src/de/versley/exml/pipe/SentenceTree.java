package de.versley.exml.pipe;

import java.util.ArrayList;
import java.util.List;

import exml.objects.NamedObject;
import exml.tueba.TuebaDocument;
import exml.tueba.TuebaNodeMarkable;
import exml.tueba.TuebaSentenceMarkable;
import exml.tueba.TuebaTerminal;

public class SentenceTree {
	private List<NamedObject> _roots;
	private List<TuebaTerminal> _terminals;
	private TuebaDocument _doc;
	private TuebaSentenceMarkable _sent;

	public SentenceTree(List<NamedObject> roots, List<TuebaTerminal> terminals,
			TuebaSentenceMarkable sent, TuebaDocument doc) {
		_roots = roots;
		_terminals = terminals;
		_sent = sent;
		_doc = doc;
	}
	
	public List<NamedObject> getRoots() {
		return _roots;
	}
	public void setRoots(List<NamedObject> _roots) {
		this._roots = _roots;
	}
	public List<TuebaTerminal> getTerminals() {
		return _terminals;
	}
	public void setTerminals(List<TuebaTerminal> _terminals) {
		this._terminals = _terminals;
	}

	public static List<SentenceTree> getTrees(TuebaDocument doc) {
		List<SentenceTree> result = new ArrayList<SentenceTree>();
		for (TuebaSentenceMarkable sent: doc.sentences.getMarkables()) {
			List<NamedObject> roots = new ArrayList<NamedObject>();
			List<TuebaTerminal> terminals = new ArrayList<TuebaTerminal>();
			for (int i = sent.getStart(); i < sent.getEnd(); i++) {
				TuebaTerminal node = doc.getTerminal(i);
				terminals.add(node);
				if (node.getParent() == null) {
					roots.add(node);
				}
			}
			for (TuebaNodeMarkable node: doc.nodes.getMarkablesInRange(sent.getStart(), sent.getEnd())) {
				if (node.getParent() == null) {
					roots.add(node);
				}
			}
			// TODO sort roots by start
			// TODO do we want to have a (local) node table?
			result.add(new SentenceTree(roots, terminals, sent, doc));
		}
		return result;
	}
	
	public void reassignParents() {
		reassignParents(_roots, null);
	}
	
	public void reassignParents(List<NamedObject> nodes, TuebaNodeMarkable parent) {
		for (NamedObject node: nodes) {
			try {
				TuebaNodeMarkable n = (TuebaNodeMarkable)node;
				n.setParent(parent);
				reassignParents(n.getChildren(), n);
			} catch (ClassCastException ex) {
				TuebaTerminal n = (TuebaTerminal)node;
				n.setParent(parent);
			}
		}
	}
	
	/**
	 * given roots and their yields, reassigns the spans of the nodes
	 */
	public void reassignSpans() {
		reassignSpans(_roots, null);
	}
	
	private void reassignSpans(List<NamedObject> nodes, TuebaNodeMarkable parent) {
		int start=Integer.MAX_VALUE;
		int end=Integer.MIN_VALUE;
		for (NamedObject node: nodes) {
			try {
				TuebaNodeMarkable n = (TuebaNodeMarkable)node;
				reassignSpans(n.getChildren(), n);
				if (n.getStart() < start) {
					start = n.getStart();
				}
				if (n.getEnd() > end) {
					end = n.getEnd();
				}
			} catch (ClassCastException ex) {
				TuebaTerminal n = (TuebaTerminal)node;
				n.setParent(parent);
				if (n.getStart() < start) {
					start = n.getStart();
				}
				if (n.getEnd() > end) {
					end = n.getEnd();
				}
			}
		}
		if (parent != null) {
			parent.setStart(start);
			parent.setEnd(end);
		}
	}

	/**
	 * given the roots of this SentenceTree, enters the nodes
	 * into the node markable and removes the others
	 */
	public void replaceNodes() {
		//TODO: remove old nodes
		insertNodes(_roots, 500);
	}
	
	protected int insertNodes(List<NamedObject> nodes, int node_num) {
		// System.err.println("insert nodes"+ nodes);
		for (NamedObject node: nodes) {
			try {
				TuebaNodeMarkable n = (TuebaNodeMarkable)node;
				_doc.nodes.addMarkable(n);
				node_num = insertNodes(n.getChildren(), node_num);
				if (n.getXMLId() == null) {
					n.setXMLId(String.format("%s_%d", _sent.getXMLId(), node_num));
					node_num += 1;
				}
			} catch (ClassCastException ex) {
				TuebaTerminal n = (TuebaTerminal)node;
				// nothing to do here
			}
		}
		return node_num;
	}
}
