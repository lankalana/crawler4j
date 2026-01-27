/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.uci.ics.crawler4j.frontier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;

/**
 * @author Yasser Ganjisaffar
 */

public class DocIDServer {
    private static final Logger logger = LoggerFactory.getLogger(DocIDServer.class);

    private final DB db;
    private final HTreeMap<String, Integer> docIDsDB;
    private static final String DATABASE_NAME = "DocIDs";

    private final Object mutex = new Object();

    private CrawlConfig config;
    private int lastDocID;
    private final boolean resumable;

    public DocIDServer(File storageFolder, CrawlConfig config) {
        this.config = config;
        this.resumable = config.isResumableCrawling();
        File dbFile = new File(storageFolder, DATABASE_NAME + ".db");
        DBMaker.Maker maker = DBMaker.fileDB(dbFile).fileMmapEnableIfSupported();
        if (resumable) {
            maker = maker.transactionEnable();
        }
        this.db = maker.make();
        lastDocID = 0;
        docIDsDB = db.hashMap(DATABASE_NAME, Serializer.STRING, Serializer.INTEGER).createOrOpen();
        if (resumable) {
            int docCount = getDocCount();
            if (docCount > 0) {
                logger.info("Loaded {} URLs that had been detected in previous crawl.", docCount);
                lastDocID = docCount;
            }
        }
    }

    /**
     * Returns the docid of an already seen url.
     *
     * @param url the URL for which the docid is returned.
     * @return the docid of the url if it is seen before. Otherwise -1 is returned.
     */
    public int getDocId(String url) {
        synchronized (mutex) {
            try {
                Integer value = docIDsDB.get(url);
                if (value != null) {
                    return value;
                }
            } catch (RuntimeException e) {
                if (config.isHaltOnError()) {
                    throw e;
                } else {
                    logger.error("Exception thrown while getting DocID", e);
                    return -1;
                }
            }

            return -1;
        }
    }

    public int getNewDocID(String url) {
        synchronized (mutex) {
            try {
                // Make sure that we have not already assigned a docid for this URL
                int docID = getDocId(url);
                if (docID > 0) {
                    return docID;
                }

                ++lastDocID;
                docIDsDB.put(url, lastDocID);
                commitIfNeeded();
                return lastDocID;
            } catch (RuntimeException e) {
                if (config.isHaltOnError()) {
                    throw e;
                } else {
                    logger.error("Exception thrown while getting new DocID", e);
                    return -1;
                }
            }
        }
    }

    public void addUrlAndDocId(String url, int docId) {
        synchronized (mutex) {
            if (docId <= lastDocID) {
                throw new IllegalArgumentException(
                    "Requested doc id: " + docId + " is not larger than: " + lastDocID);
            }

            // Make sure that we have not already assigned a docid for this URL
            int prevDocid = getDocId(url);
            if (prevDocid > 0) {
                if (prevDocid == docId) {
                    return;
                }
                throw new IllegalArgumentException("Doc id: " + prevDocid + " is already assigned to URL: " + url);
            }

            docIDsDB.put(url, docId);
            lastDocID = docId;
            commitIfNeeded();
        }
    }

    public boolean isSeenBefore(String url) {
        return getDocId(url) != -1;
    }

    public final int getDocCount() {
        try {
            return docIDsDB.size();
        } catch (RuntimeException e) {
            logger.error("Exception thrown while getting DOC Count", e);
            return -1;
        }
    }

    public void close() {
        try {
            commitIfNeeded();
            db.close();
        } catch (RuntimeException e) {
            logger.error("Exception thrown while closing DocIDServer", e);
        }
    }

    private void commitIfNeeded() {
        if (resumable) {
            db.commit();
        }
    }
}
