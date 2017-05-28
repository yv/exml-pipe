package webcorp.tokens;

public final class Token {
    public static final int TYPE_DEFAULT=0;
    public static final int TYPE_NUMBER=1;
    public static final int TYPE_PUNCT_S=2;     // sentence boundary punctuation
    public static final int TYPE_DOT=3;         // dot
    public static final int TYPE_PUNCT_QUOTE=4; // quote
    public static final int TYPE_PUNCT_DASH=5;  // dash
	public final static int FLAG_BOUNDARY=8;    // at sentence boundary

	public int start;
	public int end;
	public int flags; // 1 = at start of block; 2 = abbrev/ordinal; 4 = at sentence boundary
	public String value;
	public static final int SENT_START = 4;

	public boolean hasType(int type) {
	    return (flags & 7) == type;
    }
	public boolean hasFlag(int flag) {
	    return (flags & flag) == flag;
    }
	public boolean isSentStart() {
		return hasFlag(FLAG_BOUNDARY);
	}
	
	@Override
	public String toString() {
		return String.format("Token(%s)",value);
	}
}
