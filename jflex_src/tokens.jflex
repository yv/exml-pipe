package webcorp.tokens;

import static webcorp.tokens.Token.TYPE_NUMBER;
import static webcorp.tokens.Token.TYPE_PUNCT_S;
import static webcorp.tokens.Token.TYPE_PUNCT_DASH;
import static webcorp.tokens.Token.TYPE_PUNCT_QUOTE;

%%
%class TokenScanner
%unicode
%char
%type Token

%{

  private Token mkToken(int flags) {
    Token result = new Token();
    result.flags = flags;
    result.start = yychar;
    result.end = yychar + yylength();
    result.value = yytext();
    return result;
  }

  private Token mkToken() {
    return mkToken(0);
  }
%}

AlNum=(\p{L}|\p{Nd})
SQuote=['\u2018\u2019]
DQuote=[\"\u201c\u201d\u201e\u201f\u00ab\u00bb]|``|''|´´
MixedIdentifier=[A-Z0-9]{2,}[A-Z0-9]+
XY=§\d+|\d+[x:/]\d+|\p{Lu}+\d+|{AlNum}+([_-]{AlNum}+)*\.(jpe?g|gif|docx?|xlsx?|png)|{MixedIdentifier}
ArabicNumber=\p{Nd}+(\.\p{Nd}{3})*(,\p{Nd}+)?
RomanNumber=II|III|IV|V|X+V?I*|VI+
ISODate=20\d\d-[0-1]\d-[0-3]\d
KnownDomain=((\w+-)*\w+)\.(com|net|org|gov|de|at|info|biz|ly|tv|ag)
PathElement=\w+([-_]\w+)*
UrlPath=\/({PathElement}\/)*({PathElement}(\.\w+)?)
UserName=(\p{L}+_)*\p{L}+\d*
Prefix={AlNum}+(\.|[:/])?({AlNum}+)?-|\({AlNum}+-\)|{DQuote}{AlNum}+{DQuote}-|{XY}-
DigitWord=(19|20)?[0-9][0-9]er\p{L}*
DigitName=1822direkt|3dfx|t@x|h2g2|t3n|m4e|([Nn]eo|nd)4[Jj]
%%

{SQuote}(s|n|se) / [^\w]        { return mkToken(); }
(d|dell|nell){SQuote}           { return mkToken(); }
{DQuote}                        { return mkToken(TYPE_PUNCT_QUOTE); }
@{UserName}               { return mkToken(); }
@{DigitName}              { return mkToken(); }
#\w+                      { return mkToken(); }
{ISODate} / ![0-9]        { return mkToken(); }
{ArabicNumber} / [^0-9]   { return mkToken(TYPE_NUMBER); }
{RomanNumber} / \.        { return mkToken(TYPE_NUMBER); }
\p{Nd}+                   { return mkToken(TYPE_NUMBER); }
{XY}                      { return mkToken(); }
\w+(\.\w+)*@{KnownDomain} { return mkToken(); }
(https?:\/\/)?(www\.)?{KnownDomain}{UrlPath}?  { return mkToken(); }
{Prefix}*(\p{L}+|{DigitWord}) { return mkToken(); }
\d+-?\p{Lu}?\p{Ll}+       { return mkToken(); }
[!\?]+ { return mkToken(TYPE_PUNCT_S); }
\.     { return mkToken(TYPE_PUNCT_S); }
:      { return mkToken(TYPE_PUNCT_S); }
\.\.+  { return mkToken(TYPE_PUNCT_S); }
\p{Pd} { return mkToken(TYPE_PUNCT_DASH); }
\p{P}  { return mkToken(); }
\s+ { /* skip */ }
.      { System.err.println("Weird character:"+yytext()); return mkToken(); }