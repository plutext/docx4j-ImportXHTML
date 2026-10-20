/*
 *  This file is part of the docx4j-ImportXHTML library.
 *
 *  Copyright 2011-2026, Plutext Pty Ltd, and contributors.
 *  Portions contributed before 15 July 2013 formed part of docx4j
 *  and were contributed under ASL v2 (a copy of which is incorporated
 *  herein by reference and applies to those portions).
 *
 *  This library as a whole is licensed under the GNU Lesser General
 *  Public License as published by the Free Software Foundation;
    version 2.1.

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library (see legals/LICENSE); if not,
    see http://www.gnu.org/licenses/lgpl-2.1.html

 */
package org.docx4j.convert.in.xhtml;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.xml.bind.JAXBElement;

import org.docx4j.XmlUtils;
import org.docx4j.convert.in.xhtml.renderer.DocxRenderer;
import org.docx4j.jaxb.Context;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.CTMarkupRange;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.P;
import org.docx4j.wml.PPr;
import org.docx4j.wml.ParaRPr;
import org.docx4j.wml.R;
import org.docx4j.wml.RPr;
import org.docx4j.wml.SdtElement;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.Tr;
import org.docx4j.wml.TrPr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.openhtmltopdf.context.StandardAttributeResolver;
import com.openhtmltopdf.context.StylesheetFactoryImpl;
import com.openhtmltopdf.css.constants.CSSName;
import com.openhtmltopdf.css.extend.lib.DOMTreeResolver;
import com.openhtmltopdf.css.newmatch.Matcher;
import com.openhtmltopdf.css.sheet.PropertyDeclaration;
import com.openhtmltopdf.css.sheet.StylesheetInfo;
import com.openhtmltopdf.extend.NamespaceHandler;
import com.openhtmltopdf.extend.UserInterface;
import com.openhtmltopdf.layout.SharedContext;
import com.openhtmltopdf.render.BlockBox;
import com.openhtmltopdf.render.InlineBox;

/**
 * display: none as hidden text, for XHTMLImporterImpl.setDisplayNoneAsHidden; see
 * docs/change-requests/CR-002 sections 2 and 3.
 *
 * <p>Before layout, each element an author hid with display: none is shown again with
 * its default display (an appended inline display:&lt;default&gt; !important), so that
 * openhtmltopdf builds boxes for it; its style attribute is restored afterwards.
 * During the traversal, the importer tells this class which box it is in, and each
 * paragraph, table, row and run it creates.  Afterwards, what came from a hidden
 * element gets w:vanish, and is reported by the outermost hidden element it came from.
 *
 * <p>As in Word, an element whose display an author sets to anything but none or
 * inherit ends its ancestors' hiding, for its own content.
 *
 * <p>One instance per convert; an inactive instance does nothing.
 */
class DisplayNoneHelper {

	public static Logger log = LoggerFactory.getLogger(DisplayNoneHelper.class);

	private static final UserInterface NO_UI = new UserInterface() {
		public boolean isHover(Element e) { return false; }
		public boolean isActive(Element e) { return false; }
		public boolean isFocus(Element e) { return false; }
		public boolean isMarker(Element e) { return false; }
	};

	private final boolean active;

	/** Elements an author hid, which we've shown again, with their index in document order */
	private final Map<Element, Integer> marked = new IdentityHashMap<>();
	/** Elements whose display an author set to something other than none or inherit */
	private final Set<Element> revealing = Collections.newSetFromMap(new IdentityHashMap<>());
	/** Each marked element's style attribute as we found it; null where it had none */
	private final Map<Element, String> originalStyle = new IdentityHashMap<>();

	private final Map<Element, Element> ownerCache = new IdentityHashMap<>();

	/** Each paragraph, table and run created, with the outermost hidden element it came from (or null) */
	private final Map<Object, Element> owners = new IdentityHashMap<>();
	private final Set<Tr> hiddenRows = Collections.newSetFromMap(new IdentityHashMap<>());

	/** The hidden owner of the box being processed */
	private Element currentOwner;
	/** The hidden owner of the nearest enclosing block, whose paragraph a run goes into */
	private Element blockOwner;
	private final Deque<Element[]> stack = new ArrayDeque<>();

	private Map<Element, List<Object>> hiddenContent = Collections.emptyMap();

	DisplayNoneHelper(boolean active) {
		this.active = active;
	}

