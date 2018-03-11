/*
 * This file is part of Discord4J.
 *
 * Discord4J is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Discord4J is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Discord4J.  If not, see <http://www.gnu.org/licenses/>.
 */
package injectr.ipatch.bytecode;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public abstract class ConstantPoolInfo {
    //Constant pool tags: https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4-150
    public static final char CONSTANT_class = 7;
    public static final char CONSTANT_Fieldref = 9;
    public static final char CONSTANT_Methodref = 10;
    public static final char CONSTANT_InterfaceMethodref = 11;
    public static final char CONSTANT_String = 8;
    public static final char CONSTANT_Integer = 3;
    public static final char CONSTANT_Float = 4;
    public static final char CONSTANT_Long = 5;
    public static final char CONSTANT_Double = 6;
    public static final char CONSTANT_NameAndType = 12;
    public static final char CONSTANT_Utf8 = 1;
    public static final char CONSTANT_MethodHandle = 15;
    public static final char CONSTANT_MethodType = 16;
    public static final char CONSTANT_InvokeDynamic = 18;
    public static final char CONSTANT_Module = 19;
    public static final char CONSTANT_Package = 20;

    public static ConstantPoolInfo[] readClassPoolInfo(int constant_pool_count, DataInputStream stream) throws IOException {
        ConstantPoolInfo[] info = new ConstantPoolInfo[constant_pool_count]; //According to the spec you have to do this /shrug
        for (int i = 1; i < constant_pool_count; i++) { //1 indexing for some reason
            char tag = (char) stream.readByte();
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
                    info[i] = new MethodHandleInfo(tag, (char) stream.readByte(), stream.readUnsignedShort());
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

    private final char tag; //Unsigned byte

    public ConstantPoolInfo(char tag) {
        this.tag = tag;
    }

    /**
     * Specifies the constant pool info type.
     */
    public char getTag() {
        return tag;
    }

    public abstract char[] getInfo();

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.4.1
     */
    public static final class ClassInfo extends ConstantPoolInfo {

        private final int name_index; //Unsigned short

        public ClassInfo(char tag, int name_index) {
            super(tag);
            this.name_index = name_index;
        }

        @Override
        public char[] getInfo() {
            return new char[] {(char) (name_index >> 8) , (char) (name_index & 0xFF)};
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

        public RefInfo(char tag, int class_index, int name_and_type_index) {
            super(tag);
            this.class_index = class_index;
            this.name_and_type_index = name_and_type_index;
        }

        @Override
        public char[] getInfo() {
            return new char[] {(char) (class_index >> 8) , (char) (class_index & 0xFF),
                    (char) (name_and_type_index >> 8) , (char) (name_and_type_index & 0xFF)};
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

        public StringInfo(char tag, int string_index) {
            super(tag);
            this.string_index = string_index;
        }

        @Override
        public char[] getInfo() {
            return new char[] {(char) (string_index >> 8) , (char) (string_index & 0xFF)};
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

        public IntegerFloatInfo(char tag, int bytes) {
            super(tag);
            this.bytes = bytes;
        }

        @Override
        public char[] getInfo() {
            return new char[] {(char) (bytes >> 24),
                    (char)((bytes >> 16) & 0xFF),
                    (char)((bytes >> 8) & 0xFF),
                    (char) (bytes & 0xFF)};
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

        public LongDoubleInfo(char tag, int high_bytes, int low_bytes) {
            super(tag);
            this.high_bytes = high_bytes;
            this.low_bytes = low_bytes;
        }

        @Override
        public char[] getInfo() {
            return new char[] {(char) (high_bytes >> 24),
                    (char)((high_bytes >> 16) & 0xFF),
                    (char)((high_bytes >> 8) & 0xFF),
                    (char) (high_bytes & 0xFF),
                    (char) (low_bytes >> 24),
                    (char)((low_bytes >> 16) & 0xFF),
                    (char)((low_bytes >> 8) & 0xFF),
                    (char) (low_bytes & 0xFF)};
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

        public NameAndTypeInfo(char tag, int name_index, int descriptor_index) {
            super(tag);
            this.name_index = name_index;
            this.descriptor_index = descriptor_index;
        }

        @Override
        public char[] getInfo() {
            return new char[] {(char) (name_index >> 24),
                    (char)((name_index >> 16) & 0xFF),
                    (char)((name_index >> 8) & 0xFF),
                    (char) (name_index & 0xFF),
                    (char) (descriptor_index >> 24),
                    (char)((descriptor_index >> 16) & 0xFF),
                    (char)((descriptor_index >> 8) & 0xFF),
                    (char) (descriptor_index & 0xFF)};
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

        public Utf8Info(char tag, int length, byte[] bytes) {
            super(tag);
            this.length = length;
            this.bytes = bytes;
        }

        @Override
        public char[] getInfo() {
            char[] info = new char[2 + length];
            info[0] = (char) (length >> 8);
            info[1] = (char) (length & 0xFF);
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

        private final char reference_kind;
        private final int reference_index; //Unsigned short

        public MethodHandleInfo(char tag, char reference_kind, int reference_index) {
            super(tag);
            this.reference_kind = reference_kind;
            this.reference_index = reference_index;
        }

        @Override
        public char[] getInfo() {
            return new char[] {reference_kind,
                    (char) (reference_index >> 8),
                    (char) (reference_index & 0xFF)};
        }

        public char getReferenceKind() {
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

        public MethodTypeInfo(char tag, int descriptor_index) {
            super(tag);
            this.descriptor_index = descriptor_index;
        }

        @Override
        public char[] getInfo() {
            return new char[]{(char) (descriptor_index >> 8), (char) (descriptor_index & 0xFF)};
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

        public InvokeDynamicInfo(char tag, int bootstrap_method_attr_index, int name_and_type_index) {
            super(tag);
            this.bootstrap_method_attr_index = bootstrap_method_attr_index;
            this.name_and_type_index = name_and_type_index;
        }

        @Override
        public char[] getInfo() {
            return new char[] {(char) (bootstrap_method_attr_index >> 8) , (char) (bootstrap_method_attr_index & 0xFF),
                    (char) (name_and_type_index >> 8) , (char) (name_and_type_index & 0xFF)};
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

        public ModuleInfo(char tag, int name_index) {
            super(tag);
            this.name_index = name_index;
        }

        @Override
        public char[] getInfo() {
            return new char[]{(char) (name_index >> 8), (char) (name_index & 0xFF)};
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

        public PackageInfo(char tag, int name_index) {
            super(tag);
            this.name_index = name_index;
        }

        @Override
        public char[] getInfo() {
            return new char[]{(char) (name_index >> 8), (char) (name_index & 0xFF)};
        }

        public int getNameIndex() {
            return name_index;
        }
    }
}
