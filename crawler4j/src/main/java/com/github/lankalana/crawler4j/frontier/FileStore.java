/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.lankalana.crawler4j.frontier;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class FileStore<T extends Serializable> {
	private static final Logger logger = LoggerFactory.getLogger(FileStore.class);

	private final File file;

	FileStore(File file) {
		this.file = file;
	}

	T load(Supplier<T> empty) {
		if (!file.exists()) {
			return empty.get();
		}
		try (ObjectInputStream input =
				new ObjectInputStream(new BufferedInputStream(Files.newInputStream(file.toPath())))) {
			@SuppressWarnings("unchecked")
			T value = (T) input.readObject();
			return value;
		} catch (IOException | ClassNotFoundException | ClassCastException e) {
			logger.warn("Unable to read persistent store {}, starting fresh.", file, e);
			return empty.get();
		}
	}

	void save(T value) {
		File parent = file.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		File tmpFile = new File(parent, file.getName() + ".tmp");
		try (ObjectOutputStream output =
				new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(tmpFile.toPath())))) {
			output.writeObject(value);
		} catch (IOException e) {
			throw new RuntimeException("Error writing persistent store: " + file, e);
		}
		try {
			Files.move(
					tmpFile.toPath(),
					file.toPath(),
					StandardCopyOption.REPLACE_EXISTING,
					StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			try {
				Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException moveError) {
				throw new RuntimeException("Error replacing persistent store: " + file, moveError);
			}
		} catch (IOException e) {
			throw new RuntimeException("Error replacing persistent store: " + file, e);
		}
	}
}
