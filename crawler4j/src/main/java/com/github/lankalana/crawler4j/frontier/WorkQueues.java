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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import com.github.lankalana.crawler4j.url.WebURL;

/** @author Yasser Ganjisaffar */
public class WorkQueues {
	private final DB db;
	private final BTreeMap<Long, byte[]> urlsDB;
	private final boolean resumable;

	private final WebURLSerializer webURLSerializer;

	protected final Object mutex = new Object();

	public WorkQueues(File storageFolder, String dbName, boolean resumable) {
		this.resumable = resumable;
		File dbFile = new File(storageFolder, dbName + ".db");
		DBMaker.Maker maker = DBMaker.fileDB(dbFile).fileMmapEnableIfSupported();
		if (resumable) {
			maker = maker.transactionEnable();
		}
		this.db = maker.make();
		this.urlsDB = db.treeMap(dbName, Serializer.LONG, Serializer.BYTE_ARRAY).createOrOpen();
		this.webURLSerializer = new WebURLSerializer();
	}

	protected void commit() {
		if (resumable) {
			db.commit();
		}
	}

	public List<WebURL> get(int max) {
		synchronized (mutex) {
			List<WebURL> results = new ArrayList<>(max);
			int matches = 0;
			Iterator<byte[]> iterator = urlsDB.values().iterator();
			while (matches < max && iterator.hasNext()) {
				byte[] value = iterator.next();
				if (value != null && value.length > 0) {
					results.add(webURLSerializer.fromBytes(value));
					matches++;
				}
			}
			return results;
		}
	}

	public void delete(int count) {
		synchronized (mutex) {
			int matches = 0;
			Iterator<Long> iterator = urlsDB.keySet().iterator();
			while (matches < count && iterator.hasNext()) {
				iterator.next();
				iterator.remove();
				matches++;
			}
			commit();
		}
	}

	/*
	 * The key that is used for storing URLs determines the order
	 * they are crawled. Lower key values results in earlier crawling.
	 * Here our keys are 6 bytes. The first byte comes from the URL priority.
	 * The second byte comes from depth of crawl at which this URL is first found.
	 * The rest of the 4 bytes come from the docid of the URL. As a result,
	 * URLs with lower priority numbers will be crawled earlier. If priority
	 * numbers are the same, those found at lower depths will be crawled earlier.
	 * If depth is also equal, those found earlier (therefore, smaller docid) will
	 * be crawled earlier.
	 */
	protected static long getDatabaseEntryKey(WebURL url) {
		int depth = url.getDepth();
		depth = (depth > Byte.MAX_VALUE) ? Byte.MAX_VALUE : depth;
		long docId = url.getDocid() & 0xFFFFFFFFL;
		return ((long) (url.getPriority() & 0xFF) << 40) | ((long) (depth & 0xFF) << 32) | docId;
	}

	public void put(WebURL url) {
		urlsDB.put(getDatabaseEntryKey(url), webURLSerializer.toBytes(url));
		commit();
	}

	public long getLength() {
		return urlsDB.size();
	}

	public void close() {
		commit();
		db.close();
	}

	protected boolean removeByKey(long key) {
		boolean removed = urlsDB.remove(key) != null;
		if (removed) {
			commit();
		}
		return removed;
	}
}
