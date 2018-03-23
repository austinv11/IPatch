package injectr.ipatch.util;

import org.apache.commons.codec.digest.DigestUtils;

import java.security.MessageDigest;
import java.util.zip.Checksum;

public class MD5Checksum implements Checksum {

    private MessageDigest digest = DigestUtils.getMd5Digest();

    @Override
    public void update(int b) {
        digest.update((byte) b);
    }

    @Override
    public void update(byte[] b, int off, int len) {
        digest.update(b, off, len);
    }

    public void update(byte[] b) {
        update(b, 0, b.length);
    }

    @Override
    public long getValue() {
        throw new UnsupportedOperationException("Use getBytesValue() instead!");
    }

    public byte[] getBytesValue() {
        return digest.digest();
    }

    @Override
    public void reset() {
        digest.reset();
    }
}
