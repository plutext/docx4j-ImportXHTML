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
package org.docx4j.convert.in.xhtml.bundle;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Assert;
import org.junit.Test;

/**
 * Runs after the shade, on the jar itself: no file outside META-INF/ is in both it and
 * docx4j-bundle (or docx4j-bundle-extras), and the two jars alone convert XHTML.
 */
public class BundleIT {

	private static File jar(String property) {
		File f = new File(System.getProperty(property));
		Assert.assertTrue(property + ": " + f, f.isFile());
		return f;
	}

	private static Set<String> files(File jar) throws Exception {
		Set<String> files = new TreeSet<>();
		try (ZipFile z = new ZipFile(jar)) {
			for (Enumeration<? extends ZipEntry> e = z.entries(); e.hasMoreElements();) {
				String name = e.nextElement().getName();
				if (!name.endsWith("/") && !name.startsWith("META-INF/")) files.add(name);
			}
		}
		return files;
	}

	private static void assertDisjoint(String other) throws Exception {
		Set<String> both = files(jar("bundle.jar"));
		both.retainAll(files(jar(other)));
		Assert.assertTrue("in both this jar and " + other + ": " + both, both.isEmpty());
	}

	@Test
	public void nothingInDocx4jBundle() throws Exception {
		assertDisjoint("docx4j.bundle.jar");
	}

	@Test
	public void nothingInDocx4jBundleExtras() throws Exception {
		assertDisjoint("docx4j.bundle.extras.jar");
	}

	/** Smoke, loaded from the test classes, this jar and docx4j-bundle, and nothing else. */
	@Test
	public void convertsFromTheJarsAlone() throws Exception {
		List<URL> urls = new ArrayList<>();
		urls.add(new File(System.getProperty("test.classes")).toURI().toURL());
		urls.add(jar("bundle.jar").toURI().toURL());
		urls.add(jar("docx4j.bundle.jar").toURI().toURL());
		try (URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader())) {
			Thread.currentThread().setContextClassLoader(loader);
			Object result = loader.loadClass(Smoke.class.getName()).getMethod("run").invoke(null);
			Assert.assertEquals("HEAD FOOT BODY, hidden, ImportXHTML and jsoup from the bundle", result);
		}
	}
}
