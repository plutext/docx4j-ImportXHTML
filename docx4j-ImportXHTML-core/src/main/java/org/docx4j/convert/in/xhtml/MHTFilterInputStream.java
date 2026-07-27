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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author jharrop
 *
 */
public class MHTFilterInputStream extends FilterInputStream {
	
	/*
	 *  Typical BIRT content contains crap in the prolog, and weird 3D before attribute value:
	 *  
		=EF=BB=BF<html><head><style type=3D"text/css">.styleForeign{ font-family: serif; font-style: normal; font-variant: normal; font-weight: normal; font-size: 10pt; color: rgb(0, 0, 0); margin: 0; padding: 0; text-indent: 0pt; letter-spacing: 0; word-spacing: 0; text-transform: none; white-space: normal; line-height: normal;}</style></head><body><div class=3D"styleForeign">Dec 2, 2021 3:02 PM</div></body>
		</html>
	 * 
	 */

	public static Logger log = LoggerFactory.getLogger(MHTFilterInputStream.class);		
	
	protected MHTFilterInputStream(InputStream in) {
		super(in);
	}

	boolean reachedXml = false;
	
	public int read() throws IOException {
		
		// TODO
		log.warn("TODO, override read()");
		return super.read();
	}
	
	public int read(byte[] b) throws IOException {

		// TODO
		log.warn("TODO, override read(byte[])");
		return super.read(b);
		
	}
	
	public int read(byte[] b,
		       int off,
		       int len)
		         throws IOException {

//		System.out.print("got" + b.length);
		int outIndex = -1;
		
		if (reachedXml) {
			return super.read(b, off, len);
		} else {
			byte[] tmp = new byte[len];
			super.read(tmp, off, len);
			boolean stateAttrEq = false; // logic to skip 3D in type=3D"
			for (int i=0; i<tmp.length; i++) {
				
				// skip content in prolog				
				if (!reachedXml && tmp[i]=='<') {
					log.debug("got <" );
					reachedXml = true;
				}

				if (reachedXml) {
					
					if (stateAttrEq) {
						
						if (tmp[i]=='"') {
							stateAttrEq = false;							
						}
					} 
					
					
					if /* usual case */ (!stateAttrEq) {
					
						if (tmp[i]!=0) {
							//System.out.println(tmp[i] + ": " + (char)tmp[i]);
							outIndex++;
							b[outIndex]=tmp[i];
						}
					}
					
					if (tmp[i]=='=') {
						stateAttrEq = true;
					}
					
				}
			}
		}
		 if (outIndex==-1) {
			 return outIndex;
		 } else {
			 return outIndex+1;
		 }
		
	}
	
}
