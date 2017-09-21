package de.versley.exml.annotators;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.google.re2j.Pattern;
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
	public final String language = "en";

	private static Pattern gluedToken=Pattern.compile("[a-z]+\\.[A-Z][a-z]*");

	private WordToSentenceProcessor<CoreLabel> wts =
			new WordToSentenceProcessor<CoreLabel>();

	private static List<CoreLabel> splitGluedCore(List<CoreLabel> input, CoreLabelTokenFactory factory) {
		List<CoreLabel> result = new ArrayList<CoreLabel>(input.size());
		for (CoreLabel tok: input) {
			String s = tok.getString(TextAnnotation.class);
			if (gluedToken.matches(s)) {
				// need to split
				int splitPoint = s.indexOf('.');
				CoreLabel tok1 = factory.makeToken(s.substring(0, splitPoint), tok.beginPosition(), splitPoint);
				CoreLabel tok2 = factory.makeToken(".", tok.beginPosition()+splitPoint, 1);
				String s3 = s.substring(splitPoint+1);
				CoreLabel tok3 = factory.makeToken(s3,
						tok.beginPosition()+splitPoint+1, s3.length());
				result.add(tok1);
				result.add(tok2);
				result.add(tok3);
			} else {
				result.add(tok);
			}
		}
		return result;
	}

	@Override
	public List<Token> tokenize(String input, int offset) {
		Tokenizer <CoreLabel> tokenizer;
		List<CoreLabel> tokens;
		List<Token> result = new ArrayList<Token>();
		CoreLabelTokenFactory factory = new CoreLabelTokenFactory();
		if ("en".equals(language)) {
			tokenizer = new PTBTokenizer<CoreLabel>(new StringReader(input),
					factory, "");
		} else {
			throw new IllegalArgumentException("No model for language: "+language);
		}
		tokens = tokenizer.tokenize();
		tokens = splitGluedCore(tokens, factory);
		List<List<CoreLabel>> sentences = wts.process(tokens);
		for (List<CoreLabel> sent: sentences) {
			boolean is_first = true;
			for (CoreLabel tok: sent) {
				Token tt = new Token();
				tt.value = tok.getString(TextAnnotation.class);
				tt.start = tok.beginPosition();
				tt.end = tok.endPosition();
				result.add(tt);
				if (is_first && tt.start != 0) {
					tt.addFlag(Token.FLAG_BOUNDARY);
				}
				is_first = false;
			}
		}
		return result;
	}

}
