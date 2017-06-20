package de.versley.exml.annotators;

import edu.emory.mathcs.nlp.tokenization.EnglishTokenizer;
import edu.emory.mathcs.nlp.tokenization.Token;
import webcorp.tokens.TokenizerInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yannick on 18.06.17.
 */
public class NLP4JTokenizer implements TokenizerInterface {
    EnglishTokenizer tokenizer;

    public NLP4JTokenizer() {
        tokenizer = new EnglishTokenizer();
    }
    @Override
    public List<webcorp.tokens.Token> tokenize(String input, int offset) {
        List<List<Token>> sentences = tokenizer.segmentize(input);
        List<webcorp.tokens.Token> result = new ArrayList<>();
        for (List<Token> tokens: sentences) {
            boolean is_first = true;
            for (Token tok : tokens) {
                webcorp.tokens.Token wtok = new webcorp.tokens.Token();
                wtok.value = tok.getWordForm();
                wtok.start = tok.getStartOffset() + offset;
                wtok.end = tok.getEndOffset() + offset;
                if (is_first && wtok.start != 0) {
                    wtok.addFlag(webcorp.tokens.Token.FLAG_BOUNDARY);
                }
                result.add(wtok);
                is_first = false;
            }
        }
        return result;
    }
}
