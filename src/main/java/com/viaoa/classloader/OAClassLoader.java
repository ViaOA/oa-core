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
package com.viaoa.classloader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


/*qqqqqqqqqqqqqqqqqqqqqqq
CODEX

 1. src/main/java/com/viaoa/classloader/OAClassLoader.java / loadClass(String className)

  Concrete bug: target class loading is not synchronized, so concurrent calls can attempt duplicate defineClass for
  the same class.

  Runtime scenario: one OAClassLoader instance is shared by OA tooling/codegen/runtime code and two threads call
  loadClass(targetName) at the same time:

  1. Both enter loadClass.
  2. Both see clazz == null at line 73.
  3. Both read the same class bytes.
  4. Thread A executes defineClass(...) at line 97 and assigns clazz.
  5. Thread B also executes defineClass(...) for the same class name in the same loader.
  6. JVM throws a duplicate class definition LinkageError.

  Why this violates OA/OG classloader semantics: failed class loading must be deterministic and caller-visible as a
  load failure, not a race-dependent VM linkage error. OA metadata, annotation discovery, reflection, and
  serialization depend on stable class identity. Concurrent load of the same OA-generated/model class must resolve to
  one Class instance per loader.

  Minimal fix direction: synchronize target class loading using getClassLoadingLock(className) or make loadClass
  synchronized and recheck clazz inside the lock. Prefer implementing loadClass(String, boolean) with the standard
  ClassLoader locking/delegation pattern.

  Suggested CODEX comment location: before the if (clazz != null) check at line 73 or before defineClass at line 97.

  Suggested regression test: testConcurrentLoadClassDefinesTargetOnlyOnce.

  2. src/main/java/com/viaoa/classloader/OAClassLoader.java / loadClass(String className)

  Concrete bug: the class resource InputStream is never closed.

  Runtime scenario: OA tooling repeatedly creates OAClassLoader instances to inspect or reload generated model
  classes. Each successful or partially successful load opens a resource stream at line 78 and reads it byte-by-byte,
  but neither the success path nor the IOException path closes it.

  Why this violates OA/OG classloader semantics: class/resource loading must have clear stream ownership. Leaking
  classpath resource streams can retain jar/file handles and classloader-related resources, which is especially risky
  for generated-code/model-tooling workflows and long-running OA tooling processes.

  Minimal fix direction: use try-with-resources around ClassLoader.getSystemResourceAsStream(...). Also consider
  reading with read(byte[]) instead of one byte at a time, but the correctness fix is stream closure.

  Suggested CODEX comment location: line 78 where the stream is opened.

  Suggested regression test: testLoadClassClosesResourceStreamOnSuccessAndFailure.

  3. src/main/java/com/viaoa/classloader/OAClassLoader.java / loadClass(String className)

  Concrete bug: non-target class resolution delegates only to findSystemClass(className), not the loader’s parent/
  context delegation chain.

  Runtime scenario: the target class references an OA/model/helper type that is available through the application/
  runtime classloader but not through the system classloader. When the JVM asks this loader to resolve that referenced
  type, line 71 calls findSystemClass, which can throw ClassNotFoundException even though the class is available to
  the intended parent/application loader.

  Why this violates OA/OG classloader semantics: dependency class resolution must be deterministic and must not
  accidentally drift away from the runtime classloader that owns OA metadata, annotations, reflection, and
  serialization classes. Bypassing normal parent delegation can make generated model classes fail to resolve
  dependencies or resolve them through the wrong loader boundary.

  Minimal fix direction: delegate non-target classes with super.loadClass(className) or an explicit parent loader
  supplied to the constructor. Make parent-first vs child-first behavior explicit for the target class only.

  Suggested CODEX comment location: line 71.

  Suggested regression test: testTargetClassCanResolveDependencyFromParentClassLoader.


1. src/main/java/com/viaoa/classloader/OAClassLoader.java / loadClass(String className)

  Concrete bug: the target class itself is loaded only from the system classloader resource path, not from the
  intended parent/context/application classloader.

  Runtime scenario: OA tooling/codegen/runtime has a generated model class available through the thread context
  classloader or an application classloader, but not on the system classpath. new
  OAClassLoader(targetName).loadClass(targetName) reaches line 77, converts the class name to a resource path, then
  line 78 calls:

  ClassLoader.getSystemResourceAsStream(cn + ".class")

  If the generated/model class is visible to OA through the context/application loader but not the system loader, is
  == null and line 80 throws ClassNotFoundException.

  Why this violates OA/OG classloader semantics: OA class loading must resolve the intended class/resource
  consistently from the loader boundary that owns OA metadata, annotations, reflection, serialization, and generated
  model classes. This is not hostile input; it is normal Java deployment structure in app servers, plugin/codegen
  tooling, test loaders, or runtime model reload flows. The failure is visible, but it is the wrong resolution
  contract.

  Minimal fix direction: give OAClassLoader an explicit parent/source ClassLoader and load target bytes through that
  loader’s getResourceAsStream, falling back to the context classloader or super.getResourceAsStream according to a
  documented order. Keep the target child-definition behavior explicit if that is the purpose.

  Suggested CODEX comment location: OAClassLoader.java line 78, next to ClassLoader.getSystemResourceAsStream(...).

  Suggested regression test: testTargetClassLoadedFromContextClassLoaderWhenNotOnSystemClasspath.

*/

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
