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
        FieldInfo[] fields = FieldInfo.readFieldInfo(fields_count, constant_pool, data);

        data.close();
        return new ClassFile(minor_version, major_version, constant_pool_count, constant_pool,
                access_flags, this_class, super_class, interfaces_count, interfaces, fields_count, fields);
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
    private final FieldInfo[] fields;

    public ClassFile(int minor_version, int major_version, int constant_pool_count, ConstantPoolInfo[] constant_pool, int access_flags, int this_class, int super_class, int interfaces_count, int[] interfaces, int fields_count, FieldInfo[] fields) {
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
        this.fields = fields;
    }

}
