%%
%class TestLexer
%unicode
%char
%type int
%eofval{
 return -1;
%eofval}

%debug

ISODate=(20|19)\d\d-\d\d-\d\d
AlNum=\w
Prefix={AlNum}+(\.|[:/])?({AlNum}+)?-|\({AlNum}+-\)|\"{AlNum}+\"-
%%
{ISODate} / ![^0-9] { return 0; }
\d+ { return 3; }
{Prefix}*\p{L}+ { return 4; }
\p{L}+ { return 1; }
\s+ { /* skip */ }
.  { return 2; }