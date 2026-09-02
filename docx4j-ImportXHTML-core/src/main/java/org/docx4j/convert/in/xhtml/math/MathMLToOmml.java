/*
   Licensed to Plutext Pty Ltd under one or more contributor license agreements.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.

   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.docx4j.convert.in.xhtml.math;

import java.util.ArrayList;
import java.util.List;

import org.docx4j.math.CTAcc;
import org.docx4j.math.CTAccPr;
import org.docx4j.math.CTBorderBox;
import org.docx4j.math.CTChar;
import org.docx4j.math.CTD;
import org.docx4j.math.CTDPr;
import org.docx4j.math.CTF;
import org.docx4j.math.CTFPr;
import org.docx4j.math.CTFType;
import org.docx4j.math.CTGroupChr;
import org.docx4j.math.CTGroupChrPr;
import org.docx4j.math.CTLimLoc;
import org.docx4j.math.CTLimLow;
import org.docx4j.math.CTLimUpp;
import org.docx4j.math.CTM;
import org.docx4j.math.CTMPr;
import org.docx4j.math.CTMR;
import org.docx4j.math.CTNary;
import org.docx4j.math.CTNaryPr;
import org.docx4j.math.CTOMath;
import org.docx4j.math.CTOMathArg;
import org.docx4j.math.CTPhant;
import org.docx4j.math.CTR;
import org.docx4j.math.CTRPR;
import org.docx4j.math.CTRad;
import org.docx4j.math.CTRadPr;
import org.docx4j.math.CTSPre;
import org.docx4j.math.CTSSub;
import org.docx4j.math.CTSSubSup;
import org.docx4j.math.CTSSup;
import org.docx4j.math.CTScript;
import org.docx4j.math.CTStyle;
import org.docx4j.math.CTText;
import org.docx4j.math.CTTopBot;
import org.docx4j.math.ObjectFactory;
import org.docx4j.math.STFType;
import org.docx4j.math.STLimLoc;
import org.docx4j.math.STScript;
import org.docx4j.math.STStyle;
import org.docx4j.math.STTopBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Converts a Presentation-MathML {@code <math>} DOM element to Word OMML
 * ({@code org.docx4j.math.CTOMath}), natively in Java — no XSLT, so no
 * dependency on Microsoft's non-redistributable {@code MML2OMML.XSL}. The
 * inverse of docx4j-core's {@code OmmlToMathML}; clean-room from the W3C MathML
 * spec and ECMA-376 §22.1. See CR-math-omml-mathml.
 *
 * <p>Lenient by design: an unrecognised element is descended into rather than
 * rejected, so an unusual equation degrades rather than failing the import.</p>
 *
 * @since 17.0.3
 */
public class MathMLToOmml {

	private static final Logger log = LoggerFactory.getLogger(MathMLToOmml.class);

	private final ObjectFactory f = new ObjectFactory();

	/** Big operators that take limits: n-ary in OMML. */
	private static final String NARY_CHARS = "∑∏∐∫∬∭∮∯∰∱∲∳⋀⋁⋂⋃⨀⨁⨂⨃⨄⨅⨆";
	/** Group characters (braces/brackets) spanning a base: OMML groupChr. */
	private static final String GROUP_CHARS = "⏞⏟⎴⎵︷︸⏜⏝⏠⏡";

	public CTOMath convert(Element math) {
		CTOMath oMath = f.createCTOMath();
		appendChildren(oMath.getEGOMathElements(), math);
		return oMath;
	}

	// --------------------------------------------------------------- dispatch

