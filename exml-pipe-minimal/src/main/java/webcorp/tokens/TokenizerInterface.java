package webcorp.tokens;

import java.util.List;

public interface TokenizerInterface {
	public List<Token> tokenize(String input, int offset);
}
