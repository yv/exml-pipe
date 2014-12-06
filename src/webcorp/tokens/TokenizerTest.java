package webcorp.tokens;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.incava.util.diff.Diff;
import org.incava.util.diff.Difference;

public class TokenizerTest {
	static void reportDiff(Difference d, List<String> tok1, List<String> tok2) {
		int delStart = d.getDeletedStart();
		int delEnd = d.getDeletedEnd();
		int addStart = d.getAddedStart();
		int addEnd = d.getAddedEnd();
		StringBuffer buf=new StringBuffer();
		buf.append("[");
		boolean needComma=false;
		for (int i=delStart;i<delEnd+1;i++) {
			if (needComma) buf.append(",");
			buf.append(tok1.get(i));
			needComma=true;
		}
		buf.append("],[");
		needComma=false;
		for (int i=addStart;i<addEnd+1;i++) {
			if (needComma) buf.append(",");
			buf.append(tok2.get(i));
			needComma=true;
		}
		buf.append("]");
		if (addEnd!=-1 &&
				".".equals(tok2.get(addEnd)) && addEnd+1<tok2.size()) {
			buf.append("/").append(tok2.get(addEnd+1));
		}
		if (delEnd!=-1 &&
				".".equals(tok1.get(delEnd)) && delEnd+1<tok1.size()) {
			buf.append("/").append(tok1.get(delEnd+1));
		}
		System.out.println(buf.toString());
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		try {
			BufferedReader r = new BufferedReader(new InputStreamReader(
					new FileInputStream(args[0]), Charset.forName("UTF-8")));
			/**TokenizerModel tok_model = new TokenizerModel(new FileInputStream("models/de-token.bin"));
			Tokenizer tokenizer=new TokenizerME(tok_model);*/
			DFATokenizer tokenizer=new DFATokenizer("de");
			ObjectMapper m = new ObjectMapper();
			String s;
			int nDiffs=0;
			int nFiles=0;
			while ((s = r.readLine()) != null) {
				JsonNode data = m.readValue(s, JsonNode.class);
				List<String> blocks = m.treeToValue(data.get("blocks"),
						ArrayList.class);
				List<String> tokens = m.treeToValue(data.get("words"),
						ArrayList.class);
				List<String> tokens_auto = new ArrayList<String>();
				List<Integer> sents_auto = new ArrayList<Integer>();
				for (String blk : blocks) {
					List<Token> toks=tokenizer.tokenize(blk, 0);
					int pos=0;
					for (Token tok: toks) {
						tokens_auto.add(tok.value);
						if (tok.isSentStart()) {
							sents_auto.add(pos);
						}
						pos+=1;
					}
				}
				Diff<String> diffs = new Diff<String>(tokens, tokens_auto);
				List<Difference> diffOut = diffs.diff();
				for (Difference d : diffOut) {
					reportDiff(d, tokens, tokens_auto);
				}
				nFiles+=1;
				nDiffs+=diffOut.size();
			}
			r.close();
			System.out.format("%d diffs over %d files",nDiffs,nFiles);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}
}
