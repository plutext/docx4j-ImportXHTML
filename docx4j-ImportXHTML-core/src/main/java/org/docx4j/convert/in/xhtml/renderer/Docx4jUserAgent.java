/*
 * {{{ header & license
 * Copyright (c) 2004, 2005 Torbj�rn Gannholm
 * Copyright (c) 2006 Wisconsin Court System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * }}}
 */
package org.docx4j.convert.in.xhtml.renderer;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openhtmltopdf.outputdevice.helper.ExternalResourceControlPriority;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceType;
import com.openhtmltopdf.pdfboxout.PdfBoxImage;
import com.openhtmltopdf.resource.ImageResource;
import com.openhtmltopdf.swing.NaiveUserAgent;
import com.openhtmltopdf.util.LogMessageId;


public class Docx4jUserAgent extends NaiveUserAgent {
	
	public static Logger log = LoggerFactory.getLogger(Docx4jUserAgent.class);		


//    private static final int IMAGE_CACHE_CAPACITY = 32;
//
//    private SharedContext _sharedContext;
//
//    private final Docx4jDocxOutputDevice _outputDevice;
//
//    public Docx4jUserAgent(Docx4jDocxOutputDevice outputDevice) {
//		super(IMAGE_CACHE_CAPACITY);
//		_outputDevice = outputDevice;
//    }

    protected byte[] readStream(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(is.available());
        byte[] buf = new byte[10240];
        int i;
        while ( (i = is.read(buf)) != -1) {
            out.write(buf, 0, i);
        }
        out.close();
        return out.toByteArray();
    }


    /**
     * this method was copied from {@link com.openhtmltopdf.pdfboxout.PdfBoxUserAgent#getImageResource(String, ExternalResourceType)} v1.1.24
     * and `scaleToOutputResolution(fsImage)`, `_outputDevice.realizeImage(fsImage)` 
     */
    @Override
    public ImageResource getImageResource(String uriStr, ExternalResourceType type) {
    	
        if (!checkAccessAllowed(uriStr, type, ExternalResourceControlPriority.RUN_BEFORE_RESOLVING_URI)) {
            return new ImageResource(uriStr, null);
        }

        String uriResolved = resolveURI(uriStr);

        if (uriResolved == null) {
//            XRLog.log(Level.INFO, LogMessageId.LogMessageId2Param.LOAD_URI_RESOLVER_REJECTED_LOADING_AT_URI, "image", uriStr);
        	log.info("image " + LogMessageId.LogMessageId2Param.LOAD_URI_RESOLVER_REJECTED_LOADING_AT_URI + uriStr);
            return new ImageResource(uriStr, null);
        }

        if (!checkAccessAllowed(uriResolved, type, ExternalResourceControlPriority.RUN_AFTER_RESOLVING_URI)) {
            return new ImageResource(uriStr, null);
        }

        ImageResource resource = _imageCache.get(uriResolved);

        if (resource != null && resource.getImage() instanceof PdfBoxImage) {
            // Make copy of PdfBoxImage so we don't stuff up the cache.
            PdfBoxImage original = (PdfBoxImage) resource.getImage();
            PdfBoxImage copy = new PdfBoxImage(original.getBytes(), original.getUri(), original.getWidth(), original.getHeight(), original.getXObject());
            return new ImageResource(resource.getImageUri(), copy);
        }


        InputStream is = null;
        try {
        	is = openStream(uriResolved);
        } catch (Exception e) {
        	// NaiveUserAgent.openStream throws NPE if there is no stream factory
        	// for the URI (eg a malformed data URI).  Treat as unfetchable, so we
        	// honour this method's contract of returning an ImageResource
        	// containing a null image, rather than failing the conversion.
        	log.error(LogMessageId.LogMessageId1Param.EXCEPTION_CANT_READ_IMAGE_FILE_FOR_URI + uriStr, e);
        }

        if (is != null) {
            try {
                if (uriStr.toLowerCase(Locale.US).endsWith(".pdf")) {
                    // TODO: Implement PDF AS IMAGE
                    // PdfReader reader = _outputDevice.getReader(uri);
                    // PDFAsImage image = new PDFAsImage(uri);
                    // Rectangle rect = reader.getPageSizeWithRotation(1);
                    // image.setInitialWidth(rect.getWidth() *
                    // _outputDevice.getDotsPerPoint());
                    // image.setInitialHeight(rect.getHeight() *
                    // _outputDevice.getDotsPerPoint());
                    // resource = new ImageResource(uriStr, image);
                } else {
                    byte[] imgBytes = readStream(is);
                    PdfBoxImage fsImage = new PdfBoxImage(imgBytes, uriStr);
                    //scaleToOutputResolution(fsImage);
                    //_outputDevice.realizeImage(fsImage);
                    resource = new ImageResource(uriResolved, fsImage);
                }
                _imageCache.put(uriResolved, resource);
            } catch (Exception e) {
            	log.error(LogMessageId.LogMessageId1Param.EXCEPTION_CANT_READ_IMAGE_FILE_FOR_URI + uriStr, e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        if (resource != null) {
            resource = new ImageResource(resource.getImageUri(), resource.getImage());
        } else {
            resource = new ImageResource(uriStr, null);
        }

        return resource;
    }

    
}