	/**
	 * Show again each element an author hid, and note which elements reveal their content.
	 * Call after setDocument, and before layout.  Must not call SharedContext.getStyle,
	 * whose cache would keep the hidden style.
	 */
	void prepare(DocxRenderer renderer) {

		if (!active) return;

		SharedContext sharedContext = renderer.getSharedContext();
		NamespaceHandler nsh = sharedContext.getNamespaceHandler();

		// The default stylesheet alone, ignoring style attributes (no StylesheetFactory),
		// gives each element's default display.  It is parsed and cached by now.
		StylesheetInfo defaultStylesheet = nsh.getDefaultStylesheet(new StylesheetFactoryImpl(sharedContext.getUac()));
		Matcher defaults = new Matcher(new DOMTreeResolver(),
				new StandardAttributeResolver(nsh, sharedContext.getUac(), NO_UI),
				null,
				(defaultStylesheet==null || defaultStylesheet.getStylesheet()==null) ?
						Collections.emptyList() : Collections.singletonList(defaultStylesheet.getStylesheet()),
				sharedContext.getMedia());

		List<Element> toShow = new ArrayList<>();
		List<String> defaultDisplay = new ArrayList<>();

		Element root = body(renderer.getDocument());
		Node n = root;
		while (n!=null) {
			if (n instanceof Element) {
				Element e = (Element)n;
				PropertyDeclaration display = sharedContext.getCss().getCascadedStyle(e, false).propertyByName(CSSName.DISPLAY);
				if (display!=null && display.getOrigin()==StylesheetInfo.AUTHOR) {
					String value = display.getValue().getCssText();
					if ("none".equalsIgnoreCase(value)) {
						String def = displayOf(defaults, e);
						if (!"none".equalsIgnoreCase(def)) {
							marked.put(e, marked.size());
							toShow.add(e);
							defaultDisplay.add(def);
						}
						// else hidden by default as well (eg script), so leave it alone
					} else if (!"inherit".equalsIgnoreCase(value)) {
						// inherit is the specified keyword, not resolved, so exclude it by name
						revealing.add(e);
					}
				}
			}
			n = next(n, root);
		}

		for (int i=0; i<toShow.size(); i++) {
			Element e = toShow.get(i);
			String style = e.hasAttribute("style") ? e.getAttribute("style") : null;
			originalStyle.put(e, style);
			// An inline !important beats a stylesheet's !important, and is applied after
			// any display:none !important already in the attribute
			String s = style==null ? "" : style;
			if (!s.trim().isEmpty() && !s.trim().endsWith(";")) {
				s += ";";
			}
			e.setAttribute("style", s + "display:" + defaultDisplay.get(i) + " !important");
		}
		log.debug("display:none on " + marked.size() + " elements; " + revealing.size() + " revealing");
	}

	/**
	 * Put back the style attributes prepare changed, as they were found.
	 */
	void restore() {
		for (Map.Entry<Element, String> entry : originalStyle.entrySet()) {
			if (entry.getValue()==null) {
				entry.getKey().removeAttribute("style");
			} else {
				entry.getKey().setAttribute("style", entry.getValue());
			}
		}
		originalStyle.clear();
	}

	private static String displayOf(Matcher defaults, Element e) {
		PropertyDeclaration display = defaults.getCascadedStyle(e, false).propertyByName(CSSName.DISPLAY);
		return display==null ? "inline" : display.getValue().getCssText(); // inline is the initial value
	}

	private static Element body(Document doc) {
		Element root = doc.getDocumentElement();
		for (Node n = root.getFirstChild(); n!=null; n = n.getNextSibling()) {
			if (n instanceof Element
					&& "body".equalsIgnoreCase(n.getLocalName()==null ? n.getNodeName() : n.getLocalName())) {
				return (Element)n;
			}
		}
		return root; // a fragment
	}

	/** The node after n in document order, within root */
	private static Node next(Node n, Node root) {
		if (n.getFirstChild()!=null) return n.getFirstChild();
		while (n!=root) {
			if (n.getNextSibling()!=null) return n.getNextSibling();
			n = n.getParentNode();
		}
		return null;
	}

	/**
	 * The outermost hidden element e's content is hidden through: walking up from e, the
	 * last marked element before a revealing one (or the root); null if none.
	 */
	private Element ownerOf(Element e) {
		if (e==null || marked.isEmpty()) return null;
		if (ownerCache.containsKey(e)) return ownerCache.get(e);
		Element owner = null;
		for (Node n = e; n instanceof Element; n = n.getParentNode()) {
			if (marked.containsKey(n)) {
				owner = (Element)n;
			} else if (revealing.contains(n)) {
				break;
			}
		}
		ownerCache.put(e, owner);
		return owner;
	}

	// Tracking, during the traversal

