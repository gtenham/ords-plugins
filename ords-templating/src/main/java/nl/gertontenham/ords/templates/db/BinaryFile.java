package nl.gertontenham.ords.templates.db;

import java.io.InputStream;

/**
 * Represents a binary stream including metadata
 */
public class BinaryFile {
    private final InputStream blobStream;
    private final long size;
    private final String name;
    private final String contentType;


    public BinaryFile(InputStream blobStream, long size,
                      String name, String contentType) {
        this.blobStream = blobStream;
        this.size = size;
        this.name = name;
        this.contentType = contentType;
    }

    public InputStream getBlobStream() {
        return blobStream;
    }

    public long getSize() {
        return size;
    }

    public String getName() {
        return name;
    }

    public String getContentType() {
        return contentType;
    }



}
