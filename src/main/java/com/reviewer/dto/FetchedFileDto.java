package com.reviewer.dto;

public class FetchedFileDto {
    private String path;
    private long size;

    public FetchedFileDto() {
    }

    public FetchedFileDto(String path, long size) {
        this.path = path;
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
