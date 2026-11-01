package com.reviewer.dto;

public class FetchedFile {
    private String path;
    private String content;
    private long size;

    public FetchedFile() {
    }

    public FetchedFile(String path, String content, long size) {
        this.path = path;
        this.content = content;
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
