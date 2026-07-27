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

import java.io.IOException;
import java.io.InputStream;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.stream.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class allows us to get the body
 * 
 * @author jharrop
 *
 */
public class MHTContentHandler implements org.apache.james.mime4j.parser.ContentHandler {
	
	// TODO: extract charset

	public static Logger log = LoggerFactory.getLogger(MHTContentHandler.class);	
	
	/*  Typical BIRT content:
	 * 
		From:
		Subject:
		Date:
		MIME-Version: 1.0
		Content-Type: multipart/related; type="text/html"; boundary="___Actuate_Content_Boundary___"
		
		--___Actuate_Content_Boundary___
		Content-Type: text/html; charset="gb2312"
		Content-Transfer-Encoding: quoted-printable
		
		=EF=BB=BF<html><head><style type=3D"text/css">.styleForeign{ font-family: serif; font-style: normal; font-variant: normal; font-weight: normal; font-size: 10pt; color: rgb(0, 0, 0); margin: 0; padding: 0; text-indent: 0pt; letter-spacing: 0; word-spacing: 0; text-transform: none; white-space: normal; line-height: normal;}</style></head><body><div class=3D"styleForeign">Dec 2, 2021 3:02 PM</div></body>
		</html>
		--___Actuate_Content_Boundary___--
		
		
		from which we extract the body:
		
			=EF=BB=BF<html><head><style type=3D"text/css">.styleForeign{ font-family: serif; font-style: normal; font-variant: normal; font-weight: normal; font-size: 10pt; color: rgb(0, 0, 0); margin: 0; padding: 0; text-indent: 0pt; letter-spacing: 0; word-spacing: 0; text-transform: none; white-space: normal; line-height: normal;}</style></head><body><div class=3D"styleForeign">Dec 2, 2021 3:02 PM</div></body>
			</html>
		
 * 
 */	
	
	@Override
	public void startMessage() throws MimeException {
		log.info("startMessage");
		
	}

	@Override
	public void endMessage() throws MimeException {
		log.info("end message");
		
	}

	@Override
	public void startBodyPart() throws MimeException {
		log.info("startBodyPart");
		
	}

	@Override
	public void endBodyPart() throws MimeException {
		log.info("endBodyPart");
		
	}

	@Override
	public void startHeader() throws MimeException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void field(Field rawField) throws MimeException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endHeader() throws MimeException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void preamble(InputStream is) throws MimeException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void epilogue(InputStream is) throws MimeException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void startMultipart(BodyDescriptor bd) throws MimeException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void endMultipart() throws MimeException {
		// TODO Auto-generated method stub
		
	}

	private InputStream body = null;
	
	public InputStream getBody() {
		return body;
	}
	
	private String charset;
	public String getCharset() {
		return charset;
	}

	private String mimeType;
	public String getMimeType() {
		return mimeType;
	}


	@Override
	public void body(BodyDescriptor bd, InputStream is) throws MimeException, IOException {

		log.info("body");
		charset = bd.getCharset();
		mimeType = bd.getMimeType();
		
		//IOUtils.copy(is, System.out);
		body=is;
		
	}

	@Override
	public void raw(InputStream is) throws MimeException, IOException {
		// TODO Auto-generated method stub
		
	}

}
