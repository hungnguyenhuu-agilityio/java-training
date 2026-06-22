package com.taskflow.domain;

import java.util.List;

public class PageResponse<T> {

    public static class Pagination {
        private final int page;
        private final int size;
        private final long totalElements;
        private final int totalPages;

        public Pagination(int page, int size, long totalElements) {
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        }

        public int getPage()           { return page; }
        public int getSize()           { return size; }
        public long getTotalElements() { return totalElements; }
        public int getTotalPages()     { return totalPages; }
    }

    private final List<T> data;
    private final Pagination pagination;

    public PageResponse(List<T> data, int page, int size, long totalElements) {
        this.data = data;
        this.pagination = new Pagination(page, size, totalElements);
    }

    public List<T> getData()           { return data; }
    public Pagination getPagination()  { return pagination; }
}