	private void appendChildren(List<Object> target, Element parent) {
		for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				appendElement(target, (Element) n);
			}
		}
	}

	private void appendElement(List<Object> target, Element e) {
		String name = localName(e);
		switch (name) {
		case "mi":
		case "mn":
		case "mo":
			target.add(f.createCTOMathArgR(run(tokenText(e), variant(e), false)));
			break;
		case "mtext":
			target.add(f.createCTOMathArgR(run(text(e), null, true)));
			break;
		case "ms":
			target.add(f.createCTOMathArgR(run("\"" + text(e) + "\"", null, true)));
			break;
		case "mspace":
		case "mglyph":
			break; // no OMML equivalent
		case "mrow":
		case "mstyle":
		case "merror":
		case "mpadded":
			appendChildren(target, e); // transparent grouping
			break;
		case "mfrac":
			target.add(f.createCTOMathArgF(frac(e)));
			break;
		case "msqrt":
			target.add(f.createCTOMathArgRad(sqrt(e)));
			break;
		case "mroot":
			target.add(f.createCTOMathArgRad(root(e)));
			break;
		case "msub":
			target.add(f.createCTOMathArgSSub(sSub(e)));
			break;
		case "msup":
			target.add(f.createCTOMathArgSSup(sSup(e)));
			break;
		case "msubsup":
			target.add(f.createCTOMathArgSSubSup(sSubSup(e)));
			break;
		case "munder":
			appendUnderOver(target, e, true, false);
			break;
		case "mover":
			appendUnderOver(target, e, false, true);
			break;
		case "munderover":
			appendUnderOver(target, e, true, true);
			break;
		case "mfenced":
			target.add(f.createCTOMathArgD(fenced(e)));
			break;
		case "mtable":
			target.add(f.createCTOMathArgM(matrix(e)));
			break;
		case "mmultiscripts":
			appendMultiscripts(target, e);
			break;
		case "mphantom":
			target.add(f.createCTOMathArgPhant(phantom(e)));
			break;
		case "menclose":
			appendEnclose(target, e);
			break;
		default:
			log.debug("MathML element <{}> not mapped; descending", name);
			appendChildren(target, e);
		}
	}

	// -------------------------------------------------------------- token run

	private CTR run(String value, String mathvariant, boolean normalText) {
		CTR r = f.createCTR();
		CTRPR rPr = runProperties(mathvariant, normalText);
		if (rPr != null) {
			r.getContent().add(f.createCTRRPrMath(rPr));
		}
		CTText t = f.createCTText();
		t.setValue(value);
		t.setSpace("preserve");
		r.getContent().add(f.createCTRTMath(t));
		return r;
	}

	private CTRPR runProperties(String mathvariant, boolean normalText) {
		if (normalText) {
			CTRPR rPr = f.createCTRPR();
			rPr.setNor(f.createCTOnOff());
			return rPr;
		}
		if (mathvariant == null) {
			return null;
		}
		STStyle sty = null;
		STScript scr = null;
		switch (mathvariant) {
		case "normal":                   sty = STStyle.P; break;
		case "bold":                     sty = STStyle.B; break;
		case "italic":                   sty = STStyle.I; break;
		case "bold-italic":              sty = STStyle.BI; break;
		case "double-struck":            scr = STScript.DOUBLE_STRUCK; break;
		case "script":                   scr = STScript.SCRIPT; break;
		case "bold-script":              scr = STScript.SCRIPT; sty = STStyle.B; break;
		case "fraktur":                  scr = STScript.FRAKTUR; break;
		case "bold-fraktur":             scr = STScript.FRAKTUR; sty = STStyle.B; break;
		case "sans-serif":               scr = STScript.SANS_SERIF; break;
		case "bold-sans-serif":          scr = STScript.SANS_SERIF; sty = STStyle.B; break;
		case "sans-serif-italic":        scr = STScript.SANS_SERIF; sty = STStyle.I; break;
		case "sans-serif-bold-italic":   scr = STScript.SANS_SERIF; sty = STStyle.BI; break;
		case "monospace":                scr = STScript.MONOSPACE; break;
		default:
			return null;
		}
		CTRPR rPr = f.createCTRPR();
		if (sty != null) {
			CTStyle s = f.createCTStyle();
			s.setVal(sty);
			rPr.setSty(s);
		}
		if (scr != null) {
			CTScript s = f.createCTScript();
			s.setVal(scr);
			rPr.setScr(s);
		}
		return rPr;
	}

	// -------------------------------------------------------------- structures

	private CTF frac(Element e) {
		CTF frac = f.createCTF();
		List<Element> kids = childElements(e);
		String linethickness = attr(e, "linethickness");
		String bevelled = attr(e, "bevelled");
		if ("true".equals(bevelled)) {
			frac.setFPr(fPr(STFType.SKW));
		} else if ("0".equals(linethickness) || "0pt".equals(linethickness)) {
			frac.setFPr(fPr(STFType.NO_BAR));
		}
		frac.setNum(argFrom(kids, 0));
		frac.setDen(argFrom(kids, 1));
		return frac;
	}

	private CTFPr fPr(STFType type) {
		CTFPr pr = f.createCTFPr();
		CTFType t = f.createCTFType();
		t.setVal(type);
		pr.setType(t);
		return pr;
	}

	private CTRad sqrt(Element e) {
		CTRad rad = f.createCTRad();
		CTRadPr pr = f.createCTRadPr();
		pr.setDegHide(f.createCTOnOff());
		rad.setRadPr(pr);
		rad.setDeg(f.createCTOMathArg());          // empty
		rad.setE(argFromChildren(e));
		return rad;
	}

	private CTRad root(Element e) {
		CTRad rad = f.createCTRad();
		List<Element> kids = childElements(e);
		rad.setE(argFrom(kids, 0));                // base
		rad.setDeg(argFrom(kids, 1));              // index
		return rad;
	}

	private CTSSub sSub(Element e) {
		CTSSub s = f.createCTSSub();
		List<Element> kids = childElements(e);
		s.setE(argFrom(kids, 0));
		s.setSub(argFrom(kids, 1));
		return s;
	}

	private CTSSup sSup(Element e) {
		CTSSup s = f.createCTSSup();
		List<Element> kids = childElements(e);
		s.setE(argFrom(kids, 0));
		s.setSup(argFrom(kids, 1));
		return s;
	}

	private CTSSubSup sSubSup(Element e) {
		CTSSubSup s = f.createCTSSubSup();
		List<Element> kids = childElements(e);
		s.setE(argFrom(kids, 0));
		s.setSub(argFrom(kids, 1));
		s.setSup(argFrom(kids, 2));
		return s;
	}

	/** munder / mover / munderover — an n-ary, an accent, a group char, or a limit. */
	private void appendUnderOver(List<Object> target, Element e, boolean under, boolean over) {
		List<Element> kids = childElements(e);
		Element base = kids.isEmpty() ? null : kids.get(0);
		String baseChar = singleMoChar(base);

		if (baseChar != null && NARY_CHARS.indexOf(baseChar) >= 0) {
			CTNary nary = f.createCTNary();
			CTNaryPr pr = f.createCTNaryPr();
			pr.setChr(chr(baseChar));
			CTLimLoc loc = f.createCTLimLoc();
			loc.setVal(STLimLoc.UND_OVR);
			pr.setLimLoc(loc);
			if (!under) { pr.setSubHide(f.createCTOnOff()); }
			if (!over)  { pr.setSupHide(f.createCTOnOff()); }
			nary.setNaryPr(pr);
			nary.setSub(under ? argFrom(kids, 1) : f.createCTOMathArg());
			nary.setSup(over ? argFrom(kids, under ? 2 : 1) : f.createCTOMathArg());
			nary.setE(f.createCTOMathArg());
			target.add(f.createCTOMathArgNary(nary));
			return;
		}

		// accent (combining mark over the base)
		if (over && !under && "true".equals(attr(e, "accent"))) {
			CTAcc acc = f.createCTAcc();
			String accChar = singleMoChar(kids.size() > 1 ? kids.get(1) : null);
			if (accChar != null) {
				CTAccPr pr = f.createCTAccPr();
				pr.setChr(chr(accChar));
				acc.setAccPr(pr);
			}
			acc.setE(argFrom(kids, 0));
			target.add(f.createCTOMathArgAcc(acc));
			return;
		}

		// group character (brace/bracket spanning the base)
		String scriptChar = singleMoChar(kids.size() > 1 ? kids.get(under && over ? 1 : 1) : null);
		if (scriptChar != null && GROUP_CHARS.indexOf(scriptChar) >= 0 && !(under && over)) {
			CTGroupChr gc = f.createCTGroupChr();
			CTGroupChrPr pr = f.createCTGroupChrPr();
			pr.setChr(chr(scriptChar));
			CTTopBot pos = f.createCTTopBot();
			pos.setVal(over ? STTopBot.TOP : STTopBot.BOT);
			pr.setPos(pos);
			gc.setGroupChrPr(pr);
			gc.setE(argFrom(kids, 0));
			target.add(f.createCTOMathArgGroupChr(gc));
			return;
		}

		if (under && over) {
			// nest: lower limit then upper limit
			CTLimLow low = f.createCTLimLow();
			low.setE(argFrom(kids, 0));
			low.setLim(argFrom(kids, 1));
			CTLimUpp upp = f.createCTLimUpp();
			CTOMathArg inner = f.createCTOMathArg();
			inner.getEGOMathElements().add(f.createCTOMathArgLimLow(low));
			upp.setE(inner);
			upp.setLim(argFrom(kids, 2));
			target.add(f.createCTOMathArgLimUpp(upp));
		} else if (under) {
			CTLimLow low = f.createCTLimLow();
			low.setE(argFrom(kids, 0));
			low.setLim(argFrom(kids, 1));
			target.add(f.createCTOMathArgLimLow(low));
		} else {
			CTLimUpp upp = f.createCTLimUpp();
			upp.setE(argFrom(kids, 0));
			upp.setLim(argFrom(kids, 1));
			target.add(f.createCTOMathArgLimUpp(upp));
		}
	}

	private CTD fenced(Element e) {
		CTD d = f.createCTD();
		CTDPr pr = f.createCTDPr();
		pr.setBegChr(chr(attrDefault(e, "open", "(")));
		pr.setEndChr(chr(attrDefault(e, "close", ")")));
		String sep = attr(e, "separators");
		pr.setSepChr(chr(sep == null || sep.isEmpty() ? "," : sep.substring(0, 1)));
		d.setDPr(pr);
		for (Element kid : childElements(e)) {
			d.getE().add(argOfChild(kid));
		}
		return d;
	}

	private CTM matrix(Element e) {
		CTM m = f.createCTM();
		CTMPr mPr = f.createCTMPr();
		m.setMPr(mPr);
		for (Element rowEl : childElements(e)) {
			if (!localName(rowEl).equals("mtr") && !localName(rowEl).equals("mlabeledtr")) {
				continue;
			}
			CTMR row = f.createCTMR();
			for (Element cell : childElements(rowEl)) {
				if (localName(cell).equals("mtd")) {
					row.getE().add(argFromChildren(cell));
				} else {
					row.getE().add(argOfChild(cell)); // inferred cell
				}
			}
			m.getMr().add(row);
		}
		return m;
	}

	private void appendMultiscripts(List<Object> target, Element e) {
		List<Element> kids = childElements(e);
		if (kids.isEmpty()) {
			return;
		}
		Element base = kids.get(0);
		int prescriptsAt = -1;
		for (int i = 1; i < kids.size(); i++) {
			if (localName(kids.get(i)).equals("mprescripts")) {
				prescriptsAt = i;
				break;
			}
		}
		int postEnd = (prescriptsAt < 0) ? kids.size() : prescriptsAt;

		// post-scripts (first pair) fold into an sSubSup base
		Object baseElement = f.createCTOMathArgR(run(tokenText(base), variant(base), false));
		if (postEnd >= 3) {
			CTSSubSup ss = f.createCTSSubSup();
			CTOMathArg b = f.createCTOMathArg();
			b.getEGOMathElements().add(baseElement);
			ss.setE(b);
			ss.setSub(scriptArg(kids.get(1)));
			ss.setSup(scriptArg(kids.get(2)));
			baseElement = f.createCTOMathArgSSubSup(ss);
		}

		if (prescriptsAt >= 0 && kids.size() >= prescriptsAt + 3) {
			CTSPre pre = f.createCTSPre();
			CTOMathArg b = f.createCTOMathArg();
			b.getEGOMathElements().add(baseElement);
			pre.setE(b);
			pre.setSub(scriptArg(kids.get(prescriptsAt + 1)));
			pre.setSup(scriptArg(kids.get(prescriptsAt + 2)));
			target.add(f.createCTOMathArgSPre(pre));
		} else {
			target.add(baseElement);
		}
	}

	private CTPhant phantom(Element e) {
		CTPhant ph = f.createCTPhant();
		ph.setE(argFromChildren(e));
		return ph;
	}

	private void appendEnclose(List<Object> target, Element e) {
		String notation = attrDefault(e, "notation", "longdiv");
		if (notation.contains("radical")) {
			CTRad rad = f.createCTRad();
			CTRadPr pr = f.createCTRadPr();
			pr.setDegHide(f.createCTOnOff());
			rad.setRadPr(pr);
			rad.setDeg(f.createCTOMathArg());
			rad.setE(argFromChildren(e));
			target.add(f.createCTOMathArgRad(rad));
		} else if (notation.contains("box") || notation.contains("circle")
				|| notation.contains("roundedbox")) {
			CTBorderBox bb = f.createCTBorderBox();
			bb.setE(argFromChildren(e));
			target.add(f.createCTOMathArgBorderBox(bb));
		} else {
			// strikes, longdiv, actuarial, over/underline: no faithful OMML — keep content
			appendChildren(target, e);
		}
	}

	// ------------------------------------------------------------------ args

	/** An arg built from one MathML element (an mrow flattens into it). */
	private CTOMathArg argOfChild(Element child) {
		CTOMathArg arg = f.createCTOMathArg();
		appendElement(arg.getEGOMathElements(), child);
		return arg;
	}

	/** An arg from the i-th element child (empty if absent). */
	private CTOMathArg argFrom(List<Element> kids, int i) {
		if (i >= kids.size()) {
			return f.createCTOMathArg();
		}
		return argOfChild(kids.get(i));
	}

	/** An arg holding all of the element's children (implicit mrow). */
	private CTOMathArg argFromChildren(Element e) {
		CTOMathArg arg = f.createCTOMathArg();
		appendChildren(arg.getEGOMathElements(), e);
		return arg;
	}

	/** A prescript/postscript arg; an {@code <none/>} yields an empty arg. */
	private CTOMathArg scriptArg(Element e) {
		if (e == null || localName(e).equals("none") || localName(e).equals("mprescripts")) {
			return f.createCTOMathArg();
		}
		return argOfChild(e);
	}

	// --------------------------------------------------------------- helpers

	private CTChar chr(String v) {
		CTChar c = f.createCTChar();
		c.setVal(v);
		return c;
	}

	private static List<Element> childElements(Element e) {
		List<Element> out = new ArrayList<>();
		for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				out.add((Element) n);
			}
		}
		return out;
	}

	/** If the element is a single {@code <mo>} (or token) holding one char, that char. */
	private static String singleMoChar(Element e) {
		if (e == null) {
			return null;
		}
		String t = text(e).trim();
		return t.length() == 1 ? t : null;
	}

	private static String localName(Element e) {
		String ln = e.getLocalName();
		if (ln != null) {
			return ln;
		}
		String n = e.getNodeName();
		int colon = n.indexOf(':');
		return colon >= 0 ? n.substring(colon + 1) : n;
	}

	private static String text(Element e) {
		return e.getTextContent();
	}

	/** Token text with the source whitespace of {@code <mn> 1 </mn>} trimmed. */
	private static String tokenText(Element e) {
		return e.getTextContent().trim();
	}

	private String variant(Element e) {
		return attr(e, "mathvariant");
	}

	private static String attr(Element e, String name) {
		String v = e.getAttribute(name);
		return (v == null || v.isEmpty()) ? null : v;
	}

	private static String attrDefault(Element e, String name, String dflt) {
		String v = attr(e, name);
		return v == null ? dflt : v;
	}
}