	void enter(BlockBox box) {
		if (!active) return;
		stack.push(new Element[] {currentOwner, blockOwner});
		Element e = box.getElement();
		if (e==null) return;
		currentOwner = ownerOf(e);
		if (!e.getNodeName().equals("img")  // the importer doesn't start a new paragraph after an img
				&& !"inline".equals(box.getStyle().getStringProperty(CSSName.DISPLAY))) {
			blockOwner = currentOwner;
		}
	}

	void enter(InlineBox box) {
		if (!active) return;
		stack.push(new Element[] {currentOwner, blockOwner});
		if (box.getElement()!=null) {
			currentOwner = ownerOf(box.getElement());
		}
	}

	void leave() {
		if (!active) return;
		Element[] previous = stack.pop();
		currentOwner = previous[0];
		blockOwner = previous[1];
	}

	/**
	 * A paragraph for the flow of the enclosing block, whichever box's content
	 * caused it to be created; or an existing paragraph a block is taking over (the
	 * importer lets a block's first child use the block's paragraph).  A paragraph
	 * belongs to the last block to take it over before it has a run.
	 */
	void paragraph(P p) {
		if (!active) return;
		if (!owners.containsKey(p) || !hasRun(p)) {
			owners.put(p, blockOwner);
		}
	}

	private static boolean hasRun(P p) {
		for (Object o : p.getContent()) {
			if (!(XmlUtils.unwrap(o) instanceof CTMarkupRange)) return true; // anything but a bookmark
		}
		return false;
	}

	/**
	 * A run, a table, or a paragraph made for the box being processed itself (math, hr).
	 */
	void created(Object o) {
		if (!active) return;
		owners.put(o, currentOwner);
	}

	/**
	 * The runs added to p from index from onwards (by an XHTMLImageHandler).
	 */
	void createdRuns(P p, int from) {
		if (!active) return;
		for (int i=from; i<p.getContent().size(); i++) {
			Object o = XmlUtils.unwrap(p.getContent().get(i));
			if (o instanceof R) {
				owners.put(o, currentOwner);
			}
		}
	}

	/**
	 * A row is hidden (w:trPr/w:hidden), as Word hides it, where its tr, or the
	 * thead, tbody, tfoot or table above it, is hidden, with no revealing element
	 * between.
	 */
	void row(Tr tr, Element e) {
		if (!active || marked.isEmpty()) return;
		for (Node n = e; n instanceof Element; n = n.getParentNode()) {
			String name = n.getNodeName();
			if (marked.containsKey(n)
					&& (name.equals("tr") || name.equals("thead") || name.equals("tbody")
							|| name.equals("tfoot") || name.equals("table"))) {
				hiddenRows.add(tr);
				return;
			}
			if (revealing.contains(n) || name.equals("table")) return;
		}
	}

	// After the traversal

	private final Map<Object, Element> effectiveOwners = new IdentityHashMap<>();
	private final Set<Object> whollyHidden = Collections.newSetFromMap(new IdentityHashMap<>());

	/**
	 * Apply w:vanish (and w:trPr/w:hidden) to what came from hidden elements, and
	 * collect it for getHiddenContent.
	 *
	 * @param content what this convert produced
	 */
	void apply(List<Object> content) {

		if (!active || marked.isEmpty()) return;

		for (Object o : content) {
			apply(o, null);
		}

		Map<Element, List<Object>> found = new IdentityHashMap<>();
		for (Object o : content) {
			collect(o, null, found);
		}
		List<Element> keys = new ArrayList<>(found.keySet());
		keys.sort((a, b) -> marked.get(a) - marked.get(b)); // document order
		hiddenContent = new LinkedHashMap<>();
		for (Element key : keys) {
			hiddenContent.put(key, Collections.unmodifiableList(found.get(key)));
		}
	}

	/**
	 * @return whether everything o holds is hidden
	 */
	private boolean apply(Object o, Element inherited) {

		Object u = XmlUtils.unwrap(o);
		// What wasn't recorded (a cell, a field, a dummy cell's paragraph) is hidden with its parent
		Element owner = owners.containsKey(u) ? owners.get(u) : inherited;
		effectiveOwners.put(u, owner);
		boolean hidden = owner!=null;

		if (u instanceof R) {
			if (hidden) vanish((R)u);
			return hidden;
		}
		if (u.getClass().getName().startsWith("org.docx4j.math.")) {
			if (hidden) vanishMath(u);
			return hidden;
		}
		if (u instanceof P && hidden) {
			vanish((P)u);
		}
		if (u instanceof Tr && hiddenRows.contains(u)) {
			hide((Tr)u);
		}

		boolean all = hidden;
		for (Object child : children(u)) {
			all &= apply(child, owner);
		}
		if (all && (u instanceof P || u instanceof Tbl)) {
			whollyHidden.add(u);
		}
		return all;
	}

