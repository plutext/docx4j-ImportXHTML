/*
 *  This file is part of the docx4j-ImportXHTML library.
 *
 *  Copyright 2022, Plutext Pty Ltd, and contributors.
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
 * CLASS_TO_STYLE_ONLY: a Word style matching a class attribute will
 * be used, and nothing else
 * 
 * CLASS_PLUS_OTHER: a Word style matching a class attribute will
 * be used; other css will be translated to direct formatting
 * 
 * IGNORE_CLASS: css will be translated to direct formatting
 *
 */
public enum FormattingOption {

	CLASS_TO_STYLE_ONLY, CLASS_PLUS_OTHER, IGNORE_CLASS;
}
