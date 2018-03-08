package injectr.ipatch.bytecode;

import java.io.*;

/**
 * Object representation of a compiled classfile
 * See: https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html
 *
 * Current max supported jvm version: 9
 */
public final class ClassFile {

    /**
     * Magic value to identify the class file format
     */
    public static final int MAGIC = 0xCAFEBABE;

    // Access flags: https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.1-200-E.1
    public static final int ACC_PUBLIC = 0x0001;
    public static final int ACC_FINAL = 0x0010;
    public static final int ACC_SUPER = 0x0020;
    public static final int ACC_INTERFACE = 0x0200;
    public static final int ACC_ABSTRACT = 0x0400;
    public static final int ACC_SYNTHETIC = 0x1000;
    public static final int ACC_ANNOTATION = 0x2000;
    public static final int ACC_ENUM = 0x4000;
    public static final int ACC_MODULE = 0x8000;

    public static ClassFile readFrom(InputStream stream) throws IOException {
        DataInputStream data = new DataInputStream(stream);

        int magic = data.readInt();
        if (magic != MAGIC)
            throw new IOException("Magic value does not match 0xCAFEBABE! This is an invalid class!");

        int minor_version = data.readUnsignedShort();
        int major_version = data.readUnsignedShort();

        int constant_pool_count = data.readUnsignedShort();
        ConstantPoolInfo[] constant_pool = ConstantPoolInfo.readClassPoolInfo(constant_pool_count, data);

        int access_flags = data.readUnsignedShort();

        int this_class = data.readUnsignedShort();
        int super_class = data.readUnsignedShort();

        int interfaces_count = data.readUnsignedShort();
        int[] interfaces = new int[interfaces_count];
        for (int i = 0; i < interfaces_count; i++) {
            interfaces[i] = data.readUnsignedShort();
        }

        int fields_count = data.readUnsignedShort();

        data.close();
        return new ClassFile(minor_version, major_version, constant_pool_count, constant_pool,
                access_flags, this_class, super_class, interfaces_count, interfaces, fields_count);
    }

    public static ClassFile readFrom(byte[] bytes) throws IOException {
        return readFrom(new ByteArrayInputStream(bytes));
    }

    private final int minor_version, major_version; //Unsigned!
    private final int constant_pool_count; //Unsigned short!
    private final ConstantPoolInfo[] constant_pool;
    private final int access_flags; //Unsigned short!
    private final int this_class; //Unsigned short!
    private final int super_class; //Unsigned short!
    private final int interfaces_count; //Unsigned short!
    private final int[] interfaces; //Unsigned shorts!
    private final int fields_count; //Unsigned short!

    public ClassFile(int minor_version, int major_version, int constant_pool_count, ConstantPoolInfo[] constant_pool, int access_flags, int this_class, int super_class, int interfaces_count, int[] interfaces, int fields_count) {
        this.minor_version = minor_version;
        this.major_version = major_version;
        this.constant_pool_count = constant_pool_count;
        this.constant_pool = constant_pool;
        this.access_flags = access_flags;
        this.this_class = this_class;
        this.super_class = super_class;
        this.interfaces_count = interfaces_count;
        this.interfaces = interfaces;
        this.fields_count = fields_count;
    }

    public static abstract class ConstantPoolInfo {
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

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.5
     */
    public static final class FieldInfo {

        //Access flags: https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.5-200-A.1
        public static final int ACC_PUBLIC = 0x0001;
        public static final int ACC_PRIVATE = 0x0002;
        public static final int ACC_PROTECTED = 0x0004;
        public static final int ACC_STATIC = 0x0008;
        public static final int ACC_FINAL = 0x0010;
        public static final int ACC_VOLATILE = 0x0040;
        public static final int ACC_TRANSIENT = 0x0080;
        public static final int ACC_SYNTHETIC = 0x1000;
        public static final int ACC_ENUM = 0x4000;

        public static FieldInfo[] readFieldInfo(int field_info_count, DataInputStream stream) {

        }

        private final int access_flags; //Unsigned short
        private final int name_index; //Unsigned short
        private final int descriptor_index; //Unsigned short
        private final int attributes_count; //Unsigned short
        private final AttributeInfo[] attributes;

        public FieldInfo(int access_flags, int name_index, int descriptor_index, int attributes_count, AttributeInfo[] attributes) {
            this.access_flags = access_flags;
            this.name_index = name_index;
            this.descriptor_index = descriptor_index;
            this.attributes_count = attributes_count;
            this.attributes = attributes;
        }

        public int getAccessFlags() {
            return access_flags;
        }

        public int getNameIndex() {
            return name_index;
        }

        public int getDescriptorIndex() {
            return descriptor_index;
        }

        public int getAttributesCount() {
            return attributes_count;
        }

        public AttributeInfo[] getAttributes() {
            return attributes;
        }
    }

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7
     */
    public static abstract class AttributeInfo {

        //Attribute keys = https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7-300
        public static final String ConstantValue = "ConstantValue";
        public static final String Code = "Code";
        public static final String StackMapTable = "StackMapTable";
        public static final String Exceptions = "Exceptions";
        public static final String InnerClasses = "InnerClasses";
        public static final String EnclosingMethod = "EnclosingMethod";
        public static final String Synthetic = "Synthetic";
        public static final String Signature = "Signature";
        public static final String SourceFile = "SourceFile";
        public static final String SourceDebugExtension = "SourceDebugExtension";
        public static final String LineNumberTable = "LineNumberTable";
        public static final String LocalVariableTable = "LocalVariableTable";
        public static final String LocalVariableTypeTable = "LocalVariableTypeTable";
        public static final String Deprecated = "Deprecated";
        public static final String RuntimeVisibleAnnotations = "RuntimeVisibleAnnotations";
        public static final String RuntimeInvisibleAnnotations = "RuntimeInvisibleAnnotations";
        public static final String RuntimeVisibleParameterAnnotations = "RuntimeVisibleParameterAnnotations";
        public static final String RuntimeInvisibleParameterAnnotations = "RuntimeInvisibleParameterAnnotations";
        public static final String RuntimeVisibleTypeAnnotations = "RuntimeVisibleTypeAnnotations";
        public static final String RuntimeInvisibleTypeAnnotations = "RuntimeInvisibleTypeAnnotations";
        public static final String AnnotationDefault = "AnnotationDefault";
        public static final String BootstrapMethods = "BootstrapMethods";
        public static final String MethodParameters = "MethodParameters";
        public static final String Module = "Module";
        public static final String ModulePackages = "ModulePackages";
        public static final String ModuleMainClass = "ModuleMainClass";

        public static AttributeInfo[] readAttributeInfo(int attribute_info_count, ConstantPoolInfo[] constant_pool, DataInputStream stream) {

        }

        private final int attribute_name_index; //Unsigned short
        private final long attribute_length; //Unsigned int

