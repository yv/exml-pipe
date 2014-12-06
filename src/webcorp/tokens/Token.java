package webcorp.tokens;

public final class Token {
	public int start;
	public int end;
	public int flags; // 1 = at start of block; 2 = abbrev/ordinal; 4 = at sentence boundary
	public String value;
	public String wsp_after;
	
	public boolean isSentStart() {
		return (flags&4)==4;
	}
	
	@Override
	public String toString() {
		return String.format("Token(%s)",value);
	}
}
