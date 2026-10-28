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

import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Word's range for each integer attribute the importer writes (CR-007).  Word refuses
 * no value: it keeps each in a fixed width, so a value outside it wraps, with no error
 * (docx4j CR-066).  A row height of 32768 to 65535 is read as exact, cutting its content
 * off, and a negative indent comes out positive.  So a value outside its range is written
 * as the nearest end of it, with a WARN.
 *
 * The ranges are Word's storage, cut to what CSS can mean.
 *
 * @since 17.3.5
 */
enum WordRange {

	IND_LEFT("w:ind/@w:left", -32768, 32767),
	TBL_IND("w:tblInd", -32768, 32767),
	SPACING_BEFORE("w:spacing/@w:before", 0, 65535),
	SPACING_AFTER("w:spacing/@w:after", 0, 65535),
	SPACING_LINE("w:spacing/@w:line", -32768, 32767),
	RUN_SPACING("w:rPr/w:spacing/@w:val", -32768, 32767),
	SZ("w:sz/@w:val", 0, 65535),
	TC_W("w:tcW/@w:w", 0, 32767),
	GRID_COL("w:gridCol/@w:w", 0, 32767),
	TBL_CELL_SPACING("w:tblCellSpacing/@w:w", 0, 32767),
	TC_MAR("w:tcMar/@w:w", 0, 32767),
	/** atLeast; Word reads 32768 to 65535 as exact */
	TR_HEIGHT("w:trHeight/@w:val", 0, 32767),
	/** 8 bits in Word */
	BORDER_SZ("a border's w:sz", 0, 255),
	/** 5 bits in Word */
	BORDER_SPACE("a border's w:space", 0, 31);

	private static final Logger log = LoggerFactory.getLogger(WordRange.class);

	private final String attribute;
	private final BigInteger min;
	private final BigInteger max;

	private WordRange(String attribute, long min, long max) {
		this.attribute = attribute;
		this.min = BigInteger.valueOf(min);
		this.max = BigInteger.valueOf(max);
	}

	BigInteger clamp(long value, String source) {
		return clamp(BigInteger.valueOf(value), source);
	}

	/**
	 * @param value as computed; null is returned as it is
	 * @param source the CSS property or element the value comes from, for the WARN, or null
	 * @return the value where it is inside the range; otherwise the nearest end of the range,
	 * with a WARN
	 */
	BigInteger clamp(BigInteger value, String source) {
		BigInteger nearest;
		if (value==null) {
			return null;
		} else if (value.compareTo(min) < 0) {
			nearest = min;
		} else if (value.compareTo(max) > 0) {
			nearest = max;
		} else {
			return value;
		}
		log.warn("{} {}{} is outside Word's range {}..{}, so {} is written (CR-007)",
				attribute, value, source==null ? "" : " (from " + source + ")", min, max, nearest);
		return nearest;
	}

}
