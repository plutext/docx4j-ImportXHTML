/*
 *  This file is part of the docx4j-ImportXHTML library.
 *
 *  Copyright 2026, Plutext Pty Ltd, and contributors.
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

/**
 * Thrown by {@link XHTMLImageHandlerDefault} when an image can't be added to the
 * docx, and property docx4j-ImportXHTML.Images.ThrowOnMissing is true.
 *
 * <p>Despite the name, this covers any image which couldn't be added, including
 * one which couldn't be fetched (eg a wrong or unreachable URL), one which
 * couldn't be read or decoded, and a malformed data URI.
 *
 * <p>The default (property absent or false) is to insert placeholder text
 * instead, so conversion continues; set the property if you'd rather fail fast
 * than deliver a document with images silently missing.
 *
 * <p>This is an unchecked exception, so that the signature of
 * {@link XHTMLImageHandler#addImage} is unaffected; it propagates out of
 * XHTMLImporter's convert methods.
 *
 * @since 17.0.2
 */
public class MissingImageException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * The value of the img @src which couldn't be added.
	 */
	private final String src;

	public MissingImageException(String src) {
		this(src, null);
	}

	public MissingImageException(String src, Throwable cause) {
		super("Couldn't add image: " + src, cause);
		this.src = src;
	}

	/**
	 * @return the value of the img @src which couldn't be added
	 */
	public String getSrc() {
		return src;
	}

}
