package webcorp.tokens;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import dk.brics.automaton.AutomatonMatcher;
import dk.brics.automaton.RunAutomaton;

public class DFATokenizer implements TokenizerInterface {
	public final RunAutomaton _token_dfa;
	public final RunAutomaton _num_dfa;
	public final RunAutomaton _ord_noun_dfa;
	public final RunAutomaton _abbrev_dfa;
	public final RunAutomaton _lc_name_dfa;
	public final RunAutomaton _pre_ord_dfa;
	public final RunAutomaton _sent_dfa;
	public static final Pattern konj_re = Pattern.compile("und|oder|bis");

	public static final int WORD = 0;
	public static final int NUMBER = 1;
	public static final int ABBREV = 2;
	
	protected final MyAutomatonProvider _automata;

	public DFATokenizer(String lang) {
		try {
			_automata=MyAutomatonProvider.load(String.format("%s_token_model",lang));
			_token_dfa=_automata.getRunAutomaton("Token");
			_num_dfa=_automata.getRunAutomaton("Number");
			_ord_noun_dfa=_automata.getRunAutomaton("OrdNoun");
			_abbrev_dfa=_automata.getRunAutomaton("Abbrev");
			_lc_name_dfa=_automata.getRunAutomaton("LCName");
			_pre_ord_dfa=_automata.getRunAutomaton("PreOrd");
			_sent_dfa=_automata.getRunAutomaton("SentPunct");
		} catch (IOException ex) {
			throw new RuntimeException("Cannot load automata",ex);
		}
	}

	public int classifyToken(Token tok) {
		if (_num_dfa.run(tok.value)) {
			return NUMBER;
		}
		if (_abbrev_dfa.run(tok.value)) {
			return ABBREV;
		} else {
			return WORD;
		}
	}

	private boolean isLower(String s) {
		if (Character.isLowerCase(s.charAt(0))) {
			if (_lc_name_dfa.run(s)) {
				return false;
			} else {
				return true;
			}
		}
		return false;
	}

	/*
	 * if result[pos-1] is a number and result[pos] is a dot, is
	 * result[pos-1:pos+1] an ordinal number?
	 */
	private boolean plausibleOrdinal(int pos, List<Token> result) {
		String s_post;
		if (pos < result.size() - 1) {
			s_post = result.get(pos + 1).value;
		} else {
			s_post = "*END*";
		}
		String s_pre;
		if (pos >= 2) {
			s_pre = result.get(pos - 2).value;
		} else {
			s_pre = "*BEGIN*";
		}
		if (isLower(s_post) || "?!:,/".contains(s_post)
				|| _ord_noun_dfa.run(s_post)) {
			return true;
		} else if (_pre_ord_dfa.run(s_pre)) {
			return true;
		}
		return false;
	}

	private boolean plausibleAbbrev(int pos, List<Token> result) {
		String s_post;
		if (pos < result.size() - 1) {
			s_post = result.get(pos + 1).value;
			// System.out.println("post:"+s_post);
		} else {
			s_post = "*END*";
		}
		if (isLower(s_post)) {
			return true;
		}
		return true; // false;
	}

	/*
	 * needed: - attach clitic 's and ' to proper names (McDonald's, Disney's
	 * but not geht's hat's) - attach - to truncated items (-los, -bohrung) but
	 * not to non-truncated ones (-hat, -wird)?
	 */
	public List<Token> tokenize(String input, int offset) {
		List<Token> result = new ArrayList<Token>();
		AutomatonMatcher m = _token_dfa.newMatcher(input);
		while (m.find()) {
			Token tok = new Token();
			tok.value = m.group(0);
			if (!_token_dfa.run(tok.value)) {
				System.err.println("Invalid token:" + tok.value);
			}
			// tok.wsp_after = m.group(2);
			tok.start = m.start() + offset;
			tok.end = m.end() + offset;
			result.add(tok);
		}
		// no tokens? no problem.
		if (result.size() == 0)
			return result;
		// Step 1: join abbreviations
		Token tok = result.get(0);
		for (int i = 1; i < result.size(); i++) {
			Token tokNext = result.get(i);
			boolean attach = false;
			if (tok.end == tokNext.start && ".".equals(tokNext.value)) {
				int cls = classifyToken(tok);
				// System.out.format("Classify: %s => %s\n",tok.value,cls);
				switch (cls) {
				case ABBREV:
					attach = plausibleAbbrev(i, result);
					break;
				case NUMBER:
					attach = plausibleOrdinal(i, result);
					break;
				}
			}
			if (attach) {
				tok.value = tok.value + tokNext.value;
				tok.end = tokNext.end;
				tok.wsp_after = tokNext.wsp_after;
				result.remove(i);
				i--;
			} else {
				tok = tokNext;
			}
		}
		// Step 2: detect sentence boundaries
		for (int i = 0; i < result.size() - 1; i++) {
			if (_sent_dfa.run(result.get(i).value)) {
				result.get(i + 1).flags |= 4;
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
					tok.wsp_after = tokNext.wsp_after;
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
				tok.wsp_after = tokNext.wsp_after;
				result.remove(i);
				i--;
			} else {
				tok = tokNext;
			}
		}
		return result;
	}

	static private String[] test_tokens = { "(Musik-)Geschichte",
			"Anti-Abtreibungs-Gesetz", "öffentlich-rechtlichen", "§218-Gesetz" };

	public void run_test() {
		for (String s : test_tokens) {
			AutomatonMatcher m = _token_dfa.newMatcher(s);
			boolean f = m.find();
			if (!f) {
				System.err.println("Not recognized: " + s);
				continue;
			}
			if (m.start() != 0 || m.end() != s.length()) {
				System.err.format("Recognized %s, wanted %s\n", m.group(), s);
				continue;
			}
		}
	}

	public static void main(String[] args) {
		DFATokenizer tok = new DFATokenizer("de");
		tok.run_test();
		List<Token> toks = tok
				.tokenize(
						"Peter: Nach §218 und dem 12. 1000-Meter-Lauf, sagte die 16jährige, wäre alles- (naja, fast alles) mit dem \"Grenzenlos\"-Modell OK gewesen, mit ca. 12 Metern/Sek. Wissen's, das ist Charly's Traum, und Jonas'...",
						0);
		for (Token s : toks) {
			System.out.println(s.value);
		}
	}
}
