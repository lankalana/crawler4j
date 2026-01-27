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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.github.lankalana.crawler4j.url.WebURL;

/**
 * @author Yasser Ganjisaffar
 */
public class WebURLSerializer {

    public byte[] toBytes(WebURL url) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(buffer)) {
            writeString(output, url.getURL());
            output.writeInt(url.getDocid());
            output.writeInt(url.getParentDocid());
            writeString(output, url.getParentUrl());
            output.writeShort(url.getDepth());
            output.writeByte(url.getPriority());
            writeString(output, url.getAnchor());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize WebURL", e);
        }
        return buffer.toByteArray();
    }

    public WebURL fromBytes(byte[] data) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(data))) {
            WebURL webURL = new WebURL();
            String url = readString(input);
            if (url == null) {
                throw new IllegalStateException("Missing URL in serialized WebURL");
            }
            webURL.setURL(url);
            webURL.setDocid(input.readInt());
            webURL.setParentDocid(input.readInt());
            webURL.setParentUrl(readString(input));
            webURL.setDepth(input.readShort());
            webURL.setPriority(input.readByte());
            webURL.setAnchor(readString(input));
            return webURL;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize WebURL", e);
        }
    }

    private static void writeString(DataOutputStream output, String value) throws IOException {
        if (value == null) {
            output.writeInt(-1);
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private static String readString(DataInputStream input) throws IOException {
        int length = input.readInt();
        if (length < 0) {
            return null;
        }
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
