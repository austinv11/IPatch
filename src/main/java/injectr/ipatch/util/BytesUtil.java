package injectr.ipatch.util;

import injectr.ipatch.bytecode.ClassFile;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.ChecksumCalculatingInputStream;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class BytesUtil {

    public static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 5; //Based on Hotspot VM, Source: https://stackoverflow.com/a/3039805

    /**
     * Generates an empty buffer using a 2d byte array data structure which has the specified total capacity.
     */
    public static byte[][] allocate(long n) {
        int requiredArrays = (int) ((n / ((long) MAX_ARRAY_SIZE)) + 1);
        byte[][] arrays = new byte[requiredArrays][];
        for (int i = 0; i < requiredArrays; i++) {
            byte[] currArray = i == requiredArrays - 1 ? new byte[(int) n] : new byte[MAX_ARRAY_SIZE];
            n -= MAX_ARRAY_SIZE;
            arrays[i] = currArray;
        }
        return arrays;
    }

    /**
     * Inserts the specified data into the specified buffer with the given offset and returns the amount of data inserted..
     * Note: Expects a buffer in the form returned from {@link #allocate(long)}.
     */
    public static int insert(byte[] data, byte[][] buf, long offset) {
        if (buf.length == 0)
            throw new RuntimeException("Buffer too small!");

        if (buf.length == 1) {
            if (data.length >  buf[0].length - offset)
                throw new RuntimeException("Buffer too small!");

            System.arraycopy(data, 0, buf[0], Math.toIntExact(offset), data.length);
        } else {
            int normalArrayLen = buf[0].length;
            int lastArrayLen = buf[buf.length - 1].length;

            if ((normalArrayLen * (buf.length - 1)) + lastArrayLen - offset < data.length)
                throw new RuntimeException("Buffer too small!");

            int arraysToSkip = (int) (offset / (long) normalArrayLen);
            offset -= ((long) arraysToSkip) * ((long) normalArrayLen);
            int dataPointer = 0;
            for (int i = arraysToSkip; dataPointer < data.length; i++) {
                byte[] currArray = buf[i];
                int startIndex = (int) offset;
                int len = Math.min(currArray.length - startIndex, data.length);
                System.arraycopy(data, dataPointer, currArray, startIndex, len);
                dataPointer += offset;
                if (offset > 0)
                    offset = 0;
            }
        }

        return data.length;
    }

    public static byte[] shortToBytes(int i) {
        return new byte[] {(byte) (i >> 8), (byte) (i & 0xFF)};
    }

    public static byte[] intToBytes(int i) {
        return new byte[] {(byte) (i >> 24), (byte) ((i >> 16) & 0xFF), (byte) ((i >> 8) & 0xFF), (byte) (i & 0xFF)};
    }

    public static ClassFile readAndVerify(InputStream stream) throws IOException {
        MD5Checksum checksum = new MD5Checksum();
        try (ChecksumCalculatingInputStream checksumStream = new ChecksumCalculatingInputStream(checksum, stream)) {
            ClassFile clazz = ClassFile.readFrom(checksumStream);
            clazz.writeTo(new FileOutputStream("./test2.class"));
            byte[] originalChecksum = checksum.getBytesValue();
            byte[] newChecksum = clazz.checksum();
            if (!Arrays.equals(originalChecksum, newChecksum)) {
                throw new IOException("Invalid checkums!");
            }
            return clazz;
        }
    }
}
