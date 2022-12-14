/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package org.jboss.jandex;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream that reads integers that were packed by
 * {@link PackedDataOutputStream}
 *
 * <p>
 * <b>Thread-Safety</b>
 * </p>
 * This class is not thread-safe can <b>not<b> be shared between threads.
 *
 * @author Jason T. Greene
 */
class PackedDataInputStream extends DataInputStream {

    static final int MAX_1BYTE = 0x7F;

    public PackedDataInputStream(InputStream in) {
        super(in);
    }

    /**
     * Reads a packed unsigned integer. Every byte uses the first bit as a control bit to
     * signal when there are additional bytes to be read. The remaining seven bits are data.
     * Depending on the size of the number one to five bytes may be read.
     *
     * @return the unpacked integer
     *
     * @throws IOException
     */
    public int readPackedU32() throws IOException {
        byte b;
        int i = 0;

        do {
            b = readByte();
            i = (i << 7) | (b & MAX_1BYTE);
        } while ((b & 0x80) == 0x80);

        return i;
    }
}
