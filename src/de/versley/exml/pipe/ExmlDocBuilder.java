package de.versley.exml.pipe;

import webcorp.tokens.DFATokenizer;
import webcorp.tokens.Token;
import webcorp.tokens.TokenizerInterface;
import de.versley.exml.annotators.CoreNLPTokenizer;
import exml.MarkableLevel;
import exml.MissingObjectException;
import exml.tueba.TuebaDocument;
import exml.tueba.TuebaSentenceMarkable;
import exml.tueba.TuebaTerminal;

/**
 * Utility class to build EXML documents from formats containing raw text
 * (e.g., .txt, .pdf, .html)
 * @author yannick
 *
 */
public class ExmlDocBuilder {
	private TuebaDocument _doc;
	private TokenizerInterface _tok;
	private String _lang;
	private int _sent_no = 1;
	
	public ExmlDocBuilder(String language) {
		_doc = new TuebaDocument();
		_lang = language;
	}
	
	public TuebaDocument getDocument() {
		return _doc;
	}
	
	protected TokenizerInterface getTokenizer() {
		if (_tok == null) {
			if ("de".equals(_lang)) {
				_tok = new DFATokenizer(_lang);
			} else if ("en".equals(_lang)) {
				_tok = new CoreNLPTokenizer();
			}
		}
		return _tok;
	}
	
	public int addText(String text) {
		TokenizerInterface tokenizer = getTokenizer();
		TuebaDocument doc = _doc;
		MarkableLevel<TuebaSentenceMarkable> sentLevel = doc.sentences;
		int w_no = 1;
		int sent_start = doc.size();
		for (Token tok: tokenizer.tokenize(text, 0)) {
			if (tok.isSentStart() && doc.size()!=sent_start) {
				try {
					TuebaSentenceMarkable m_sent = sentLevel.addMarkable(sent_start, doc.size());
					m_sent.setXMLId(String.format("s%d",  _sent_no));
					_sent_no++;
					w_no = 1;
					sent_start = doc.size();
				} catch (MissingObjectException e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
			TuebaTerminal n = doc.createTerminal(tok.value);
			n.setXMLId(String.format("s%d_%d", _sent_no, w_no));
			doc.nameForObject(n);
			n.setWord(tok.value);
			w_no++;
		}
		if (doc.size()!=sent_start) {
			try {
				TuebaSentenceMarkable m_sent = sentLevel.addMarkable(sent_start, doc.size());
				m_sent.setXMLId(String.format("s%d",  _sent_no));
				_sent_no++;
				w_no = 1;
			} catch (MissingObjectException e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
		return doc.size();
	}
}
