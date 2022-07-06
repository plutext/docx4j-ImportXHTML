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
