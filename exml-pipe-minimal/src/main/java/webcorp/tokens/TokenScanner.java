package webcorp.tokens;

import java.io.IOException;

/**
 * This is an interface implemented by all JFlex-derived scanners.
 */
public interface TokenScanner {
    Token yylex() throws IOException;
}