	/**
	 * A wholly hidden paragraph or table is reported as itself, by its owner, and once:
	 * not again for what it holds, unless something in it has another owner.  A hidden
	 * run is reported where its paragraph isn't wholly hidden.
	 */
	private void collect(Object o, Element reportedAbove, Map<Element, List<Object>> found) {

		Object u = XmlUtils.unwrap(o);
		Element owner = effectiveOwners.get(u);

		if (whollyHidden.contains(u)) {
			if (owner!=reportedAbove) {
				found.computeIfAbsent(owner, k -> new ArrayList<>()).add(u);
			}
			if (u instanceof P) return;
			reportedAbove = owner;
		} else if (u instanceof R) {
			if (owner!=null) {
				found.computeIfAbsent(owner, k -> new ArrayList<>()).add(u);
			}
			return;
		}
		for (Object child : children(u)) {
			collect(child, reportedAbove, found);
		}
	}

	private static List<Object> children(Object o) {
		if (o instanceof R) {
			return Collections.emptyList();
		} else if (o instanceof ContentAccessor) {
			return ((ContentAccessor)o).getContent();
		} else if (o instanceof SdtElement
				&& ((SdtElement)o).getSdtContent()!=null) {
			return ((SdtElement)o).getSdtContent().getContent();
		}
		return Collections.emptyList();
	}

	private static void vanish(R r) {
		if (r.getRPr()==null) {
			r.setRPr(Context.getWmlObjectFactory().createRPr());
		}
		r.getRPr().setVanish(new BooleanDefaultTrue());
	}

	/** The paragraph mark */
	private static void vanish(P p) {
		if (p.getPPr()==null) {
			p.setPPr(Context.getWmlObjectFactory().createPPr());
		}
		PPr pPr = p.getPPr();
		if (pPr.getRPr()==null) {
			pPr.setRPr(Context.getWmlObjectFactory().createParaRPr());
		}
		ParaRPr rPr = pPr.getRPr();
		rPr.setVanish(new BooleanDefaultTrue());
	}

	private static void hide(Tr tr) {
		if (tr.getTrPr()==null) {
			tr.setTrPr(Context.getWmlObjectFactory().createTrPr());
		}
		TrPr trPr = tr.getTrPr();
		for (JAXBElement<?> el : trPr.getCnfStyleOrDivIdOrGridBefore()) {
			if (el.getName().getLocalPart().equals("hidden")) return;
		}
		trPr.getCnfStyleOrDivIdOrGridBefore().add(
				Context.getWmlObjectFactory().createCTTrPrBaseHidden(new BooleanDefaultTrue()));
	}

	/**
	 * An equation: w:rPr/w:vanish in each m:r.  The math classes have no common
	 * accessor for their children, so walk their getters.
	 */
	private static void vanishMath(Object o) {
		o = XmlUtils.unwrap(o);
		if (o==null || !o.getClass().getName().startsWith("org.docx4j.math.")) return;
		if (o instanceof org.docx4j.math.CTR) {
			List<Object> content = ((org.docx4j.math.CTR)o).getContent();
			RPr rPr = null;
			int at = 0;
			for (int i=0; i<content.size(); i++) {
				Object c = content.get(i);
				if (c instanceof RPr) {
					rPr = (RPr)c;
				} else if (c instanceof JAXBElement
						&& ((JAXBElement<?>)c).getName().getLocalPart().equals("rPr")) {
					at = i+1; // m:rPr comes before w:rPr
				}
			}
			if (rPr==null) {
				rPr = Context.getWmlObjectFactory().createRPr();
				content.add(at, rPr);
			}
			rPr.setVanish(new BooleanDefaultTrue());
			return;
		}
		for (Method m : o.getClass().getMethods()) {
			if (m.getParameterCount()!=0
					|| !m.getName().startsWith("get")
					|| m.getName().equals("getParent")) continue;
			Object value;
			try {
				value = m.invoke(o);
			} catch (IllegalAccessException | InvocationTargetException e) {
				continue;
			}
			if (value instanceof List) {
				for (Object child : (List<?>)value) {
					vanishMath(child);
				}
			} else {
				vanishMath(value);
			}
		}
	}

	/**
	 * @return each outermost hidden element which produced content, in document order,
	 * with what it produced; empty when inactive
	 */
	Map<Element, List<Object>> getHiddenContent() {
		return Collections.unmodifiableMap(hiddenContent);
	}

}