        public AttributeInfo(int attribute_name_index, long attribute_length) {
            this.attribute_name_index = attribute_name_index;
            this.attribute_length = attribute_length;
        }

        public int getAttributeNameIndex() {
            return attribute_name_index;
        }

        public long getAttributeLength() {
            return attribute_length;
        }

        /**
         * Since java arrays do not accept long sizes, data is loaded into a byte array, if this array overflows,
         * it moves into another array, and so on.
         */
        public abstract char[][] getInfo();

        /**
         * Gets the length of the attribute info property in terms of bytes. Useful for buffer allocation.
         */
        public abstract long getInfoByteLength();

        /**
         * Generates an empty buffer using a 2d char array data structure which has the specified total capacity.
         */
        protected char[][] allocate(long n) {
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
        protected int insert(char[] data, char[][] buf, long offset) {
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

        /**
         * Placeholder for unknown attributes as non standard jvm impls may use them
         */
        public static class DefaultAttributeInfo extends AttributeInfo {

            private final char[][] bytes;

            public DefaultAttributeInfo(int attribute_name_index, long attribute_length, char[][] bytes) {
                super(attribute_name_index, attribute_length);
                this.bytes = bytes;
            }

            @Override
            public char[][] getInfo() {
                return bytes;
            }

            @Override
            public long getInfoByteLength() {
                if (bytes.length == 0)
                    return 0;
                if (bytes.length == 1)
                    return bytes[0].length;
                int firstLen = bytes[0].length;
                int lastLen = bytes[bytes.length-1].length;
                return (((long) firstLen) * ((long) bytes.length - 1L)) + ((long) lastLen);
            }
        }

        /**
         * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.2
         */
        public static final class ConstantValueInfo extends AttributeInfo {

            private final int constantvalue_index; //Unsigned short

            public ConstantValueInfo(int attribute_name_index, long attribute_length, int constantvalue_index) {
                super(attribute_name_index, attribute_length);
                this.constantvalue_index = constantvalue_index;
            }

            @Override
            public char[][] getInfo() {
                return new char[][]{{(char) ((constantvalue_index >> 8) & 0xFF),
                        (char) (constantvalue_index & 0xFF)}};
            }

            @Override
            public long getInfoByteLength() {
                return 2;
            }

            public int getConstantValueIndex() {
                return constantvalue_index;
            }
        }

        /**
         * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.3
         */
        public static final class CodeInfo extends AttributeInfo {

            private final int max_stack; //Unsigned short
            private final int max_locals; //Unsigned short
            private final long code_length; //Unsigned int
            private final char[][] code; //Array with overflow support
            private final int exception_table_length; //Unsigned short
            private final ExceptionTableInfo[] exception_table;
            private final int attributes_count; //Unsigned short
            private final AttributeInfo[] attributes;

            public CodeInfo(int attribute_name_index, long attribute_length, int max_stack, int max_locals, long code_length, char[][] code, int exception_table_length, ExceptionTableInfo[] exception_table, int attributes_count, AttributeInfo[] attributes) {
                super(attribute_name_index, attribute_length);
                this.max_stack = max_stack;
                this.max_locals = max_locals;
                this.code_length = code_length;
                this.code = code;
                this.exception_table_length = exception_table_length;
                this.exception_table = exception_table;
                this.attributes_count = attributes_count;
                this.attributes = attributes;
            }

            @Override
            public char[][] getInfo() {
                char[][] buf = allocate(getInfoByteLength());
                int inserted = insert
                        (new char[]{(char) ((max_stack >> 8) & 0xFF),
                                (char) (max_stack & 0xFF),
                                (char) ((max_locals >> 8) & 0xFF),
                                (char) (max_locals & 0xFF),
                                (char) (code_length >> 24),
                                (char)((code_length >> 16) & 0xFF),
                                (char)((code_length >> 8) & 0xFF),
                                (char) (code_length & 0xFF)}, buf, 0);

                for (char[] chunk : code) {
                    inserted += insert(chunk, buf, inserted);
                }

                inserted += insert(new char[]{(char) ((exception_table_length >> 8) & 0xFF),
                        (char) (exception_table_length & 0xFF)}, buf, inserted);

                for (ExceptionTableInfo tableInfo : exception_table) {
                    inserted += insert(tableInfo.getBytes(), buf, inserted);
                }

                inserted += insert(new char[]{(char) ((attributes_count >> 8) & 0xFF),
                        (char) (attributes_count & 0xFF)}, buf, inserted);

                for (AttributeInfo attributeInfo : attributes) {
                    for (char[] chunk : attributeInfo.getInfo()) {
                        inserted += insert(chunk, buf, inserted);
                    }
                }

                return buf;
            }

            @Override
            public long getInfoByteLength() {
                long nestedAttributesLength = 0L;
                for (AttributeInfo info : attributes)
                    nestedAttributesLength += info.getInfoByteLength();
                return (2L * 4L) //Unsigned shorts
                        + (4L) //Unsigned int
                        + code_length
                        + (((long) exception_table_length) * 8L) //4 unsigned shorts per table
                        + nestedAttributesLength;
            }

            public int getMaxStack() {
                return max_stack;
            }

            public int getMaxLocals() {
                return max_locals;
            }

            public long getCodeLength() {
                return code_length;
            }

            public char[][] getCode() {
                return code;
            }

            public int getExceptionTableLength() {
                return exception_table_length;
            }

            public ExceptionTableInfo[] getExceptionTable() {
                return exception_table;
            }

            public int getAttributesCount() {
                return attributes_count;
            }

            public AttributeInfo[] getAttributes() {
                return attributes;
            }

            public static final class ExceptionTableInfo {

                private final int start_pc; //Unsigned short
                private final int end_pc; //Unsigned short
                private final int handler_pc; //Unsigned short
                private final int catch_type; //Unsigned short

                public ExceptionTableInfo(int start_pc, int end_pc, int handler_pc, int catch_type) {
                    this.start_pc = start_pc;
                    this.end_pc = end_pc;
                    this.handler_pc = handler_pc;
                    this.catch_type = catch_type;
                }

                public int getStartPc() {
                    return start_pc;
                }

                public int getEndPc() {
                    return end_pc;
                }

                public int getHandlerPc() {
                    return handler_pc;
                }

                public int getCatchType() {
                    return catch_type;
                }

                public char[] getBytes() {
                    return new char[]{(char) ((start_pc >> 8) & 0xFF),
                            (char) (start_pc & 0xFF),
                            (char) ((end_pc >> 8) & 0xFF),
                            (char) (end_pc & 0xFF),
                            (char) ((handler_pc >> 8) & 0xFF),
                            (char) (handler_pc & 0xFF),
                            (char) ((catch_type >> 8) & 0xFF),
                            (char) (catch_type & 0xFF)};
                }
            }
        }

        /**
         * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.4
         */
        public static final class StackMapTableInfo extends AttributeInfo {

            private final int number_of_entries; //Unsigned short
            private final StackMapFrame[] entries;

            public StackMapTableInfo(int attribute_name_index, long attribute_length, int number_of_entries, StackMapFrame[] entries) {
                super(attribute_name_index, attribute_length);
                this.number_of_entries = number_of_entries;
                this.entries = entries;
            }

            @Override
            public char[][] getInfo() {
                char[][] info = allocate(getInfoByteLength());
                long offset = insert(new char[]{(char) (number_of_entries >> 8),
                        (char) (number_of_entries & 0xFF)}, info, 0);
                for (StackMapFrame entry : entries)
                    offset += insert(entry.toBytes(), info, offset);
                return info;
            }

            @Override
            public long getInfoByteLength() {
                long total = 2L;
                for (StackMapFrame entry : entries)
                    total += entry.getInfoByteLength();
                return total;
            }

            public int getNumberOfEntries() {
                return number_of_entries;
            }

            public StackMapFrame[] getEntries() {
                return entries;
            }

            public static abstract class StackMapFrame {

                private final char frame_type;

                public StackMapFrame(char frame_type) {
                    this.frame_type = frame_type;
                }

                public abstract char[] toBytes();

                public abstract int getInfoByteLength();

                public char getFrameType() {
                    return frame_type;
                }

                public static final class SameFrame extends StackMapFrame {

                    public SameFrame(char frame_type) { //Type: 0-63
                        super(frame_type);
                    }

                    @Override
                    public char[] toBytes() {
                        return new char[]{getFrameType()};
                    }

                    @Override
                    public int getInfoByteLength() {
                        return 1;
                    }
                }

                public static final class SameLocals1StackItemFrame extends StackMapFrame {

                    private final VerificationTypeInfo stack;

                    public SameLocals1StackItemFrame(char frame_type, VerificationTypeInfo stack) { //Type: 64-127
                        super(frame_type);
                        this.stack = stack;
                    }

                    @Override
                    public char[] toBytes() {
                        char[] verification = stack.toBytes();
                        char[] bytes = new char[verification.length + 1];
                        bytes[0] = getFrameType();
                        System.arraycopy(verification, 0, bytes, 1, verification.length);
                        return bytes;
                    }

                    @Override
                    public int getInfoByteLength() {
                        return 1 + stack.getByteLength();
                    }

                    public VerificationTypeInfo getStack() {
                        return stack;
                    }
                }

                public static final class SameLocals1StackItemFrameExtended extends StackMapFrame {

                    private final int offset_delta; //Unsigned short
                    private final VerificationTypeInfo stack;

                    public SameLocals1StackItemFrameExtended(char frame_type, int offset_delta, VerificationTypeInfo stack) { //Type: 247
                        super(frame_type);
                        this.offset_delta = offset_delta;
                        this.stack = stack;
                    }

                    @Override
                    public char[] toBytes() {
                        char[] verification = stack.toBytes();
                        char[] bytes = new char[verification.length + 3];
                        bytes[0] = getFrameType();
                        bytes[1] = (char) (offset_delta >> 8);
                        bytes[2] = (char) (offset_delta & 0xFF);
                        System.arraycopy(verification, 0, bytes, 3, verification.length);
                        return bytes;
                    }

                    @Override
                    public int getInfoByteLength() {
                        return 3 + stack.getByteLength();
                    }

                    public int getOffsetDelta() {
                        return offset_delta;
                    }

                    public VerificationTypeInfo getStack() {
                        return stack;
                    }
                }

                public static final class ChopFrame extends StackMapFrame {

                    private final int offset_delta; //Unsigned short

                    public ChopFrame(char frame_type, int offset_delta) { //Types: 248-250
                        super(frame_type);
                        this.offset_delta = offset_delta;
                    }

                    @Override
                    public char[] toBytes() {
                        return new char[]{getFrameType(),
                                (char) (offset_delta >> 8),
                                (char) (offset_delta & 0xFF)};
                    }

                    @Override
                    public int getInfoByteLength() {
                        return 3;
                    }

                    public int getOffsetDelta() {
                        return offset_delta;
                    }
                }

                public static final class SameFrameExtended extends StackMapFrame {

                    private final int offset_delta; //Unsigned short

                    public SameFrameExtended(char frame_type, int offset_delta) { //Types: 251
                        super(frame_type);
                        this.offset_delta = offset_delta;
                    }

                    @Override
                    public char[] toBytes() {
                        return new char[]{getFrameType(),
                                (char) (offset_delta >> 8),
                                (char) (offset_delta & 0xFF)};
                    }

                    @Override
                    public int getInfoByteLength() {
                        return 3;
                    }

                    public int getOffsetDelta() {
                        return offset_delta;
                    }
                }

                public static final class AppendFrame extends StackMapFrame {

                    private final int offset_delta; //Unsigned short
                    private final VerificationTypeInfo[] locals;

                    public AppendFrame(char frame_type, int offset_delta, VerificationTypeInfo[] locals) { //Types: 252-254
                        super(frame_type);
                        this.offset_delta = offset_delta;
                        this.locals = locals;
                    }

                    @Override
                    public char[] toBytes() { //Max of 3 locals
                        char[] loc1 = locals[0].toBytes();
                        char[] loc2 = locals.length > 1 ? locals[1].toBytes() : new char[0];
                        char[] loc3 = locals.length > 2 ? locals[2].toBytes() : new char[0];
                        char[] buf = new char[3 + loc1.length + loc2.length + loc3.length];
                        buf[0] = getFrameType();
                        buf[1] = (char) (offset_delta >> 8);
                        buf[2] = (char) (offset_delta & 0xFF);
                        System.arraycopy(loc1, 0, buf, 3, loc1.length);
                        System.arraycopy(loc2, 0, buf, 3 + loc1.length, loc2.length);
                        System.arraycopy(loc3, 0, buf, 3 + loc1.length + loc2.length, loc3.length);
                        return buf;
                    }

                    @Override
                    public int getInfoByteLength() {
                        int total = 3;
                        for (VerificationTypeInfo info : locals)
                            total += info.getByteLength();
                        return total;
                    }

                    public int getOffsetDelta() {
                        return offset_delta;
                    }

                    public VerificationTypeInfo[] getLocals() {
                        return locals;
                    }
                }

                public static final class FullFrame extends StackMapFrame {

                    private final int offset_delta; //Unsigned short
                    private final int number_of_locals; //Unsigned short
                    private final VerificationTypeInfo[] locals;
                    private final int number_of_stack_items; //Unsigned short
                    private final VerificationTypeInfo[] stack;

                    public FullFrame(char frame_type, int offset_delta, int number_of_locals, VerificationTypeInfo[] locals, int number_of_stack_items, VerificationTypeInfo[] stack) { //Type: 255
                        super(frame_type);
                        this.offset_delta = offset_delta;
                        this.number_of_locals = number_of_locals;
                        this.locals = locals;
                        this.number_of_stack_items = number_of_stack_items;
                        this.stack = stack;
                    }

                    @Override
                    public char[] toBytes() {
                        char[] buf = new char[getInfoByteLength()];
                        buf[0] = getFrameType();
                        buf[1] = (char) (offset_delta >> 8);
                        buf[2] = (char) (offset_delta & 0xFF);
                        buf[3] = (char) (number_of_locals >> 8);
                        buf[4] = (char) (number_of_locals & 0xFF);
                        int buf_offset = 5;
                        for (int i = 0; i < number_of_locals; i++) {
                            char[] verificationBytes = locals[i].toBytes();
                            System.arraycopy(verificationBytes, 0, buf, buf_offset, verificationBytes.length);
                            buf_offset += verificationBytes.length;
                        }
                        buf[buf_offset++] = (char) (number_of_stack_items >> 8);
                        buf[buf_offset++] = (char) (number_of_stack_items & 0xFF);
                        for (int i = 0; i < number_of_stack_items; i++) {
                            char[] verificationBytes = stack[i].toBytes();
                            System.arraycopy(verificationBytes, 0, buf, buf_offset, verificationBytes.length);
                            buf_offset += verificationBytes.length;
                        }
                        return buf;
                    }

                    @Override
                    public int getInfoByteLength() {
                        int total = 7;
                        for (VerificationTypeInfo local : locals)
                            total += local.getByteLength();
                        for (VerificationTypeInfo info : stack)
                            total += info.getByteLength();
                        return total;
                    }

                    public int getOffsetDelta() {
                        return offset_delta;
                    }

                    public int getNumberOfLocals() {
                        return number_of_locals;
                    }

                    public VerificationTypeInfo[] getLocals() {
                        return locals;
                    }

                    public int getNumberOfStackItems() {
                        return number_of_stack_items;
                    }

                    public VerificationTypeInfo[] getStack() {
                        return stack;
                    }
                }

                public static abstract class VerificationTypeInfo {

                    private final char tag;

                    public VerificationTypeInfo(char tag) {
                        this.tag = tag;
                    }

                    public abstract char[] toBytes();

                    public abstract int getByteLength();

                    public char getTag() {
                        return tag;
                    }
                }

                public static final class TopVariableInfo extends VerificationTypeInfo {

                    public TopVariableInfo(char tag) { //Tag: 0
                        super(tag);
                    }

                    @Override
                    public char[] toBytes() {
                        return new char[]{getTag()};
                    }

                    @Override
                    public int getByteLength() {
                        return 1;
                    }
                }

                public static final class IntegerVariableInfo extends VerificationTypeInfo {

                    public IntegerVariableInfo(char tag) { //Tag: 1
                        super(tag);
                    }

                    @Override
                    public char[] toBytes() {
                        return new char[]{getTag()};
                    }

                    @Override
                    public int getByteLength() {
                        return 1;
                    }
                }

                public static final class FloatVariableInfo extends VerificationTypeInfo {

                    public FloatVariableInfo(char tag) { //Tag: 2
                        super(tag);
                    }

                    @Override
                    public char[] toBytes() {
                        return new char[]{getTag()};
                    }

                    @Override
                    public int getByteLength() {
                        return 1;
                    }
                }

                public static final class NullVariableInfo extends VerificationTypeInfo {

                    public NullVariableInfo(char tag) { //Tag: 5
                        super(tag);
                    }

                    @Override
                    public char[] toBytes() {
                        return new char[]{getTag()};
                    }

                    @Override
                    public int getByteLength() {
                        return 1;
                    }
                }

                public static final class UninitializedThisVariableInfo extends VerificationTypeInfo {

                    public UninitializedThisVariableInfo(char tag) { //Tag: 6
                        super(tag);
                    }

                    @Override
                    public char[] toBytes() {
                        return new char[]{getTag()};
                    }

                    @Override
                    public int getByteLength() {
                        return 1;
                    }
                }

                public static final class ObjectVariableInfo extends VerificationTypeInfo {

                    private final int cpool_index; //Unsigned short

                    public ObjectVariableInfo(char tag, int cpool_index) { //Tag: 7
                        super(tag);
                        this.cpool_index = cpool_index;
                    }

                    @Override
                    public char[] toBytes() {
                        return new char[]{getTag(),
                                (char) (cpool_index >> 8),
                                (char) (cpool_index & 0xFF)};
                    }

                    @Override
                    public int getByteLength() {
                        return 3;
                    }

                    public int getCPoolIndex() {
                        return cpool_index;
                    }
                }

                public static final class UninitializedVariableInfo extends VerificationTypeInfo {

                    private final int offset; //Unsigned short

                    public UninitializedVariableInfo(char tag, int offset) { //Tag: 8
                        super(tag);
                        this.offset = offset;
                    }

                    @Override
                    public char[] toBytes() {
                        return new char[]{getTag(),
                                (char) (offset >> 8),
                                (char) (offset & 0xFF)};
                    }

                    @Override
                    public int getByteLength() {
                        return 3;
                    }

                    public int getOffset() {
                        return offset;
                    }
                }

                public static final class LongVariableInfo extends VerificationTypeInfo {

                    public LongVariableInfo(char tag) { //Tag: 4
                        super(tag);
                    }

                    @Override
                    public char[] toBytes() {
                        return new char[]{getTag()};
                    }

                    @Override
                    public int getByteLength() {
                        return 1;
                    }
                }

                public static final class DoubleVariableInfo extends VerificationTypeInfo {

                    public DoubleVariableInfo(char tag) { //Tag: 4
                        super(tag);
                    }

                    @Override
                    public char[] toBytes() {
                        return new char[]{getTag()};
                    }

                    @Override
                    public int getByteLength() {
                        return 1;
                    }
                }
            }
        }

        /**
         * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.5
         */
        public static final class ExceptionsInfo extends AttributeInfo {

            private final int number_of_exceptions; //Unsigned short
            private final int[] exception_index_table; //Unsigned shorts

            public ExceptionsInfo(int attribute_name_index, long attribute_length, int number_of_exceptions, int[] exception_index_table) {
                super(attribute_name_index, attribute_length);
                this.number_of_exceptions = number_of_exceptions;
                this.exception_index_table = exception_index_table;
            }

            @Override
            public char[][] getInfo() {
                char[][] buf = allocate(getInfoByteLength());
                long offset = insert(new char[]{(char) (number_of_exceptions >> 8),
                        (char) (number_of_exceptions & 0xFF)}, buf, 0L);
                for (int index : exception_index_table) {
                    offset += insert(new char[]{(char) (index >> 8),
                            (char) (index & 0xFF)}, buf, offset);
                }
                return buf;
            }

            @Override
            public long getInfoByteLength() {
                return 2L + (2L * (long) number_of_exceptions);
            }

            public int getNumberOfExceptions() {
                return number_of_exceptions;
            }

            public int[] getExceptionIndexTable() {
                return exception_index_table;
            }
        }

        /**
         * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.6
         */
        public static final class InnerClassesInfo extends AttributeInfo {

            private final int number_of_classes; //Unsigned short
            private final InnerClass[] classes;

            public InnerClassesInfo(int attribute_name_index, long attribute_length, int number_of_classes, InnerClass[] classes) {
                super(attribute_name_index, attribute_length);
                this.number_of_classes = number_of_classes;
                this.classes = classes;
            }

            @Override
            public char[][] getInfo() {
                char[][] buf = allocate(getInfoByteLength());
                long offset = insert(new char[] {(char) (number_of_classes >> 8),
                        (char) (number_of_classes & 0xFF)}, buf, 0);
                for (InnerClass clazz : classes) {
                    offset += insert(clazz.getBytes(), buf, offset);
                }
                return buf;
            }

            @Override
            public long getInfoByteLength() {
                return 2L + (8L * (long) number_of_classes);
            }

            public int getNumberOfClasses() {
                return number_of_classes;
            }

            public InnerClass[] getClasses() {
                return classes;
            }

            public static final class InnerClass {

                //Access flags https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.6-300-D.1-D.1
                public static final int ACC_PUBLIC = 0x0001;
                public static final int ACC_PRIVATE = 0x0002;
                public static final int ACC_PROTECTED = 0x0004;
                public static final int ACC_STATIC = 0x0008;
                public static final int ACC_FINAL = 0x0010;
                public static final int ACC_INTERFACE = 0x0200;
                public static final int ACC_ABSTRACT = 0x0400;
                public static final int ACC_SYNTHETIC = 0x1000;
                public static final int ACC_ANNOTATION = 0x2000;
                public static final int ACC_ENUM = 0x4000;

                private final int inner_class_info_index; //Unsigned short
                private final int outer_class_info_index; //Unsigned short
                private final int inner_name_index; //Unsigned short
                private final int inner_class_access_flags; //Unsigned short

                public InnerClass(int inner_class_info_index, int outer_class_info_index, int inner_name_index, int inner_class_access_flags) {
                    this.inner_class_info_index = inner_class_info_index;
                    this.outer_class_info_index = outer_class_info_index;
                    this.inner_name_index = inner_name_index;
                    this.inner_class_access_flags = inner_class_access_flags;
                }

                public char[] getBytes() {
                    return new char[]{(char) (inner_class_info_index >> 8),
                            (char) (inner_class_info_index & 0xFF),
                            (char) (outer_class_info_index >> 8),
                            (char) (outer_class_info_index & 0xFF),
                            (char) (inner_name_index >> 8),
                            (char) (inner_name_index & 0xFF),
                            (char) (inner_class_access_flags >> 8),
                            (char) (inner_class_access_flags & 0xFF)};
                }

                public int getByteLength() {
                    return 8;
                }

                public int getInnerClassInfoIndex() {
                    return inner_class_info_index;
                }

                public int getOuterClassInfoIndex() {
                    return outer_class_info_index;
                }

                public int getInnerNameIndex() {
                    return inner_name_index;
                }

                public int getInnerClassAccessFlags() {
                    return inner_class_access_flags;
                }
            }
        }

        /**
         * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.7
         */
        public static final class EnclosingMethodInfo extends AttributeInfo {

            private final int class_index; //Unsigned short
            private final int method_index; //Unsigned short

            public EnclosingMethodInfo(int attribute_name_index, long attribute_length, int class_index, int method_index) {
                super(attribute_name_index, attribute_length);
                this.class_index = class_index;
                this.method_index = method_index;
            }

            @Override
            public char[][] getInfo() {
                return new char[][]{{(char) (class_index >> 8),
                        (char) (class_index & 0xFF),
                        (char) (method_index >> 8),
                        (char) (method_index & 0xFF)}};
            }

            @Override
            public long getInfoByteLength() {
                return 4L;
            }

            public int getClassIndex() {
                return class_index;
            }

            public int getMethodIndex() {
                return method_index;
            }
        }

        /**
         * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.8
         */
        public static final class SyntheticInfo extends AttributeInfo {

            public SyntheticInfo(int attribute_name_index, long attribute_length) {
                super(attribute_name_index, attribute_length);
            }

            @Override
            public char[][] getInfo() {
                return new char[0][];
            }

            @Override
            public long getInfoByteLength() {
                return 0;
            }
        }

        /**
         * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.9
         */
        public static final class SignatureInfo extends AttributeInfo {

            private final int signature_index; //Unsigned short

            public SignatureInfo(int attribute_name_index, long attribute_length, int signature_index) {
                super(attribute_name_index, attribute_length);
                this.signature_index = signature_index;
            }

            @Override
            public char[][] getInfo() {
                return new char[][]{{(char) (signature_index >> 8),
                        (char) (signature_index & 0xFF)}};
            }

            @Override
            public long getInfoByteLength() {
                return 2L;
            }

            public int getSignatureIndex() {
                return signature_index;
            }
        }

        /**
         * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.10
         */
        public static final class SourceFileInfo extends AttributeInfo {

            private final int sourcefile_index; //Unsigned short

            public SourceFileInfo(int attribute_name_index, long attribute_length, int sourcefile_index) {
                super(attribute_name_index, attribute_length);
                this.sourcefile_index = sourcefile_index;
            }

            @Override
            public char[][] getInfo() {
                return new char[][]{{(char) (sourcefile_index >> 8),
                        (char) (sourcefile_index & 0xFF)}};
            }

            @Override
            public long getInfoByteLength() {
                return 2L;
            }

            public int getSignatureIndex() {
                return sourcefile_index;
            }
        }

        /**
         * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.11
         */
        public static final class SourceDebugExtensionInfo extends AttributeInfo {

            private final char[][] debug_extension; //Byte array with overflow support

            public SourceDebugExtensionInfo(int attribute_name_index, long attribute_length, char[][] debug_extension) {
                super(attribute_name_index, attribute_length);
                this.debug_extension = debug_extension;
            }

            @Override
            public char[][] getInfo() {
                return debug_extension;
            }

            @Override
            public long getInfoByteLength() {
                return getAttributeLength();
            }

            public char[][] getDebugExtension() {
                return debug_extension;
            }
        }

        /**
         * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.12
         */
        public static final class LineNumberTableInfo extends AttributeInfo {

            private final int line_number_table_length; //Unsigned short
            private final LineNumber[] line_number_table;

            public LineNumberTableInfo(int attribute_name_index, long attribute_length, int line_number_table_length, LineNumber[] line_number_table) {
                super(attribute_name_index, attribute_length);
                this.line_number_table_length = line_number_table_length;
                this.line_number_table = line_number_table;
            }

            @Override
            public char[][] getInfo() {
                char[][] buf = allocate(getInfoByteLength());
                long offset = insert(new char[]{(char) (line_number_table_length >> 8),
                        (char) (line_number_table_length & 0xFF)}, buf, 0);
                for (LineNumber lineNumber : line_number_table) {
                    offset += insert(lineNumber.getBytes(), buf, offset);
                }
                return buf;
            }

            @Override
            public long getInfoByteLength() {
                return 2L + (4L * (long) line_number_table_length);
            }

            public int getLineNumberTableLength() {
                return line_number_table_length;
            }

            public LineNumber[] getLineNumberTable() {
                return line_number_table;
            }

            public static final class LineNumber {

                private final int start_pc; //Unsigned short
                private final int line_number; //Unsigned short

                public LineNumber(int start_pc, int line_number) {
                    this.start_pc = start_pc;
                    this.line_number = line_number;
                }

                public char[] getBytes() {
                    return new char[] {(char) (start_pc >> 8),
                            (char) (start_pc & 0xFF),
                            (char) (line_number >> 8),
                            (char) (line_number & 0xFF)};
                }

                public int getByteLength() {
                    return 4;
                }

                public int getStartPc() {
                    return start_pc;
                }

                public int getLineNumber() {
                    return line_number;
                }
            }
        }

        /**
         * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.13
         */
        public static final class LocalVariableTableInfo extends AttributeInfo {

            private final int local_variable_table_length; //Unsigned short
            private final LocalVariable[] local_variable_table;

            public LocalVariableTableInfo(int attribute_name_index, long attribute_length, int local_variable_table_length, LocalVariable[] local_variable_table) {
                super(attribute_name_index, attribute_length);
                this.local_variable_table_length = local_variable_table_length;
                this.local_variable_table = local_variable_table;
            }

            @Override
            public char[][] getInfo() {
                char[][] buf = allocate(getInfoByteLength());
                long offset = insert(new char[]{(char) (local_variable_table_length >> 8),
                        (char) (local_variable_table_length & 0xFF)}, buf, 0);
                for (LocalVariable variable : local_variable_table) {
                    offset += insert(variable.getBytes(), buf, offset);
                }
                return buf;
            }

            @Override
            public long getInfoByteLength() {
                return 2L + (10L * (long) local_variable_table_length);
            }

            public int getLocalVariableTableLength() {
                return local_variable_table_length;
            }

            public LocalVariable[] getLocalVariableTable() {
                return local_variable_table;
            }

            public static final class LocalVariable {

                private final int start_pc; //Unsigned short
                private final int length; //Unsigned short
                private final int name_index; //Unsigned short
                private final int descriptor_index; //Unsigned short
                private final int index; //Unsigned short

                public LocalVariable(int start_pc, int length, int name_index, int descriptor_index, int index) {
                    this.start_pc = start_pc;
                    this.length = length;
                    this.name_index = name_index;
                    this.descriptor_index = descriptor_index;
                    this.index = index;
                }

                public char[] getBytes() {
                    return new char[]{(char) (start_pc >> 8),
                            (char) (start_pc & 0xFF),
                            (char) (length >> 8),
                            (char) (length & 0xFF),
                            (char) (name_index >> 8),
                            (char) (name_index & 0xFF),
                            (char) (descriptor_index >> 8),
                            (char) (descriptor_index & 0xFF),
                            (char) (index >> 8),
                            (char) (index & 0xFF)};
                }

                public int getByteLength() {
                    return 10;
                }

                public int getStartPc() {
                    return start_pc;
                }

                public int getLength() {
                    return length;
                }

                public int getNameIndex() {
                    return name_index;
                }

                public int getDescriptorIndex() {
                    return descriptor_index;
                }

                public int getIndex() {
                    return index;
                }
            }
        }

        /**
         * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.14
         */
        public static final class LocalVariableTypeTableInfo extends AttributeInfo {

            private final int local_variable_type_table_length; //Unsigned short
            private final LocalVariableType[] local_variable_type_table;

            public LocalVariableTypeTableInfo(int attribute_name_index, long attribute_length, int local_variable_type_table_length, LocalVariableType[] local_variable_type_table) {
                super(attribute_name_index, attribute_length);
                this.local_variable_type_table_length = local_variable_type_table_length;
                this.local_variable_type_table = local_variable_type_table;
            }

            @Override
            public char[][] getInfo() {
                char[][] buf = allocate(getInfoByteLength());
                long offset = insert(new char[]{(char) (local_variable_type_table_length >> 8),
                        (char) (local_variable_type_table_length & 0xFF)}, buf, 0);
                for (LocalVariableType type : local_variable_type_table) {
                    offset += insert(type.getBytes(), buf, offset);
                }
                return buf;
            }

            @Override
            public long getInfoByteLength() {
                return 2L + (10L * (long) local_variable_type_table_length);
            }

            public int getLocalVariableTypeTableLength() {
                return local_variable_type_table_length;
            }

            public LocalVariableType[] getLocalVariableTypeTable() {
                return local_variable_type_table;
            }

            public static final class LocalVariableType {

                private final int start_pc; //Unsigned short
                private final int length; //Unsigned short
                private final int name_index; //Unsigned short
                private final int signature_index; //Unsigned short
                private final int index; //Unsigned short

                public LocalVariableType(int start_pc, int length, int name_index, int signature_index, int index) {
                    this.start_pc = start_pc;
                    this.length = length;
                    this.name_index = name_index;
                    this.signature_index = signature_index;
                    this.index = index;
                }

                public char[] getBytes() {
                    return new char[]{(char) (start_pc >> 8),
                            (char) (start_pc & 0xFF),
                            (char) (length >> 8),
                            (char) (length & 0xFF),
                            (char) (name_index >> 8),
                            (char) (name_index & 0xFF),
                            (char) (signature_index >> 8),
                            (char) (signature_index & 0xFF),
                            (char) (index >> 8),
                            (char) (index & 0xFF)};
                }

                public int getByteLength() {
                    return 10;
                }

                public int getStartPc() {
                    return start_pc;
                }

                public int getLength() {
                    return length;
                }

                public int getNameIndex() {
                    return name_index;
                }

                public int getSignatureIndex() {
                    return signature_index;
                }

                public int getIndex() {
                    return index;
                }
            }
        }

        /**
         * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.15
         */
        public static final class DeprecatedInfo extends AttributeInfo {

            public DeprecatedInfo(int attribute_name_index, long attribute_length) {
                super(attribute_name_index, attribute_length);
            }

            @Override
            public char[][] getInfo() {
                return new char[0][];
            }

            @Override
            public long getInfoByteLength() {
                return 0;
            }
        }

        public static final class AnnotationInfo {

            public static AnnotationInfo readAnnotationValue(DataInputStream stream) throws IOException {
                int type = stream.readUnsignedShort();
                int pair_count = stream.readUnsignedShort();
                ElementValuePair[] pairs = new ElementValuePair[pair_count];
                for (int i = 0; i < pair_count; i++) {
                    pairs[i] = new ElementValuePair(stream.readUnsignedShort(), ElementValue.readElementValue(stream))
                }
                return new AnnotationInfo(type, pair_count, pairs);
            }

            private final int type_index; //Unsigned short
            private final int num_element_value_pairs; //Unsigned short
            private final ElementValuePair[] element_value_pairs;

            public AnnotationInfo(int type_index, int num_element_value_pairs, ElementValuePair[] element_value_pairs) {
                this.type_index = type_index;
                this.num_element_value_pairs = num_element_value_pairs;
                this.element_value_pairs = element_value_pairs;
            }

            public char[] getBytes() {
                char[] buf = new char[getByteLength()];
                buf[0] = (char) (type_index >> 8);
                buf[1] = (char) (type_index & 0xFF);
                buf[2] = (char) (num_element_value_pairs >> 8);
                buf[3] = (char) (num_element_value_pairs & 0xFF);
                int offset = 4;
                for (ElementValuePair pair : element_value_pairs) {
                    char[] bytes = pair.getBytes();
                    System.arraycopy(bytes, 0, buf, offset, bytes.length);
                    offset += bytes.length;
                }
                return buf;
            }

            public int getByteLength() {
                int len = 4;
                for (ElementValuePair pair : element_value_pairs)
                    len += pair.getByteLength();
                return len;
            }

            public int getTypeIndex() {
                return type_index;
            }

            public int getNumElementValuePairs() {
                return num_element_value_pairs;
            }

            public ElementValuePair[] getElementValuePairs() {
                return element_value_pairs;
            }

            public static final class ElementValuePair {

                private final int element_name_index; //Unsigned short
                private final ElementValue value;

                public ElementValuePair(int element_name_index, ElementValue value) {
                    this.element_name_index = element_name_index;
                    this.value = value;
                }

                public char[] getBytes() {
                    char[] buf = new char[getByteLength()];
                    buf[0] = (char) (element_name_index >> 8);
                    buf[1] = (char) (element_name_index & 0xFF);
                    System.arraycopy(value.getBytes(), 0, buf, 2, value.getByteLength());
                    return buf;
                }

                public int getByteLength() {
                    return 2 + value.getByteLength();
                }

                public int getElementNameIndex() {
                    return element_name_index;
                }

                public ElementValue getValue() {
                    return value;
                }
            }
        }

        public static abstract class ElementValue {

            // Tag definitions: https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16.1-130
            public static final char BYTE = 'B';
            public static final char CHAR = 'C';
            public static final char DOUBLE = 'D';
            public static final char FLOAT = 'F';
            public static final char INT = 'I';
            public static final char LONG = 'J';
            public static final char SHORT = 'S';
            public static final char BOOLEAN = 'Z';
            public static final char STRING = 's';
            public static final char ENUM_TYPE = 'e';
            public static final char CLASS = 'c';
            public static final char ANNOTATION_TYPE = '@';
            public static final char ARRAY_TYPE = '[';

            public static ElementValue readElementValue(DataInputStream stream) throws IOException {
                char tag = (char) stream.readByte();
                switch (tag) {
                    case BYTE:
                    case CHAR:
                    case DOUBLE:
                    case FLOAT:
                    case INT:
                    case LONG:
                    case SHORT:
                    case BOOLEAN:
                    case STRING:
                        return new PrimitiveElementValue(tag, stream.readUnsignedShort());
                    case ENUM_TYPE:
                        return new EnumValue(tag, stream.readUnsignedShort(), stream.readUnsignedShort());
                    case CLASS:
                        return new ClassValue(tag, stream.readUnsignedShort());
                    case ANNOTATION_TYPE:
                        return new AnnotationValue(tag, AnnotationInfo.readAnnotationValue(stream));
                    case ARRAY_TYPE:
                        int value_count = stream.readUnsignedShort();
                        ElementValue[] values = new ElementValue[value_count];
                        for (int i = 0; i < value_count; i++) {
                            values[i] = readElementValue(stream);
                        }
                        return new ArrayValue(tag, value_count, values);
                    default:
                        throw new IOException("Invalid element read!");
                }
            }

            private final char tag; //Unsigned single byte;

            public ElementValue(char tag) {
                this.tag = tag;
            }

            public abstract char[] getBytes();

            public abstract int getByteLength();

            public char getTag() {
                return tag;
            }

            public static final class PrimitiveElementValue extends ElementValue {

                private final int const_value_index; //Unsigned short;

                public PrimitiveElementValue(char tag, int const_value_index) { //Applies to tags B,C,D,F,I,J,S,Z, and s
                    super(tag);
                    this.const_value_index = const_value_index;
                }

                @Override
                public char[] getBytes() {
                    return new char[] {getTag(), (char) (const_value_index >> 8), (char) (const_value_index & 0xFF)};
                }

                @Override
                public int getByteLength() {
                    return 3;
                }

                public int getConstValueIndex() {
                    return const_value_index;
                }
            }

            public static final class EnumValue extends ElementValue {

                private final int type_name_index; //Unsigned short
                private final int const_name_index; //Unsigned short

                public EnumValue(char tag, int type_name_index, int const_name_index) { //Applies to tag e
                    super(tag);
                    this.type_name_index = type_name_index;
                    this.const_name_index = const_name_index;
                }

                @Override
                public char[] getBytes() {
                    return new char[] {getTag(),
                            (char) (type_name_index >> 8),
                            (char) (type_name_index & 0xFF),
                            (char) (const_name_index >> 8),
                            (char) (const_name_index & 0xFF)};
                }

                @Override
                public int getByteLength() {
                    return 5;
                }

                public int getTypeNameIndex() {
                    return type_name_index;
                }

                public int getConstNameIndex() {
                    return const_name_index;
                }
            }

            public static final class ClassValue extends ElementValue {

                private final int class_info_index; //Unsigned short;

                public ClassValue(char tag, int class_info_index) { //Applies to tag c
                    super(tag);
                    this.class_info_index = class_info_index;
                }

                @Override
                public char[] getBytes() {
                    return new char[] {getTag(), (char) (class_info_index >> 8), (char) (class_info_index & 0xFF)};
                }

                @Override
                public int getByteLength() {
                    return 3;
                }

                public int getClassInfoIndex() {
                    return class_info_index;
                }
            }

            public static final class AnnotationValue extends ElementValue {

                private final AnnotationInfo annotation_value;

                public AnnotationValue(char tag, AnnotationInfo annotation_value) { //Applies to tag @
                    super(tag);
                    this.annotation_value = annotation_value;
                }

                @Override
                public char[] getBytes() {
                    char[] buf = new char[getByteLength()];
                    buf[0] = getTag();
                    System.arraycopy(annotation_value.getBytes(), 0, buf, 1, annotation_value.getByteLength());
                    return buf;
                }

                @Override
                public int getByteLength() {
                    return 1 + annotation_value.getByteLength();
                }

                public AnnotationInfo getAnnotationValue() {
                    return annotation_value;
                }
            }

            public static final class ArrayValue extends ElementValue {

                private final int num_values; //Unsigned short
                private final ElementValue[] values;

                public ArrayValue(char tag, int num_values, ElementValue[] values) { //Applies to tag [
                    super(tag);
                    this.num_values = num_values;
                    this.values = values;
                }

                @Override
                public char[] getBytes() {
                    char[] buf = new char[getByteLength()];
                    buf[0] = getTag();
                    buf[1] = (char) (num_values >> 8);
                    buf[2] = (char) (num_values & 0xFF);
                    int offset = 3;
                    for (ElementValue val : values) {
                        char[] val_array = val.getBytes();
                        System.arraycopy(val_array, 0, buf, offset, val_array.length);
                        offset += val_array.length;
                    }
                    return buf;
                }

                @Override
                public int getByteLength() {
                    int len = 3;
                    for (ElementValue val : values)
                        len += val.getByteLength();
                    return len;
                }

                public int getNumValues() {
                    return num_values;
                }

                public ElementValue[] getValues() {
                    return values;
                }
            }
        }

        /**
         * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.16
         */
        public static final class RuntimeVisibleAnnotationsInfo extends AttributeInfo {

            private final int num_annotations; //Unsigned short
            private final AnnotationInfo[] annotations;

            public RuntimeVisibleAnnotationsInfo(int attribute_name_index, long attribute_length, int num_annotations, AnnotationInfo[] annotations) {
                super(attribute_name_index, attribute_length);
                this.num_annotations = num_annotations;
                this.annotations = annotations;
            }

            @Override
            public char[][] getInfo() {
                char[][] buf = allocate(getInfoByteLength());
                long offset = insert(new char[]{(char) (num_annotations >> 8),
                        (char) (num_annotations & 0xFF)}, buf, 0);

                for (AnnotationInfo annotation : annotations) {
                    offset += insert(annotation.getBytes(), buf, offset);
                }
                return buf;
            }

            @Override
            public long getInfoByteLength() {
                long len = 2L;
                for (AnnotationInfo annotation : annotations)
                    len += annotation.getByteLength();
                return len;
            }

            public int getNumAnnotations() {
                return num_annotations;
            }

            public AnnotationInfo[] getAnnotations() {
                return annotations;
            }
        }

        /**
         * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.17
         */
        public static final class RuntimeInvisibleAnnotationsInfo extends AttributeInfo {

            private final int num_annotations; //Unsigned short
            private final AnnotationInfo[] annotations;

            public RuntimeInvisibleAnnotationsInfo(int attribute_name_index, long attribute_length, int num_annotations, AnnotationInfo[] annotations) {
                super(attribute_name_index, attribute_length);
                this.num_annotations = num_annotations;
                this.annotations = annotations;
            }

            @Override
            public char[][] getInfo() {
                char[][] buf = allocate(getInfoByteLength());
                long offset = insert(new char[]{(char) (num_annotations >> 8),
                        (char) (num_annotations & 0xFF)}, buf, 0);

                for (AnnotationInfo annotation : annotations) {
                    offset += insert(annotation.getBytes(), buf, offset);
                }
                return buf;
            }

            @Override
            public long getInfoByteLength() {
                long len = 2L;
                for (AnnotationInfo annotation : annotations)
                    len += annotation.getByteLength();
                return len;
            }

            public int getNumAnnotations() {
                return num_annotations;
            }

            public AnnotationInfo[] getAnnotations() {
                return annotations;
            }
        }

        public static final class ParameterAnnotationInfo {

            private final int num_annotations; //Unsigned short
            private final AnnotationInfo[] annotations;

            public ParameterAnnotationInfo(int num_annotations, AnnotationInfo[] annotations) {
                this.num_annotations = num_annotations;
                this.annotations = annotations;
            }

            public char[] getBytes() {
                char[] buf = new char[getByteLength()];
                buf[0] = (char) (num_annotations >> 8);
                buf[1] = (char) (num_annotations & 0xFF);

                int offset = 2;

                for (AnnotationInfo annotation : annotations) {
                    char[] data = annotation.getBytes();
                    System.arraycopy(data, 0, buf, offset, data.length);
                    offset += data.length;
                }
                return buf;
            }

            public int getByteLength() {
                int len = 2;
                for (AnnotationInfo annotation : annotations)
                    len += annotation.getByteLength();
                return len;
            }

            public int getNumAnnotations() {
                return num_annotations;
            }

            public AnnotationInfo[] getAnnotations() {
                return annotations;
            }
        }

        /**
         * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.18
         */
        public static final class RuntimeVisibleParameterAnnotationsInfo extends AttributeInfo {

            private final char num_parameters;
            private final ParameterAnnotationInfo[] parameter_annotations;

            public RuntimeVisibleParameterAnnotationsInfo(int attribute_name_index, long attribute_length, char num_parameters, ParameterAnnotationInfo[] parameter_annotations) {
                super(attribute_name_index, attribute_length);
                this.num_parameters = num_parameters;
                this.parameter_annotations = parameter_annotations;
            }

            @Override
            public char[][] getInfo() {
                char[][] buf = allocate(getInfoByteLength());
                long offset = insert(new char[]{num_parameters}, buf, 0);
                for (ParameterAnnotationInfo annotation : parameter_annotations)
                    offset += insert(annotation.getBytes(), buf, offset);
                return buf;
            }

            @Override
            public long getInfoByteLength() {
                long len = 1L;
                for (ParameterAnnotationInfo annotation : parameter_annotations)
                    len += annotation.getByteLength();
                return len;
            }

            public char getNumParameters() {
                return num_parameters;
            }

            public ParameterAnnotationInfo[] getParameterAnnotations() {
                return parameter_annotations;
            }
        }

        /**
         * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.19
         */
        public static final class RuntimeInvisibleParameterAnnotationsInfo extends AttributeInfo {

            private final char num_parameters;
            private final ParameterAnnotationInfo[] parameter_annotations;

            public RuntimeInvisibleParameterAnnotationsInfo(int attribute_name_index, long attribute_length, char num_parameters, ParameterAnnotationInfo[] parameter_annotations) {
                super(attribute_name_index, attribute_length);
                this.num_parameters = num_parameters;
                this.parameter_annotations = parameter_annotations;
            }

            @Override
            public char[][] getInfo() {
                char[][] buf = allocate(getInfoByteLength());
                long offset = insert(new char[]{num_parameters}, buf, 0);
                for (ParameterAnnotationInfo annotation : parameter_annotations)
                    offset += insert(annotation.getBytes(), buf, offset);
                return buf;
            }

            @Override
            public long getInfoByteLength() {
                long len = 1L;
                for (ParameterAnnotationInfo annotation : parameter_annotations)
                    len += annotation.getByteLength();
                return len;
            }

            public char getNumParameters() {
                return num_parameters;
            }

            public ParameterAnnotationInfo[] getParameterAnnotations() {
                return parameter_annotations;
            }
        }
    }
}
