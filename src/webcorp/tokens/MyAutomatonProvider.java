package webcorp.tokens;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.AutomatonProvider;
import dk.brics.automaton.Datatypes;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.RunAutomaton;

/**
 * allows to specify automata declaratively in a file, which is automatically
 * compiled to Automaton and RunAutomaton instances on load.
 * 
 * The file format for the files is as follows:
 * The file consists of multiple regular expressions, preceded by the name
 * of the automaton these are compiled to. If the name is prefixed by &lt;&lt;&lt;
 * the automaton is compiled normally. If the name is prefixed by &lt;*&lt;
 * it is also compiled into a RunAutomaton (which is then stored along with the
 * NFA representations).
 * 
 * If the regular expression consists of multiple lines, the lines are
 * implicitly joined with "|" (i.e., putting an additional "|" will make
 * the automaton recognize the empty string).
 * @author yannickv
 *
 */
public class MyAutomatonProvider implements AutomatonProvider, Serializable {
	private static final long serialVersionUID = -3710554826183480265L;
	private final HashMap<String,Automaton> _automata=new HashMap<String,Automaton>();
	private final HashMap<String,RunAutomaton> _dfa=new HashMap<String,RunAutomaton>();
	
	public static final String[] wanted_cats={"Lu","Ll","Lo","L","Nd","P","Ps","Pe","Pi","Pf","Pc","Po","Z"};
	public MyAutomatonProvider() {
		for (String s: wanted_cats) {
			_automata.put(s,Datatypes.get(s));
		}
	}

	public void putAutomaton(String name, Automaton obj) {
		_automata.put(name,obj);
	}
	
	@Override
	public Automaton getAutomaton(String name) throws IOException {
		return _automata.get(name);
	}
	
	public RunAutomaton getRunAutomaton(String name) {
		if (_dfa.containsKey(name)) {
			return _dfa.get(name);
		} else if (!_automata.containsKey(name)) {
			return null;
		} else {
			RunAutomaton result=new RunAutomaton(_automata.get(name));
			_dfa.put(name, result);
			return result;
		}
	}
	
	public static MyAutomatonProvider compile(BufferedReader reader) throws IllegalArgumentException, IOException {
		String lastName=null;
		boolean wantDFA=false;
		MyAutomatonProvider result=new MyAutomatonProvider();
		StringBuffer buf=new StringBuffer();
		String s;
		while ((s=reader.readLine())!=null) {
			if (s.startsWith(">>> ") || s.startsWith(">*> ")) {
				if (lastName!=null) {
					Automaton a=new RegExp(buf.toString().substring(1)).toAutomaton(result);
					result.putAutomaton(lastName, a);
					if (wantDFA) {
						result._dfa.put(lastName, new RunAutomaton(a));
					}
				}
				buf.setLength(0);
				lastName=s.substring(4);
				wantDFA=(s.charAt(1)=='*');
			} else {
				if (s.length()>=1 && !s.startsWith("##")) {
					System.err.format("%s> %s\n",lastName,s);
					buf.append('|').append(s);
				}
			}
		}
		if (lastName!=null) {
			Automaton a=new RegExp(buf.toString().substring(1)).toAutomaton(result);
			result.putAutomaton(lastName, a);
			if (wantDFA) {
				result._dfa.put(lastName, new RunAutomaton(a));
			}			
		}
		return result;
	}
	
	public static MyAutomatonProvider load(String prefix) throws IOException {
		File objFile=new File(prefix+".obj.gz");
		if (!objFile.exists()) {
			// || objFile.lastModified()<txtFile.lastModified()) {
			MyAutomatonProvider result=compile(Utils.openResourceIn(prefix+".txt", "UTF-8"));
			try {
				ObjectOutputStream oos=Utils.openObjectOut(objFile.getPath());
				oos.writeObject(result);
				oos.close();
			} catch (IOException ex) {
				objFile.delete();
				throw ex;
			}
			return result;
		} else {
			try {
				ObjectInputStream ois=Utils.openObjectIn(objFile.getPath());
				MyAutomatonProvider result=(MyAutomatonProvider)ois.readObject();
				ois.close();
				return result;
			} catch (ClassNotFoundException ex) {
				throw new RuntimeException("Cannot read compiled automaton",ex);
			}
		}
	}
	
	public static void main(String[] args) {
		try {
			load(args[0]);
		} catch(Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}
}
