package injectr.ipatch.util;

public class BytesUtil {

    /**
     * Generates an empty buffer using a 2d char array data structure which has the specified total capacity.
     */
    public static char[][] allocate(long n) {
        int requiredArrays = (int) ((n / ((long) Integer.MAX_VALUE)) + 1);
        char[][] arrays = new char[requiredArrays][];
        for (int i = 0; i < requiredArrays; i++) {
            char[] currArray = n == requiredArrays - 1 ? new char[(int) n] : new char[Integer.MAX_VALUE];
            n -= Integer.MAX_VALUE;
            arrays[i] = currArray;
        }
        return arrays;
    }

    /**
     * Inserts the specified data into the specified buffer with the given offset and returns the amount of data inserted..
     * Note: Expects a buffer in the form returned from {@link #allocate(long)}.
     */
    public static int insert(char[] data, char[][] buf, long offset) {
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
                char[] currArray = buf[i];
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
}
