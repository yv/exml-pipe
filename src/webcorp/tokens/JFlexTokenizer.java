package webcorp.tokens;

import com.google.re2j.Pattern;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JFlexTokenizer implements TokenizerInterface {
    private final Pattern _abbrev_re;
    private final Pattern _ord_noun_re;
    private final Pattern _ord_gen_re;
    private final Pattern _lc_name_re;
    private final Pattern _pre_ord_re;
    private final Pattern _weak_abbrev_re;
    private final Pattern _sent_start_re;
	public static final Pattern konj_re = Pattern.compile("und|oder|bis");

	public static final int WORD = 0;
	public static final int NUMBER = 1;
	public static final int ABBREV = 2;
	public static final int WEAK_ABBREV = 3;

    private static Map<String, Pattern> compilePatterns(BufferedReader reader) throws IOException {
        Map<String, Pattern> result = new HashMap<>();
        StringBuffer buf=new StringBuffer();
        String s;
        String lastName=null;
        while ((s=reader.readLine())!=null) {
            if (s.startsWith(">>> ") || s.startsWith(">*> ")) {
                if (lastName!=null) {
                    Pattern a=Pattern.compile(buf.toString().substring(1));
                    result.put(lastName, a);
                }
                buf.setLength(0);
                lastName=s.substring(4);
            } else {
                if (s.length()>=1 && !s.startsWith("##")) {
                    // System.err.format("%s> %s\n",lastName,s);
                    buf.append('|').append(s);
                }
            }
        }
        if (lastName!=null) {
            Pattern a=Pattern.compile(buf.toString().substring(1));
            result.put(lastName, a);
        }
        return result;
    }

	public JFlexTokenizer(String lang) {
        try {
            BufferedReader in = Utils.openResourceIn(JFlexTokenizer.class,
                    "de_token_macros.txt", "UTF-8");
            Map<String, Pattern> patterns = compilePatterns(in);
            _abbrev_re = patterns.get("Abbrev");
            _lc_name_re = patterns.get("LCName");
            _ord_noun_re = patterns.get("OrdNoun");
            _ord_gen_re = patterns.get("OrdGen");
            _pre_ord_re = patterns.get("PreOrd");
            _weak_abbrev_re = patterns.get("WeakAbbrev");
            _sent_start_re = patterns.get("SentStart");
        } catch (IOException e) {
            throw new RuntimeException("Cannot load patterns", e);
        }
    }

	public int classifyToken(Token tok) {
	    if (tok.hasType(Token.TYPE_NUMBER)) {
	        return NUMBER;
        } else if (_abbrev_re.matches(tok.value)) {
            return ABBREV;
        } else if (_weak_abbrev_re.matches(tok.value)) {
	        return WEAK_ABBREV;
        } else {
            return WORD;
        }
	}

	private boolean isLower(String s) {
		if (Character.isLowerCase(s.charAt(0))) {
            return !_lc_name_re.matches(s);
		}
		return false;
	}

	/*
	 * if result[pos-1] is a number and result[pos] is a dot, is
	 * result[pos-1:pos+1] an ordinal number?
	 */
	private boolean plausibleOrdinal(int pos, List<Token> result) {
		String s_post;
		boolean num_post;
		if (pos < result.size() - 1) {
		    Token tokNext = result.get(pos + 1);
			s_post = tokNext.value;
			num_post = tokNext.hasType(Token.TYPE_NUMBER);
		} else {
			s_post = "*END*";
			num_post = false;
		}
		String s_pos = result.get(pos-1).value;
		int c_pos = s_pos.codePointAt(0);
		boolean is_roman = !(c_pos >= '0' && c_pos <= '9');
		String s_pre;
		if (pos >= 2) {
			s_pre = result.get(pos - 2).value;
		} else {
			s_pre = "*BEGIN*";
		}
		if ("?!:,/".contains(s_post)
                || _ord_noun_re.matches(s_post)
                || _pre_ord_re.matches(s_pre)) {
		    return true;
        }
		if (is_roman || s_pos.length() <= 2) {
            return isLower(s_post) || num_post ||
                    (s_pre.endsWith(".")
                            && result.get(pos - 2).hasType(Token.TYPE_NUMBER)
                            && !_sent_start_re.matches(s_post));
        } else {
		    return _ord_gen_re.matches(s_post);
        }
	}

	private boolean plausibleAbbrev(int pos, List<Token> result) {
		String s_post;
		if (pos < result.size() - 1) {
			s_post = result.get(pos + 1).value;
			// System.out.println("post:"+s_post);
		} else {
		    return true;
		}
		if (isLower(s_post)) {
			return true;
		} else if (_sent_start_re.matches(s_post)){
		    // Kategorie A. Der ...
            return false;
        }
		return true;
	}

	private boolean plausibleTrunc(int pos, List<Token> result) {
        String s_post;
        if (pos < result.size() - 1) {
            s_post = result.get(pos + 1).value;
            // System.out.println("post:"+s_post);
        } else {
            return false;
        }
        if (konj_re.matches(s_post) || ",".equals(s_post)) {
            return true;
        }
        return false;
    }

	/*
	 * needed: - attach clitic 's and ' to proper names (McDonald's, Disney's
	 * but not geht's hat's) - attach - to truncated items (-los, -bohrung) but
	 * not to non-truncated ones (-hat, -wird)?
	 */
	public List<Token> tokenize(String input, int offset) {
		List<Token> result = new ArrayList<Token>();
		TokenScanner scanner = new TokenScanner(new StringReader(input));
		Token tok;
        try {
            while ((tok = scanner.yylex()) != null) {
                result.add(tok);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // no tokens? no problem.
		if (result.size() == 0)
			return result;
		// Step 1: join abbreviations and TRUNC
		tok = result.get(0);
		for (int i = 1; i < result.size(); i++) {
			Token tokNext = result.get(i);
			boolean attach = false;
			if (tok.end == tokNext.start) {
                if (".".equals(tokNext.value)) {
                    int cls = classifyToken(tok);
                    // System.out.format("Classify: %s => %s\n",tok.value,cls);
                    switch (cls) {
                        case ABBREV:
                            attach = true;
                        case WEAK_ABBREV:
                            attach = plausibleAbbrev(i, result);
                            break;
                        case NUMBER:
                            attach = plausibleOrdinal(i, result);
                            break;
                    }
                } else if ("-".equals(tokNext.value)) {
                    attach = plausibleTrunc(i, result);
                }
                if (attach) {
                    tok.value = tok.value + tokNext.value;
                    tok.end = tokNext.end;
                    result.remove(i);
                    i--;
                } else {
                    tok = tokNext;
                }
            } else {
			    tok = tokNext;
            }
		}
		// Step 2: detect sentence boundaries
		for (int i = 0; i < result.size() - 1; i++) {
			if (result.get(i).hasType(Token.TYPE_PUNCT_S)) {
				result.get(i + 1).flags |= Token.FLAG_BOUNDARY;
			}
		}
		// Step 3: reattach some clitics (Hans', Disney's)
		tok = result.get(0);
		for (int i = 1; i < result.size(); i++) {
			Token tokNext = result.get(i);
			if (tok.end == tokNext.start && tokNext.value.charAt(0) == '\'') {
				boolean attach = false;
				if (tokNext.value.length() == 1) {
					char c = tok.value.charAt(tok.value.length() - 1);
					if (c == 'b' || c == 't' || c == 's') {
						attach = true;
					}
				} else if (tokNext.value.length() == 2
						&& (Character.toLowerCase(tokNext.value.charAt(1)) == 's')) {
					attach = Character.isUpperCase(tok.value.charAt(0));
				}
				// System.err.format("%s|%s\n", tok.value,tokNext.value);
				if (attach) {
					tok.value = tok.value + tokNext.value;
					tok.end = tokNext.end;
					result.remove(i);
					i--;
				} else {
					tok = tokNext;
				}
			} else {
				tok = tokNext;
			}
		}
		// Step 4: reattach some separated dash-compounds
		// (Telekomwimpel-|schwingenden)
		tok = result.get(0);
		for (int i = 1; i < result.size(); i++) {
			Token tokNext = result.get(i);
			if (tok.value.length()>3 && tok.value.endsWith("-")
					&& Character.isLetter(tokNext.value.charAt(0))
					&& !konj_re.matcher(tokNext.value).matches()) {
				tok.value = tok.value + tokNext.value;
				tok.end = tokNext.end;
				result.remove(i);
				i--;
			} else {
				tok = tokNext;
			}
		}
		return result;
	}
}
