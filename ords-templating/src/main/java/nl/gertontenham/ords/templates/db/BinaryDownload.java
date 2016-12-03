package nl.gertontenham.ords.templates.db;

/**
 * Represents a binary downloadable stream including metadata
 */
public class BinaryDownload {

    private final BinaryFile binaryFile;
    private final String contentDisposition;

    public BinaryDownload(BinaryFile binaryFile, String contentDisposition) {
        this.binaryFile = binaryFile;
        this.contentDisposition = contentDisposition;
    }

    public BinaryFile getBinaryFile() {
        return binaryFile;
    }

    public String getContentDisposition() {
        return contentDisposition;
    }
}
