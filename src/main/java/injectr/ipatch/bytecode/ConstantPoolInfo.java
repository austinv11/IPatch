package injectr.ipatch.bytecode;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public abstract class ConstantPoolInfo {
    //Constant pool tags: https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4-150
    public static final byte CONSTANT_class = 7;
    public static final byte CONSTANT_Fieldref = 9;
    public static final byte CONSTANT_Methodref = 10;
    public static final byte CONSTANT_InterfaceMethodref = 11;
    public static final byte CONSTANT_String = 8;
    public static final byte CONSTANT_Integer = 3;
    public static final byte CONSTANT_Float = 4;
    public static final byte CONSTANT_Long = 5;
    public static final byte CONSTANT_Double = 6;
    public static final byte CONSTANT_NameAndType = 12;
    public static final byte CONSTANT_Utf8 = 1;
    public static final byte CONSTANT_MethodHandle = 15;
    public static final byte CONSTANT_MethodType = 16;
    public static final byte CONSTANT_InvokeDynamic = 18;
    public static final byte CONSTANT_Module = 19;
    public static final byte CONSTANT_Package = 20;

    public static ConstantPoolInfo[] readClassPoolInfo(int constant_pool_count, DataInputStream stream) throws IOException {
        ConstantPoolInfo[] info = new ConstantPoolInfo[constant_pool_count]; //According to the spec you have to do this /shrug
        for (int i = 1; i < constant_pool_count; i++) { //1 indexing for some reason
            byte tag = stream.readByte();
            switch (tag) {
                case CONSTANT_class:
                    info[i] = new ClassInfo(tag, stream.readUnsignedShort());
                    break;
                case CONSTANT_Fieldref:
                case CONSTANT_Methodref:
                case CONSTANT_InterfaceMethodref:
                    info[i] = new RefInfo(tag, stream.readUnsignedShort(), stream.readUnsignedShort());
                    break;
                case CONSTANT_String:
                    info[i] = new StringInfo(tag, stream.readUnsignedShort());
                    break;
                case CONSTANT_Integer:
                case CONSTANT_Float:
                    info[i] = new IntegerFloatInfo(tag, stream.readInt());
                    break;
                case CONSTANT_Long:
                case CONSTANT_Double:
                    info[i] = new LongDoubleInfo(tag, stream.readInt(), stream.readInt());
                    break;
                case CONSTANT_NameAndType:
                    info[i] = new NameAndTypeInfo(tag, stream.readUnsignedShort(), stream.readUnsignedShort());
                    break;
                case CONSTANT_Utf8:
                    int length = stream.readUnsignedShort();
                    byte[] data = new byte[length];
                    stream.readFully(data, 0, length);
                    info[i] = new Utf8Info(tag, length, data);
                    break;
                case CONSTANT_MethodHandle:
                    info[i] = new MethodHandleInfo(tag, (byte) stream.readByte(), stream.readUnsignedShort());
                    break;
                case CONSTANT_MethodType:
                    info[i] = new MethodTypeInfo(tag, stream.readUnsignedShort());
                    break;
                case CONSTANT_InvokeDynamic:
                    info[i] = new InvokeDynamicInfo(tag, stream.readUnsignedShort(), stream.readUnsignedShort());
                    break;
                case CONSTANT_Module:
                    info[i] = new ModuleInfo(tag, stream.readUnsignedShort());
                    break;
                case CONSTANT_Package:
                    info[i] = new PackageInfo(tag, stream.readUnsignedShort());
                    break;
                default:
                    throw new IOException("Read invalid constant pool info type " + (int) tag);
            }
        }
        return info;
    }

    private final byte tag; //Unsigned byte

    public ConstantPoolInfo(byte tag) {
        this.tag = tag;
    }

    /**
     * Specifies the constant pool info type.
     */
    public byte getTag() {
        return tag;
    }

    public abstract byte[] getInfo();

    public byte[] toBytes() {
        byte[] info = getInfo();
        byte[] dat = new byte[info.length + 1];
        dat[0] = tag;
        System.arraycopy(info, 0, dat, 1, info.length);
        return dat;
    }

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.1
     */
    public static final class ClassInfo extends ConstantPoolInfo {

        private final int name_index; //Unsigned short

        public ClassInfo(byte tag, int name_index) {
            super(tag);
            this.name_index = name_index;
        }

        @Override
        public byte[] getInfo() {
            return new byte[] {(byte) (name_index >> 8) , (byte) (name_index & 0xFF)};
        }

        public int getNameIndex() {
            return name_index;
        }
    }

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.2
     */
    public static final class RefInfo extends ConstantPoolInfo { //Represents Fieldref_info, Methodref_info and InterfaceMethodref_info

        private final int class_index; //Unsigned short
        private final int name_and_type_index; //Unsigned short

        public RefInfo(byte tag, int class_index, int name_and_type_index) {
            super(tag);
            this.class_index = class_index;
            this.name_and_type_index = name_and_type_index;
        }

        @Override
        public byte[] getInfo() {
            return new byte[] {(byte) (class_index >> 8), (byte) (class_index & 0xFF),
                    (byte) (name_and_type_index >> 8), (byte) (name_and_type_index & 0xFF)};
        }

        public int getClassIndex() {
            return class_index;
        }

        public int getNameAndTypeIndex() {
            return name_and_type_index;
        }
    }

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.3
     */
    public static final class StringInfo extends ConstantPoolInfo {

        private final int string_index; //Unsigned short

        public StringInfo(byte tag, int string_index) {
            super(tag);
            this.string_index = string_index;
        }

        @Override
        public byte[] getInfo() {
            return new byte[] {(byte) (string_index >> 8) , (byte) (string_index & 0xFF)};
        }

        public int getStringIndex() {
            return string_index;
        }

        public String getString(ConstantPoolInfo[] constant_pool) {
            Utf8Info str = (Utf8Info) constant_pool[getStringIndex()];
            return str.getString();
        }
    }

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.4
     */
    public static final class IntegerFloatInfo extends ConstantPoolInfo {

        private final int bytes;

        public IntegerFloatInfo(byte tag, int bytes) {
            super(tag);
            this.bytes = bytes;
        }

        @Override
        public byte[] getInfo() {
            return new byte[] {(byte) (bytes >> 24),
                    (byte)((bytes >> 16) & 0xFF),
                    (byte)((bytes >> 8) & 0xFF),
                    (byte) (bytes & 0xFF)};
        }

        public int getBytes() {
            return bytes;
        }

        public int getInt() {
            return bytes;
        }

        public float getFloat() {
            return Float.intBitsToFloat(bytes);
        }
    }

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.5
     */
    public static final class LongDoubleInfo extends ConstantPoolInfo {

        private final int high_bytes;
        private final int low_bytes;

        public LongDoubleInfo(byte tag, int high_bytes, int low_bytes) {
            super(tag);
            this.high_bytes = high_bytes;
            this.low_bytes = low_bytes;
        }

        @Override
        public byte[] getInfo() {
            return new byte[] {(byte) (high_bytes >> 24),
                    (byte)((high_bytes >> 16) & 0xFF),
                    (byte)((high_bytes >> 8) & 0xFF),
                    (byte) (high_bytes & 0xFF),
                    (byte) (low_bytes >> 24),
                    (byte)((low_bytes >> 16) & 0xFF),
                    (byte)((low_bytes >> 8) & 0xFF),
                    (byte) (low_bytes & 0xFF)};
        }

        public int getHighBytes() {
            return high_bytes;
        }

        public int getLowBytes() {
            return low_bytes;
        }

        public long getConcatenatedBytes() {
            return ((long) high_bytes << 32) | (long) low_bytes;
        }

        public long getLong() {
            return getConcatenatedBytes();
        }

        public double getDouble() {
            return Double.longBitsToDouble(getConcatenatedBytes());
        }
    }

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.6
     */
    public static final class NameAndTypeInfo extends ConstantPoolInfo {

        private final int name_index; //Unsigned short
        private final int descriptor_index; //Unsigned short

        public NameAndTypeInfo(byte tag, int name_index, int descriptor_index) {
            super(tag);
            this.name_index = name_index;
            this.descriptor_index = descriptor_index;
        }

        @Override
        public byte[] getInfo() {
            return new byte[] {(byte)(name_index >> 8),
                    (byte) (name_index & 0xFF),
                    (byte) (descriptor_index >> 8),
                    (byte) (descriptor_index & 0xFF)};
        }

        public int getNameIndex() {
            return name_index;
        }

        public int getDescriptorIndex() {
            return descriptor_index;
        }
    }

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.7
     */
    public static final class Utf8Info extends ConstantPoolInfo {

        private final int length; //Unsigned short
        private final byte[] bytes;

        public Utf8Info(byte tag, int length, byte[] bytes) {
            super(tag);
            this.length = length;
            this.bytes = bytes;
        }

        @Override
        public byte[] getInfo() {
            byte[] info = new byte[2 + length];
            info[0] = (byte) (length >> 8);
            info[1] = (byte) (length & 0xFF);
            System.arraycopy(bytes, 0, info, 2, length);
            return info;
        }

        public int getLength() {
            return length;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public String getString() {
            try {
                return new String(bytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.8
     */
    public static final class MethodHandleInfo extends ConstantPoolInfo {

        private final byte reference_kind;
        private final int reference_index; //Unsigned short

        public MethodHandleInfo(byte tag, byte reference_kind, int reference_index) {
            super(tag);
            this.reference_kind = reference_kind;
            this.reference_index = reference_index;
        }

        @Override
        public byte[] getInfo() {
            return new byte[] {reference_kind,
                    (byte) (reference_index >> 8),
                    (byte) (reference_index & 0xFF)};
        }

        public byte getReferenceKind() {
            return reference_kind;
        }

        public int getReferenceIndex() {
            return reference_index;
        }
    }

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.9
     */
    public static final class MethodTypeInfo extends ConstantPoolInfo {

        private final int descriptor_index; //Unsigned short

        public MethodTypeInfo(byte tag, int descriptor_index) {
            super(tag);
            this.descriptor_index = descriptor_index;
        }

        @Override
        public byte[] getInfo() {
            return new byte[]{(byte) (descriptor_index >> 8), (byte) (descriptor_index & 0xFF)};
        }

        public int getDescriptorIndex() {
            return descriptor_index;
        }
    }

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.10
     */
    public static final class InvokeDynamicInfo extends ConstantPoolInfo {

        private final int bootstrap_method_attr_index; //Unsigned short
        private final int name_and_type_index; //Unsigned short

        public InvokeDynamicInfo(byte tag, int bootstrap_method_attr_index, int name_and_type_index) {
            super(tag);
            this.bootstrap_method_attr_index = bootstrap_method_attr_index;
            this.name_and_type_index = name_and_type_index;
        }

        @Override
        public byte[] getInfo() {
            return new byte[] {(byte) (bootstrap_method_attr_index >> 8) , (byte) (bootstrap_method_attr_index & 0xFF),
                    (byte) (name_and_type_index >> 8) , (byte) (name_and_type_index & 0xFF)};
        }

        public int getBootstrapMethodAttrIndex() {
            return bootstrap_method_attr_index;
        }

        public int getNameAndTypeIndex() {
            return name_and_type_index;
        }
    }

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.11
     */
    public static final class ModuleInfo extends ConstantPoolInfo {

        private final int name_index; //Unsigned short

        public ModuleInfo(byte tag, int name_index) {
            super(tag);
            this.name_index = name_index;
        }

        @Override
        public byte[] getInfo() {
            return new byte[]{(byte) (name_index >> 8), (byte) (name_index & 0xFF)};
        }

        public int getNameIndex() {
            return name_index;
        }
    }

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.12
     */
    public static final class PackageInfo extends ConstantPoolInfo {

        private final int name_index; //Unsigned short

        public PackageInfo(byte tag, int name_index) {
            super(tag);
            this.name_index = name_index;
        }

        @Override
        public byte[] getInfo() {
            return new byte[]{(byte) (name_index >> 8), (byte) (name_index & 0xFF)};
        }

        public int getNameIndex() {
            return name_index;
        }
    }
}
