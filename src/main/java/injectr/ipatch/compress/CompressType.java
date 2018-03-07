package injectr.ipatch.compress;

public enum CompressType {
    NONE(""),
    TAR(".tar"),
    ZIP(".zip"),
    BROTLI(".br"),
    BZIP2(".bz2"),
    GZIP(".gz"),
    PACK200(".pack"),
    XZ(".xz"),
    Z(".Z"),
    LZMA(".lzma"),
    SNAPPY(".sz"),
    LZ4(".lz4"),
    ZSTANDARD(".zstd");

    private final String extension;

    CompressType(String extension) {
        this.extension = extension;
    }

    @Override
    public String toString() {
        return extension;
    }
}
