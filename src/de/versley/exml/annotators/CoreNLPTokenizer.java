package de.versley.exml.annotators;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import webcorp.tokens.Token;
import webcorp.tokens.TokenizerInterface;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.WordToSentenceProcessor;

public class CoreNLPTokenizer implements TokenizerInterface {
	public String language = "en";
	private WordToSentenceProcessor<CoreLabel> wts =
			new WordToSentenceProcessor<CoreLabel>();

	@Override
	public List<Token> tokenize(String input, int offset) {
		Tokenizer <CoreLabel> tokenizer;
		List<CoreLabel> tokens;
		List<Token> result = new ArrayList<Token>();
		if ("en".equals(language)) {
			tokenizer = new PTBTokenizer<CoreLabel>(new StringReader(input),
					new CoreLabelTokenFactory(), "");
		} else {
			throw new IllegalArgumentException("No model for language: "+language);
		}
		tokens = tokenizer.tokenize();
		List<List<CoreLabel>> sentences = wts.process(tokens);
		for (List<CoreLabel> sent: sentences) {
			boolean is_first = true;
			for (CoreLabel tok: sent) {
				Token tt = new Token();
				tt.value = tok.getString(TextAnnotation.class);
				tt.start = tok.beginPosition();
				tt.end = tok.endPosition();
				tt.wsp_after = tok.after();
				result.add(tt);
				if (is_first) {
					tt.flags |= Token.SENT_START;
				}
				is_first = false;
			}
		}
		return result;
	}

}
