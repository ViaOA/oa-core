/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Custom {@link ClassLoader} implementation that loads a single specified class
 * from the system classpath as a raw class resource.
 */
public class OAClassLoader extends ClassLoader {

	/**
	 * The fully qualified name of the class that this class loader is responsible for loading.
	 */
	private final String className;
	
	/**
	 * Cached {@link Class} instance once the target class has been successfully loaded.
	 */
	private Class<?> clazz;

	/**
	 * Creates a new class loader for loading a specific class name.
	 *
	 * @param className the fully qualified name of the class to load
	 */
	public OAClassLoader(String className) {
		this.className = className;
	}

	/**
	 * Loads the specified class.
	 *
	 * If the requested class name does not match the configured class name,
	 * this method delegates to the system class loader.
	 *
	 * If the class matches and has already been loaded, the cached class is returned.
	 * Otherwise, the class bytecode is read from the system resource stream and
	 * defined using {@link ClassLoader#defineClass(String, byte[], int, int)}.
	 *
	 * @param className the fully qualified name of the class to load
	 * @return the loaded {@link Class}
	 * @throws ClassNotFoundException if the class resource cannot be found or read
	 */
	public Class<?> loadClass(String className) throws ClassNotFoundException {
		if (!this.className.equals(className)) {
			return findSystemClass(className);
		}
		if (clazz != null) {
			return clazz;
		}

		String cn = className.replace('.', '/');
		InputStream is = ClassLoader.getSystemResourceAsStream(cn + ".class");
		if (is == null) {
			throw new ClassNotFoundException("could not load class as resource using OAClassLoader");
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (;;) {
			int x;
			try {
				x = is.read();
			} catch (IOException e) {
				throw new ClassNotFoundException("IO exception while reading " + className + ".class", e);
			}
			if (x < 0) {
				break;
			}
			baos.write(x);
		}
		byte[] bs = baos.toByteArray();

		clazz = super.defineClass(className, bs, 0, bs.length);

		return clazz;
	}


}
