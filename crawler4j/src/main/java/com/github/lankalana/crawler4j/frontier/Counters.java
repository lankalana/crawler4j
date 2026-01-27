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

package com.github.lankalana.crawler4j.frontier;

import java.io.File;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import com.github.lankalana.crawler4j.crawler.CrawlConfig;

/**
 * @author Yasser Ganjisaffar
 */
public class Counters {
    private static final Logger logger = LoggerFactory.getLogger(Counters.class);

    public static class ReservedCounterNames {
        public static final String SCHEDULED_PAGES = "Scheduled-Pages";
        public static final String PROCESSED_PAGES = "Processed-Pages";
    }

    private static final String DATABASE_NAME = "Statistics";
    private final DB db;
    private CrawlConfig config;
    private final boolean resumable;

    protected final Object mutex = new Object();

    protected Map<String, Long> counterValues;

    public Counters(File storageFolder, CrawlConfig config) {
        this.config = config;
        this.resumable = config.isResumableCrawling();

    /*
     * When crawling is set to be resumable, we have to keep the statistics
     * in a transactional database to make sure they are not lost if crawler
     * is crashed or terminated unexpectedly.
     */
        File dbFile = new File(storageFolder, DATABASE_NAME + ".db");
        DBMaker.Maker maker = DBMaker.fileDB(dbFile).fileMmapEnableIfSupported();
        if (resumable) {
            maker = maker.transactionEnable();
        }
        db = maker.make();
        counterValues = db.hashMap(DATABASE_NAME, Serializer.STRING, Serializer.LONG).createOrOpen();
    }

    public long getValue(String name) {
        synchronized (mutex) {
            Long value = counterValues.get(name);
            if (value == null) {
                return 0;
            }
            return value;
        }
    }

    public void setValue(String name, long value) {
        synchronized (mutex) {
            try {
                counterValues.put(name, value);
                commitIfNeeded();
            } catch (RuntimeException e) {
                if (config.isHaltOnError()) {
                    throw e;
                } else {
                    logger.error("Exception setting value", e);
                }
            }
        }
    }

    public void increment(String name) {
        increment(name, 1);
    }

    public void increment(String name, long addition) {
        synchronized (mutex) {
            long prevValue = getValue(name);
            setValue(name, prevValue + addition);
        }
    }

    public void close() {
        try {
            commitIfNeeded();
            db.close();
        } catch (RuntimeException e) {
            logger.error("Exception thrown while trying to close statisticsDB", e);
        }
    }

    private void commitIfNeeded() {
        if (resumable) {
            db.commit();
        }
    }
}
