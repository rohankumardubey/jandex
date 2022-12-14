package org.jboss.jandex.test.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexWriter;

public class IndexingUtil {
    public static Index roundtrip(Index index) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new IndexWriter(bytes).write(index);
        return new IndexReader(new ByteArrayInputStream(bytes.toByteArray())).read();
    }
}
