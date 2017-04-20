package edu.virginia.lib.aptrust.bags;

import java.io.File;

public class PendingPayloadFile {

    private String path;

    private File file;

    public PendingPayloadFile(final File file) {
        this.file = file;
        this.path = file.getName();
    }

    public PendingPayloadFile(final File file, final String path) {
        this.file = file;
        this.path = path;
    }

    public String getPathWithinPayload() {
        return path;
    }

    public File getFile() {
        return file;
    }

}
