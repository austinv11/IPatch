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

import injectr.ipatch.util.BytesUtil;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7
 */
public abstract class AttributeInfo {

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

    public static AttributeInfo[] readAttributeInfo(int attribute_info_count, ConstantPoolInfo[] constant_pool, DataInputStream stream) throws IOException {
        AttributeInfo[] info = new AttributeInfo[attribute_info_count];
        for (int i = 0; i < attribute_info_count; i++) {
            int name_index = stream.readUnsignedShort();
            if (!(constant_pool[name_index] instanceof ConstantPoolInfo.Utf8Info))
                throw new IOException("Expected utf8 constant!");
            long len = Integer.toUnsignedLong(stream.readInt());
            switch (((ConstantPoolInfo.Utf8Info) constant_pool[name_index]).getString()) {
                case ConstantValue:
                    info[i] = new ConstantValueInfo(name_index, len, stream.readUnsignedShort());
                    break;
                case Code:
                    int max_stack = stream.readUnsignedShort();
                    int max_local = stream.readUnsignedShort();
                    long code_len = Integer.toUnsignedLong(stream.readInt());
                    char[][] code = BytesUtil.allocate(code_len);
                    for (int j = 0; j < code.length; j++) {
                        byte[] dat = new byte[code[j].length];
                        stream.readFully(dat, 0, code[j].length);
                        code[j] = ByteBuffer.wrap(dat).asCharBuffer().array();
                    }
                    int exception_len = stream.readUnsignedShort();
                    CodeInfo.ExceptionTableInfo[] table = new CodeInfo.ExceptionTableInfo[exception_len];
                    for (int j = 0; j < exception_len; j++) {
                        table[j] = new CodeInfo.ExceptionTableInfo(stream.readUnsignedShort(), stream.readUnsignedShort(), stream.readUnsignedShort(), stream.readUnsignedShort());
                    }
                    int attr_count = stream.readUnsignedShort();
                    AttributeInfo[] attributes = readAttributeInfo(attr_count, constant_pool, stream);
                    info[i] = new CodeInfo(name_index, len, max_stack, max_local, code_len, code, exception_len, table, attr_count, attributes);
                    break;
                case StackMapTable:
                    int entry_count = stream.readUnsignedShort();
                    StackMapTableInfo.StackMapFrame[] entries = new StackMapTableInfo.StackMapFrame[entry_count];
                    for (int j = 0; j < entry_count; j++) {
                        char tag = (char) stream.readByte();
                        if (tag >= 0 && tag <= 63) {
                            entries[j] = new StackMapTableInfo.StackMapFrame.SameFrame(tag);
                        } else if (tag >= 64 && tag <= 127) {
                            entries[j] = new StackMapTableInfo.StackMapFrame.SameLocals1StackItemFrame(tag, StackMapTableInfo.StackMapFrame.VerificationTypeInfo.readVerificationTypeInfo(stream));
                        } else if (tag == 247) {
                            entries[j] = new StackMapTableInfo.StackMapFrame.SameLocals1StackItemFrameExtended(tag, stream.readUnsignedShort(), StackMapTableInfo.StackMapFrame.VerificationTypeInfo.readVerificationTypeInfo(stream));
                        } else if (tag >= 248 && tag <= 250) {
                            entries[j] = new StackMapTableInfo.StackMapFrame.ChopFrame(tag, stream.readUnsignedShort());
                        } else if (tag == 251) {
                            entries[j] = new StackMapTableInfo.StackMapFrame.SameFrameExtended(tag, stream.readUnsignedShort());
                        } else if (tag >= 252 && tag <= 254) {
                            int offset = stream.readUnsignedShort();
                            StackMapTableInfo.StackMapFrame.VerificationTypeInfo[] verifs = new StackMapTableInfo.StackMapFrame.VerificationTypeInfo[tag - 251];
                            for (int k = 0; k < verifs.length; k++) {
                                verifs[k] = StackMapTableInfo.StackMapFrame.VerificationTypeInfo.readVerificationTypeInfo(stream);
                            }
                            entries[j] = new StackMapTableInfo.StackMapFrame.AppendFrame(tag, offset, verifs);
                        } else if (tag == 255) {
                            int offset = stream.readUnsignedShort();
                            int num_locals = stream.readUnsignedShort();
                            StackMapTableInfo.StackMapFrame.VerificationTypeInfo[] locals = new StackMapTableInfo.StackMapFrame.VerificationTypeInfo[num_locals];
                            for (int k = 0; k < locals.length; k++) {
                                locals[k] = StackMapTableInfo.StackMapFrame.VerificationTypeInfo.readVerificationTypeInfo(stream);
                            }
                            int num_stack = stream.readUnsignedShort();
                            StackMapTableInfo.StackMapFrame.VerificationTypeInfo[] stack = new StackMapTableInfo.StackMapFrame.VerificationTypeInfo[num_stack];
                            for (int k = 0; k < stack.length; k++) {
                                stack[k] = StackMapTableInfo.StackMapFrame.VerificationTypeInfo.readVerificationTypeInfo(stream);
                            }
                            entries[j] = new StackMapTableInfo.StackMapFrame.FullFrame(tag, offset, num_locals, locals, num_stack, stack);
                        }
                    }

                    info[i] = new StackMapTableInfo(name_index, len, entry_count, entries);
                    break;
                case Exceptions:
                    int num_exceptions = stream.readUnsignedShort();
                    int[] indices = new int[num_exceptions];
                    for (int j = 0; j < num_exceptions; j++) {
                        indices[j] = stream.readUnsignedShort();
                    }
                    info[i] = new ExceptionsInfo(name_index, len, num_exceptions, indices);
                    break;
                case InnerClasses:
                    int num_classes = stream.readUnsignedShort();
                    InnerClassesInfo.InnerClass[] classes = new InnerClassesInfo.InnerClass[num_classes];
                    for (int j = 0; j < num_classes; j++) {
                        classes[j] = new InnerClassesInfo.InnerClass(stream.readUnsignedShort(), stream.readUnsignedShort(), stream.readUnsignedShort(), stream.readUnsignedShort());
                    }
                    info[i] = new InnerClassesInfo(name_index, len, num_classes, classes);
                    break;
                case EnclosingMethod:
                    info[i] = new EnclosingMethodInfo(name_index, len, stream.readUnsignedShort(), stream.readUnsignedShort());
                    break;
                case Synthetic:
                    info[i] = new SyntheticInfo(name_index, len);
                    break;
                case Signature:
                    info[i] = new SignatureInfo(name_index, len, stream.readUnsignedShort());
                    break;
                case SourceFile:
                    info[i] = new SourceFileInfo(name_index, len, stream.readUnsignedShort());
                    break;
                case SourceDebugExtension:
                    char[][] data = BytesUtil.allocate(len);
                    for (int j = 0; j < data.length; j++) {
                        byte[] dat = new byte[data[j].length];
                        stream.readFully(dat, 0, data[j].length);
                        data[j] = ByteBuffer.wrap(dat).asCharBuffer().array();
                    }
                    info[i] = new SourceDebugExtensionInfo(name_index, len, data);
                    break;
                case LineNumberTable:
                    int table_len = stream.readUnsignedShort();
                    LineNumberTableInfo.LineNumber[] lines = new LineNumberTableInfo.LineNumber[table_len];
                    for (int j = 0; j < table_len; j++) {
                        lines[j] = new LineNumberTableInfo.LineNumber(stream.readUnsignedShort(), stream.readUnsignedShort());
                    }
                    info[i] = new LineNumberTableInfo(name_index, len, table_len, lines);
                    break;
                case LocalVariableTable:
                    table_len = stream.readUnsignedShort();
                    LocalVariableTableInfo.LocalVariable[] vars = new LocalVariableTableInfo.LocalVariable[table_len];
                    for (int j = 0; j < table_len; j++) {
                        vars[j] = new LocalVariableTableInfo.LocalVariable(stream.readUnsignedShort(), stream.readUnsignedShort(), stream.readUnsignedShort(), stream.readUnsignedShort(), stream.readUnsignedShort());
                    }
                    info[i] = new LocalVariableTableInfo(name_index, len, table_len, vars);
                    break;
                case LocalVariableTypeTable:
                    table_len = stream.readUnsignedShort();
                    LocalVariableTypeTableInfo.LocalVariableType[] typeVars = new LocalVariableTypeTableInfo.LocalVariableType[table_len];
                    for (int j = 0; j < table_len; j++) {
                        typeVars[j] = new LocalVariableTypeTableInfo.LocalVariableType(stream.readUnsignedShort(), stream.readUnsignedShort(), stream.readUnsignedShort(), stream.readUnsignedShort(), stream.readUnsignedShort());
                    }
                    info[i] = new LocalVariableTypeTableInfo(name_index, len, table_len, typeVars);
                    break;
                case Deprecated:
                    info[i] = new DeprecatedInfo(name_index, len);
                    break;
                case RuntimeVisibleAnnotations:
                    int num_annotations = stream.readUnsignedShort();
                    AnnotationInfo[] annotations = new AnnotationInfo[num_annotations];
                    for (int j = 0; j < num_annotations; j++) {
                        annotations[j] = AnnotationInfo.readAnnotationValue(stream);
                    }
                    info[i] = new RuntimeVisibleAnnotationsInfo(name_index, len, num_annotations, annotations);
                    break;
                case RuntimeInvisibleAnnotations:
                    num_annotations = stream.readUnsignedShort();
                    annotations = new AnnotationInfo[num_annotations];
                    for (int j = 0; j < num_annotations; j++) {
                        annotations[j] = AnnotationInfo.readAnnotationValue(stream);
                    }
                    info[i] = new RuntimeInvisibleAnnotationsInfo(name_index, len, num_annotations, annotations);
                    break;
                case RuntimeVisibleParameterAnnotations:
                    char num_pannotations = (char) stream.readByte();
                    ParameterAnnotationInfo[] pannotations = new ParameterAnnotationInfo[num_pannotations];
                    for (char j = 0; j < num_pannotations; j++) {
                        num_annotations = stream.readUnsignedShort();
                        annotations = new AnnotationInfo[num_annotations];
                        for (int k = 0; k < num_annotations; k++) {
                            annotations[k] = AnnotationInfo.readAnnotationValue(stream);
                        }
                        pannotations[j] = new ParameterAnnotationInfo(num_annotations, annotations);
                    }
                    info[i] = new RuntimeVisibleParameterAnnotationsInfo(name_index, len, num_pannotations, pannotations);
                    break;
                case RuntimeInvisibleParameterAnnotations:
                    num_pannotations = (char) stream.readByte();
                    pannotations = new ParameterAnnotationInfo[num_pannotations];
                    for (char j = 0; j < num_pannotations; j++) {
                        num_annotations = stream.readUnsignedShort();
                        annotations = new AnnotationInfo[num_annotations];
                        for (int k = 0; k < num_annotations; k++) {
                            annotations[k] = AnnotationInfo.readAnnotationValue(stream);
                        }
                        pannotations[j] = new ParameterAnnotationInfo(num_annotations, annotations);
                    }
                    info[i] = new RuntimeInvisibleParameterAnnotationsInfo(name_index, len, num_pannotations, pannotations);
                    break;
                case RuntimeVisibleTypeAnnotations:
                    char num_tannotations = (char) stream.readByte();
                    TypeAnnotationInfo[] tannotations = new TypeAnnotationInfo[num_tannotations];
                    for (char j = 0; j < num_tannotations; j++) {
                        tannotations[j] = TypeAnnotationInfo.readTypeAnnotationInfo(stream);
                    }
                    info[i] = new RuntimeVisibleTypeAnnotationsInfo(name_index, len, num_tannotations, tannotations);
                    break;
                case RuntimeInvisibleTypeAnnotations:
                    num_tannotations = (char) stream.readByte();
                    tannotations = new TypeAnnotationInfo[num_tannotations];
                    for (char j = 0; j < num_tannotations; j++) {
                        tannotations[j] = TypeAnnotationInfo.readTypeAnnotationInfo(stream);
                    }
                    info[i] = new RuntimeInvisibleTypeAnnotationsInfo(name_index, len, num_tannotations, tannotations);
                    break;
                case AnnotationDefault:
                    info[i] = new AnnotationDefaultInfo(name_index, len, ElementValue.readElementValue(stream));
                    break;
                case BootstrapMethods:
                    int bootstrap_count = stream.readUnsignedShort();
                    BootstrapMethodsInfo.BootstrapMethod[] bmethods = new BootstrapMethodsInfo.BootstrapMethod[bootstrap_count];
                    for (int j = 0; j < bootstrap_count; j++) {
                        int method_ref = stream.readUnsignedShort();
                        int arg_count = stream.readUnsignedShort();
                        int[] args = new int[arg_count];
                        for (int k = 0; k < arg_count; k++) {
                            args[k] = stream.readUnsignedShort();
                        }
                        bmethods[j] = new BootstrapMethodsInfo.BootstrapMethod(method_ref, arg_count, args);
                    }
                    info[i] = new BootstrapMethodsInfo(name_index, len, bootstrap_count, bmethods);
                    break;
                case MethodParameters:
                    char param_count = (char) stream.readByte();
                    MethodParametersInfo.Parameter[] params = new MethodParametersInfo.Parameter[param_count];
                    for (char j = 0; j < param_count; j++) {
                        params[j] = new MethodParametersInfo.Parameter(stream.readUnsignedShort(), stream.readUnsignedShort());
                    }
                    info[i] = new MethodParametersInfo(name_index, len, param_count, params);
                    break;
                case Module:
                    int requires_count = stream.readUnsignedShort();
                    ModuleInfo.Requires[] requires = new ModuleInfo.Requires[requires_count];
                    for (int j = 0; j < requires_count; j++) {
                        requires[j] = new ModuleInfo.Requires(stream.readUnsignedShort(), stream.readUnsignedShort(), stream.readUnsignedShort());
                    }
                    int exports_count = stream.readUnsignedShort();
                    ModuleInfo.Exports[] exports = new ModuleInfo.Exports[exports_count];
                    for (int j = 0; j < exports_count; j++) {
                        int eindex = stream.readUnsignedShort();
                        int flags = stream.readUnsignedShort();
                        int ecount = stream.readUnsignedShort();
                        int[] exports_index = new int[ecount];
                        for (int k = 0; k < ecount; k++) {
                            exports_index[k] = stream.readUnsignedShort();
                        }
                        exports[j] = new ModuleInfo.Exports(eindex, flags, ecount, exports_index);
                    }
                    int opens_count = stream.readUnsignedShort();
                    ModuleInfo.Opens[] opens = new ModuleInfo.Opens[opens_count];
                    for (int j = 0; j < opens_count; j++) {
                        int oindex = stream.readUnsignedShort();
                        int flags = stream.readUnsignedShort();
                        int ocount = stream.readUnsignedShort();
                        int[] opens_index = new int[ocount];
                        for (int k = 0; k < ocount; k++) {
                            opens_index[k] = stream.readUnsignedShort();
                        }
                        opens[j] = new ModuleInfo.Opens(oindex, flags, ocount, opens_index);
                    }
                    int uses_count = stream.readUnsignedShort();
                    int[] uses = new int[uses_count];
                    for (int j = 0; j < uses_count; j++) {
                        uses[j] = stream.readUnsignedShort();
                    }
                    int provides_count = stream.readUnsignedShort();
                    ModuleInfo.Provides[] provides = new ModuleInfo.Provides[provides_count];
                    for (int j = 0; j < provides_count; j++) {
                        int pindex = stream.readUnsignedShort();
                        int pcount = stream.readUnsignedShort();
                        int[] provides_index = new int[pcount];
                        for (int k = 0; k < pcount; k++) {
                            provides_index[k] = stream.readUnsignedShort();
                        }
                        provides[j] = new ModuleInfo.Provides(pindex, pcount, provides_index);
                    }
                    info[i] = new ModuleInfo(name_index, len, requires_count, requires, exports_count, exports, opens_count, opens, uses_count, uses, provides_count, provides);
                    break;
                case ModulePackages:
                    int package_count = stream.readUnsignedShort();
                    int[] packages = new int[package_count];
                    for (int j = 0; j < package_count; j++) {
                        packages[j] = stream.readUnsignedShort();
                    }
                    info[i] = new ModulePackagesInfo(name_index, len, package_count, packages);
                    break;
                case ModuleMainClass:
                    info[i] = new ModuleMainClass(name_index, len, stream.readUnsignedShort());
                    break;
                default:
                    char[][] buf = BytesUtil.allocate(len);
                    for (int j = 0; j < buf.length; j++) {
                        byte[] dat = new byte[buf[j].length];
                        stream.readFully(dat, 0, buf[j].length);
                        buf[j] = ByteBuffer.wrap(dat).asCharBuffer().array();
                    }
                    info[i] = new DefaultAttributeInfo(name_index, len, buf);
                    break;
            }
        }
        return info;
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
        private final CodeInfo.ExceptionTableInfo[] exception_table;
        private final int attributes_count; //Unsigned short
        private final AttributeInfo[] attributes;

        public CodeInfo(int attribute_name_index, long attribute_length, int max_stack, int max_locals, long code_length, char[][] code, int exception_table_length, CodeInfo.ExceptionTableInfo[] exception_table, int attributes_count, AttributeInfo[] attributes) {
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
            char[][] buf = BytesUtil.allocate(getInfoByteLength());
            int inserted = BytesUtil.insert
                    (new char[]{(char) ((max_stack >> 8) & 0xFF),
                            (char) (max_stack & 0xFF),
                            (char) ((max_locals >> 8) & 0xFF),
                            (char) (max_locals & 0xFF),
                            (char) (code_length >> 24),
                            (char)((code_length >> 16) & 0xFF),
                            (char)((code_length >> 8) & 0xFF),
                            (char) (code_length & 0xFF)}, buf, 0);

            for (char[] chunk : code) {
                inserted += BytesUtil.insert(chunk, buf, inserted);
            }

            inserted += BytesUtil.insert(new char[]{(char) ((exception_table_length >> 8) & 0xFF),
                    (char) (exception_table_length & 0xFF)}, buf, inserted);

            for (CodeInfo.ExceptionTableInfo tableInfo : exception_table) {
                inserted += BytesUtil.insert(tableInfo.getBytes(), buf, inserted);
            }

            inserted += BytesUtil.insert(new char[]{(char) ((attributes_count >> 8) & 0xFF),
                    (char) (attributes_count & 0xFF)}, buf, inserted);

            for (AttributeInfo attributeInfo : attributes) {
                for (char[] chunk : attributeInfo.getInfo()) {
                    inserted += BytesUtil.insert(chunk, buf, inserted);
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

        public CodeInfo.ExceptionTableInfo[] getExceptionTable() {
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
        private final StackMapTableInfo.StackMapFrame[] entries;

        public StackMapTableInfo(int attribute_name_index, long attribute_length, int number_of_entries, StackMapTableInfo.StackMapFrame[] entries) {
            super(attribute_name_index, attribute_length);
            this.number_of_entries = number_of_entries;
            this.entries = entries;
        }

        @Override
        public char[][] getInfo() {
            char[][] info = BytesUtil.allocate(getInfoByteLength());
            long offset = BytesUtil.insert(new char[]{(char) (number_of_entries >> 8),
                    (char) (number_of_entries & 0xFF)}, info, 0);
            for (StackMapTableInfo.StackMapFrame entry : entries)
                offset += BytesUtil.insert(entry.toBytes(), info, offset);
            return info;
        }

        @Override
        public long getInfoByteLength() {
            long total = 2L;
            for (StackMapTableInfo.StackMapFrame entry : entries)
                total += entry.getInfoByteLength();
            return total;
        }

        public int getNumberOfEntries() {
            return number_of_entries;
        }

        public StackMapTableInfo.StackMapFrame[] getEntries() {
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

            public static final class SameFrame extends StackMapTableInfo.StackMapFrame {

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

            public static final class SameLocals1StackItemFrame extends StackMapTableInfo.StackMapFrame {

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

            public static final class SameLocals1StackItemFrameExtended extends StackMapTableInfo.StackMapFrame {

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

            public static final class ChopFrame extends StackMapTableInfo.StackMapFrame {

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

            public static final class SameFrameExtended extends StackMapTableInfo.StackMapFrame {

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

            public static final class AppendFrame extends StackMapTableInfo.StackMapFrame {

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

            public static final class FullFrame extends StackMapTableInfo.StackMapFrame {

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

                //Tags
                public static final char ITEM_Top = 0;
                public static final char ITEM_Integer = 1;
                public static final char ITEM_Float = 2;
                public static final char ITEM_Null = 5;
                public static final char ITEM_UninitializedThis = 6;
                public static final char ITEM_Object = 7;
                public static final char ITEM_Uninitialized = 8;
                public static final char ITEM_Long = 4;
                public static final char ITEM_Double = 3;

                public static VerificationTypeInfo readVerificationTypeInfo(DataInputStream stream) throws IOException {
                    char tag = (char) stream.readByte();
                    switch (tag) {
                        case ITEM_Top:
                            return new TopVariableInfo(tag);
                        case ITEM_Integer:
                            return new IntegerVariableInfo(tag);
                        case ITEM_Float:
                            return new FloatVariableInfo(tag);
                        case ITEM_Null:
                            return new NullVariableInfo(tag);
                        case ITEM_UninitializedThis:
                            return new UninitializedThisVariableInfo(tag);
                        case ITEM_Object:
                            return new ObjectVariableInfo(tag, stream.readUnsignedShort());
                        case ITEM_Uninitialized:
                            return new UninitializedVariableInfo(tag, stream.readUnsignedShort());
                        case ITEM_Long:
                            return new LongVariableInfo(tag);
                        case ITEM_Double:
                            return new DoubleVariableInfo(tag);
                        default:
                            throw new IOException("Invalid verification type!");
                    }
                }

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

            public static final class TopVariableInfo extends StackMapTableInfo.StackMapFrame.VerificationTypeInfo {

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

            public static final class IntegerVariableInfo extends StackMapTableInfo.StackMapFrame.VerificationTypeInfo {

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

            public static final class FloatVariableInfo extends StackMapTableInfo.StackMapFrame.VerificationTypeInfo {

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

            public static final class NullVariableInfo extends StackMapTableInfo.StackMapFrame.VerificationTypeInfo {

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

            public static final class UninitializedThisVariableInfo extends StackMapTableInfo.StackMapFrame.VerificationTypeInfo {

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

            public static final class ObjectVariableInfo extends StackMapTableInfo.StackMapFrame.VerificationTypeInfo {

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

            public static final class UninitializedVariableInfo extends StackMapTableInfo.StackMapFrame.VerificationTypeInfo {

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

            public static final class LongVariableInfo extends StackMapTableInfo.StackMapFrame.VerificationTypeInfo {

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

            public static final class DoubleVariableInfo extends StackMapTableInfo.StackMapFrame.VerificationTypeInfo {

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
            char[][] buf = BytesUtil.allocate(getInfoByteLength());
            long offset = BytesUtil.insert(new char[]{(char) (number_of_exceptions >> 8),
                    (char) (number_of_exceptions & 0xFF)}, buf, 0L);
            for (int index : exception_index_table) {
                offset += BytesUtil.insert(new char[]{(char) (index >> 8),
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
        private final InnerClassesInfo.InnerClass[] classes;

        public InnerClassesInfo(int attribute_name_index, long attribute_length, int number_of_classes, InnerClassesInfo.InnerClass[] classes) {
            super(attribute_name_index, attribute_length);
            this.number_of_classes = number_of_classes;
            this.classes = classes;
        }

        @Override
        public char[][] getInfo() {
            char[][] buf = BytesUtil.allocate(getInfoByteLength());
            long offset = BytesUtil.insert(new char[] {(char) (number_of_classes >> 8),
                    (char) (number_of_classes & 0xFF)}, buf, 0);
            for (InnerClassesInfo.InnerClass clazz : classes) {
                offset += BytesUtil.insert(clazz.getBytes(), buf, offset);
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

        public InnerClassesInfo.InnerClass[] getClasses() {
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
        private final LineNumberTableInfo.LineNumber[] line_number_table;

        public LineNumberTableInfo(int attribute_name_index, long attribute_length, int line_number_table_length, LineNumberTableInfo.LineNumber[] line_number_table) {
            super(attribute_name_index, attribute_length);
            this.line_number_table_length = line_number_table_length;
            this.line_number_table = line_number_table;
        }

        @Override
        public char[][] getInfo() {
            char[][] buf = BytesUtil.allocate(getInfoByteLength());
            long offset = BytesUtil.insert(new char[]{(char) (line_number_table_length >> 8),
                    (char) (line_number_table_length & 0xFF)}, buf, 0);
            for (LineNumberTableInfo.LineNumber lineNumber : line_number_table) {
                offset += BytesUtil.insert(lineNumber.getBytes(), buf, offset);
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

        public LineNumberTableInfo.LineNumber[] getLineNumberTable() {
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
        private final LocalVariableTableInfo.LocalVariable[] local_variable_table;

        public LocalVariableTableInfo(int attribute_name_index, long attribute_length, int local_variable_table_length, LocalVariableTableInfo.LocalVariable[] local_variable_table) {
            super(attribute_name_index, attribute_length);
            this.local_variable_table_length = local_variable_table_length;
            this.local_variable_table = local_variable_table;
        }

        @Override
        public char[][] getInfo() {
            char[][] buf = BytesUtil.allocate(getInfoByteLength());
            long offset = BytesUtil.insert(new char[]{(char) (local_variable_table_length >> 8),
                    (char) (local_variable_table_length & 0xFF)}, buf, 0);
            for (LocalVariableTableInfo.LocalVariable variable : local_variable_table) {
                offset += BytesUtil.insert(variable.getBytes(), buf, offset);
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

        public LocalVariableTableInfo.LocalVariable[] getLocalVariableTable() {
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
        private final LocalVariableTypeTableInfo.LocalVariableType[] local_variable_type_table;

        public LocalVariableTypeTableInfo(int attribute_name_index, long attribute_length, int local_variable_type_table_length, LocalVariableTypeTableInfo.LocalVariableType[] local_variable_type_table) {
            super(attribute_name_index, attribute_length);
            this.local_variable_type_table_length = local_variable_type_table_length;
            this.local_variable_type_table = local_variable_type_table;
        }

        @Override
        public char[][] getInfo() {
            char[][] buf = BytesUtil.allocate(getInfoByteLength());
            long offset = BytesUtil.insert(new char[]{(char) (local_variable_type_table_length >> 8),
                    (char) (local_variable_type_table_length & 0xFF)}, buf, 0);
            for (LocalVariableTypeTableInfo.LocalVariableType type : local_variable_type_table) {
                offset += BytesUtil.insert(type.getBytes(), buf, offset);
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

        public LocalVariableTypeTableInfo.LocalVariableType[] getLocalVariableTypeTable() {
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
                pairs[i] = new ElementValuePair(stream.readUnsignedShort(), ElementValue.readElementValue(stream));
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
                    return new ElementValue.PrimitiveElementValue(tag, stream.readUnsignedShort());
                case ENUM_TYPE:
                    return new ElementValue.EnumValue(tag, stream.readUnsignedShort(), stream.readUnsignedShort());
                case CLASS:
                    return new ElementValue.ClassValue(tag, stream.readUnsignedShort());
                case ANNOTATION_TYPE:
                    return new ElementValue.AnnotationValue(tag, AnnotationInfo.readAnnotationValue(stream));
                case ARRAY_TYPE:
                    int value_count = stream.readUnsignedShort();
                    ElementValue[] values = new ElementValue[value_count];
                    for (int i = 0; i < value_count; i++) {
                        values[i] = readElementValue(stream);
                    }
                    return new ElementValue.ArrayValue(tag, value_count, values);
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
            char[][] buf = BytesUtil.allocate(getInfoByteLength());
            long offset = BytesUtil.insert(new char[]{(char) (num_annotations >> 8),
                    (char) (num_annotations & 0xFF)}, buf, 0);

            for (AnnotationInfo annotation : annotations) {
                offset += BytesUtil.insert(annotation.getBytes(), buf, offset);
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
            char[][] buf = BytesUtil.allocate(getInfoByteLength());
            long offset = BytesUtil.insert(new char[]{(char) (num_annotations >> 8),
                    (char) (num_annotations & 0xFF)}, buf, 0);

            for (AnnotationInfo annotation : annotations) {
                offset += BytesUtil.insert(annotation.getBytes(), buf, offset);
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
            char[][] buf = BytesUtil.allocate(getInfoByteLength());
            long offset = BytesUtil.insert(new char[]{num_parameters}, buf, 0);
            for (ParameterAnnotationInfo annotation : parameter_annotations)
                offset += BytesUtil.insert(annotation.getBytes(), buf, offset);
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
            char[][] buf = BytesUtil.allocate(getInfoByteLength());
            long offset = BytesUtil.insert(new char[]{num_parameters}, buf, 0);
            for (ParameterAnnotationInfo annotation : parameter_annotations)
                offset += BytesUtil.insert(annotation.getBytes(), buf, offset);
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

    public static class TypeAnnotationInfo {

        public static TypeAnnotationInfo readTypeAnnotationInfo(DataInputStream stream) throws IOException {
            char target_type = (char) stream.readByte();
            TargetInfo target;
            switch (target_type) {
                case 0x00:
                case 0x01:
                    target = new TargetInfo.TypeParameterTargetInfo((char) stream.readByte());
                    break;
                case 0x10:
                    target = new TargetInfo.SupertypeTargetInfo(stream.readUnsignedShort());
                    break;
                case 0x11:
                case 0x12:
                    target = new TargetInfo.TypeParameterBoundTargetInfo((char) stream.readByte(), (char) stream.readByte());
                    break;
                case 0x13:
                case 0x14:
                case 0x15:
                    target = new TargetInfo.EmptyTargetInfo();
                    break;
                case 0x16:
                    target = new TargetInfo.FormalParameterTargetInfo((char) stream.readByte());
                    break;
                case 0x17:
                    target = new TargetInfo.ThrowsTargetInfo(stream.readUnsignedShort());
                    break;
                case 0x40:
                case 0x41:
                    int table_len = stream.readUnsignedShort();
                    TargetInfo.LocalVarTargetInfo.Table[] tables = new TargetInfo.LocalVarTargetInfo.Table[table_len];
                    for (int i = 0; i < table_len; i++) {
                        tables[i] = new TargetInfo.LocalVarTargetInfo.Table(stream.readUnsignedShort(), stream.readUnsignedShort(), stream.readUnsignedShort());
                    }
                    target = new TargetInfo.LocalVarTargetInfo(table_len, tables);
                    break;
                case 0x42:
                    target = new TargetInfo.CatchTargetInfo(stream.readUnsignedShort());
                    break;
                case 0x43:
                case 0x44:
                case 0x45:
                case 0x46:
                    target = new TargetInfo.OffsetTargetInfo(stream.readUnsignedShort());
                    break;
                case 0x47:
                case 0x48:
                case 0x49:
                case 0x4A:
                case 0x4B:
                   target = new TargetInfo.TypeArgumentTargetInfo(stream.readUnsignedShort(), (char) stream.readByte());
                   break;
                default:
                    throw new IOException("Invalid element!");

            }
            char path_len = (char) stream.readByte();
            TypePath.Path[] path = new TypePath.Path[path_len];
            for (char i = 0; i < path_len; i++)
                path[i] = new TypePath.Path((char) stream.readByte(), (char) stream.readByte());
            TypePath type_path = new TypePath(path_len, path);
            int type_index = stream.readUnsignedShort();
            int pair_count = stream.readUnsignedShort();
            ElementValuePair[] pairs = new ElementValuePair[pair_count];
            for (int i = 0; i < pair_count; i++) {
                pairs[i] = new ElementValuePair(stream.readUnsignedShort(), ElementValue.readElementValue(stream));
            }
            return new TypeAnnotationInfo(target_type, target, type_path, type_index, pair_count, pairs);
        }

        private final char target_type;
        private final TargetInfo target_info;
        private final TypePath path;
        private final int type_index; //Unsigned short
        private final int num_element_value_pairs; //Unsigned short
        private final ElementValuePair[] element_value_pairs;

        public TypeAnnotationInfo(char target_type, TargetInfo target_info, TypePath path, int type_index, int num_element_value_pairs, ElementValuePair[] element_value_pairs) {
            this.target_type = target_type;
            this.target_info = target_info;
            this.path = path;
            this.type_index = type_index;
            this.num_element_value_pairs = num_element_value_pairs;
            this.element_value_pairs = element_value_pairs;
        }

        public char[] getBytes() {
            char[] buf = new char[getByteLength()];
            buf[0] = target_type;
            int offset = 1;
            char[] info = target_info.getBytes();
            System.arraycopy(info, 0, buf, offset, info.length);
            offset += info.length;
            char[] p = path.getBytes();
            System.arraycopy(p, 0, buf, offset, p.length);
            offset += p.length;
            buf[offset++] = (char) (type_index >> 8);
            buf[offset++] = (char) (type_index & 0xFF);
            buf[offset++] = (char) (num_element_value_pairs >> 8);
            buf[offset++] = (char) (num_element_value_pairs & 0xFF);
            for (ElementValuePair pair : element_value_pairs) {
                char[] dat = pair.getBytes();
                System.arraycopy(dat, 0, buf, offset, dat.length);
                offset += dat.length;
            }
            return buf;
        }

        public int getByteLength() {
            int len = 5 + target_info.getByteLength() + path.getByteLength();
            for (ElementValuePair pair : element_value_pairs)
                len += pair.getByteLength();
            return len;
        }

        public char getTargetType() {
            return target_type;
        }

        public TargetInfo getTargetOnfo() {
            return target_info;
        }

        public TypePath getPath() {
            return path;
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

        public static abstract class TargetInfo {

            public abstract char[] getBytes();

            public abstract int getByteLength();

            public static final class TypeParameterTargetInfo extends TargetInfo {

                private final char type_parameter_index;

                public TypeParameterTargetInfo(char type_parameter_index) {
                    this.type_parameter_index = type_parameter_index;
                }

                @Override
                public char[] getBytes() {
                    return new char[]{type_parameter_index};
                }

                @Override
                public int getByteLength() {
                    return 1;
                }

                public char getTypeParameterIndex() {
                    return type_parameter_index;
                }
            }

            public static final class SupertypeTargetInfo extends TargetInfo {

                private final int supertype_index; //Unsigned short!

                public SupertypeTargetInfo(int supertype_index) {
                    this.supertype_index = supertype_index;
                }

                @Override
                public char[] getBytes() {
                    return new char[]{(char) (supertype_index >> 8),
                            (char) (supertype_index & 0xFF)};
                }

                @Override
                public int getByteLength() {
                    return 2;
                }

                public int getSupertypeIndex() {
                    return supertype_index;
                }
            }

            public static final class TypeParameterBoundTargetInfo extends TargetInfo {

                private final char type_parameter_index;
                private final char bound_index;

                public TypeParameterBoundTargetInfo(char type_parameter_index, char bound_index) {
                    this.type_parameter_index = type_parameter_index;
                    this.bound_index = bound_index;
                }

                @Override
                public char[] getBytes() {
                    return new char[]{type_parameter_index, bound_index};
                }

                @Override
                public int getByteLength() {
                    return 2;
                }

                public char getTypeParameterIndex() {
                    return type_parameter_index;
                }

                public char getBoundIndex() {
                    return bound_index;
                }
            }

            public static final class EmptyTargetInfo extends TargetInfo {

                @Override
                public char[] getBytes() {
                    return new char[0];
                }

                @Override
                public int getByteLength() {
                    return 0;
                }
            }

            public static final class FormalParameterTargetInfo extends TargetInfo {

                private final char formal_parameter_index;

                public FormalParameterTargetInfo(char formal_parameter_index) {
                    this.formal_parameter_index = formal_parameter_index;
                }

                @Override
                public char[] getBytes() {
                    return new char[]{formal_parameter_index};
                }

                @Override
                public int getByteLength() {
                    return 1;
                }

                public char getFormalParameterIndex() {
                    return formal_parameter_index;
                }
            }

            public static final class ThrowsTargetInfo extends TargetInfo {

                private final int throws_type_index; //Unsigned short

                public ThrowsTargetInfo(int throws_type_index) {
                    this.throws_type_index = throws_type_index;
                }

                @Override
                public char[] getBytes() {
                    return new char[]{(char) (throws_type_index >> 8),
                            (char) (throws_type_index & 0xFF)};
                }

                @Override
                public int getByteLength() {
                    return 2;
                }

                public int getThrowsTypeIndex() {
                    return throws_type_index;
                }
            }

            public static final class LocalVarTargetInfo extends TargetInfo {

                private final int table_length; //Unsigned short
                private final Table[] table;

                public LocalVarTargetInfo(int table_length, Table[] table) {
                    this.table_length = table_length;
                    this.table = table;
                }

                @Override
                public char[] getBytes() {
                    char[] buf = new char[2 + (table_length * 6)];
                    buf[0] = (char) (table_length >> 8);
                    buf[1] = (char) (table_length & 0xFF);

                    int offset = 2;
                    for (Table t : table) {
                        System.arraycopy(t.getBytes(), 0, buf, offset, 6);
                        offset += 6;
                    }
                    return buf;
                }

                public int getTableLength() {
                    return table_length;
                }

                public Table[] getTable() {
                    return table;
                }

                @Override
                public int getByteLength() {
                    return table_length * 6;
                }

                public static final class Table {

                    private final int start_pc; //Unsigned short
                    private final int length; //Unsigned short
                    private final int index; //Unsigned short

                    public Table(int start_pc, int length, int index) {
                        this.start_pc = start_pc;
                        this.length = length;
                        this.index = index;
                    }

                    public char[] getBytes() {
                        return new char[] {(char) (start_pc >> 8),
                                (char) (start_pc & 0xFF),
                                (char) (length >> 8),
                                (char) (length & 0xFF),
                                (char) (index >> 8),
                                (char) (index & 0xFF)};
                    }

                    public int getByteLength() {
                        return 6;
                    }

                    public int getStartPc() {
                        return start_pc;
                    }

                    public int getLength() {
                        return length;
                    }

                    public int getIndex() {
                        return index;
                    }
                }
            }

            public static final class CatchTargetInfo extends TargetInfo {

                private final int exception_table_index; //Unsigned short

                public CatchTargetInfo(int exception_table_index) {
                    this.exception_table_index = exception_table_index;
                }

                @Override
                public char[] getBytes() {
                    return new char[]{(char) (exception_table_index >> 8),
                            (char) (exception_table_index & 0xFF)};
                }

                @Override
                public int getByteLength() {
                    return 2;
                }

                public int getExceptionTableIndex() {
                    return exception_table_index;
                }
            }

            public static final class OffsetTargetInfo extends TargetInfo {

                private final int offset; //Unsigned short

                public OffsetTargetInfo(int offset) {
                    this.offset = offset;
                }

                @Override
                public char[] getBytes() {
                    return new char[] {(char) (offset >> 8),
                            (char) (offset & 0xFF)};
                }

                @Override
                public int getByteLength() {
                    return 2;
                }

                public int getOffset() {
                    return offset;
                }
            }

            public static final class TypeArgumentTargetInfo extends TargetInfo {

                private final int offset; //Unsigned short
                private final char type_argument_index;

                public TypeArgumentTargetInfo(int offset, char type_argument_index) {
                    this.offset = offset;
                    this.type_argument_index = type_argument_index;
                }

                @Override
                public char[] getBytes() {
                    return new char[]{(char) (offset >> 8),
                            (char) (offset & 0xFF),
                    type_argument_index};
                }

                @Override
                public int getByteLength() {
                    return 3;
                }

                public int getOffset() {
                    return offset;
                }

                public char getTypeArgumentIndex() {
                    return type_argument_index;
                }
            }
        }

        public static final class TypePath {

            private final char path_length;
            private final Path[] path;

            public TypePath(char path_length, Path[] path) {
                this.path_length = path_length;
                this.path = path;
            }

            public char[] getBytes() {
                char[] buf = new char[getByteLength()];
                buf[0] = path_length;
                int offset = 1;
                for (Path p : path) {
                    buf[offset++] = p.type_path_kind;
                    buf[offset++] = p.type_argument_index;
                }
                return buf;
            }

            public int getByteLength() {
                return path_length + (2 * path.length);
            }

            public char getPathLength() {
                return path_length;
            }

            public Path[] getPath() {
                return path;
            }

            public static final class Path {

                private final char type_path_kind;
                private final char type_argument_index;

                public Path(char type_path_kind, char type_argument_index) {
                    this.type_path_kind = type_path_kind;
                    this.type_argument_index = type_argument_index;
                }

                public char[] getBytes() {
                    return new char[]{type_path_kind, type_argument_index};
                }

                public int getByteLength() {
                    return 2;
                }

                public char getTypePathKind() {
                    return type_path_kind;
                }

                public char getTypeArgumentIndex() {
                    return type_argument_index;
                }
            }
        }
    }

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.20
     */
    public static final class RuntimeVisibleTypeAnnotationsInfo extends AttributeInfo {

        private final int num_annotations; //Unsigned short
        private final TypeAnnotationInfo[] annotations;

        public RuntimeVisibleTypeAnnotationsInfo(int attribute_name_index, long attribute_length, int num_annotations, TypeAnnotationInfo[] annotations) {
            super(attribute_name_index, attribute_length);
            this.num_annotations = num_annotations;
            this.annotations = annotations;
        }

        @Override
        public char[][] getInfo() {
            char[][] buf = BytesUtil.allocate(getInfoByteLength());
            long offset = BytesUtil.insert(new char[]{(char) (num_annotations >> 8),
                    (char) (num_annotations & 0xFF)}, buf, 0);
            for (TypeAnnotationInfo annotation : annotations) {
                offset += BytesUtil.insert(annotation.getBytes(), buf, offset);
            }
            return buf;
        }

        @Override
        public long getInfoByteLength() {
            long len = 2L;
            for (TypeAnnotationInfo info : annotations)
                len += info.getByteLength();
            return len;
        }

        public int getNumAnnotations() {
            return num_annotations;
        }

        public TypeAnnotationInfo[] getAnnotations() {
            return annotations;
        }
    }

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.21
     */
    public static final class RuntimeInvisibleTypeAnnotationsInfo extends AttributeInfo {

        private final int num_annotations; //Unsigned short
        private final TypeAnnotationInfo[] annotations;

        public RuntimeInvisibleTypeAnnotationsInfo(int attribute_name_index, long attribute_length, int num_annotations, TypeAnnotationInfo[] annotations) {
            super(attribute_name_index, attribute_length);
            this.num_annotations = num_annotations;
            this.annotations = annotations;
        }

        @Override
        public char[][] getInfo() {
            char[][] buf = BytesUtil.allocate(getInfoByteLength());
            long offset = BytesUtil.insert(new char[]{(char) (num_annotations >> 8),
                    (char) (num_annotations & 0xFF)}, buf, 0);
            for (TypeAnnotationInfo annotation : annotations) {
                offset += BytesUtil.insert(annotation.getBytes(), buf, offset);
            }
            return buf;
        }

        @Override
        public long getInfoByteLength() {
            long len = 2L;
            for (TypeAnnotationInfo info : annotations)
                len += info.getByteLength();
            return len;
        }

        public int getNumAnnotations() {
            return num_annotations;
        }

        public TypeAnnotationInfo[] getAnnotations() {
            return annotations;
        }
    }

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.22
     */
    public static final class AnnotationDefaultInfo extends AttributeInfo {

        private final ElementValue default_value;

        public AnnotationDefaultInfo(int attribute_name_index, long attribute_length, ElementValue default_value) {
            super(attribute_name_index, attribute_length);
            this.default_value = default_value;
        }

        @Override
        public char[][] getInfo() {
            return new char[][]{default_value.getBytes()};
        }

        @Override
        public long getInfoByteLength() {
            return default_value.getByteLength();
        }

        public ElementValue getDefaultValue() {
            return default_value;
        }
    }

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.23
     */
    public static final class BootstrapMethodsInfo extends AttributeInfo {

        private final int num_bootstrap_methods;
        private final BootstrapMethod[] bootstrap_methods;

        public BootstrapMethodsInfo(int attribute_name_index, long attribute_length, int num_bootstrap_methods, BootstrapMethod[] bootstrap_methods) {
            super(attribute_name_index, attribute_length);
            this.num_bootstrap_methods = num_bootstrap_methods;
            this.bootstrap_methods = bootstrap_methods;
        }

        @Override
        public char[][] getInfo() {
            char[][] buf = BytesUtil.allocate(getInfoByteLength());
            long offset = BytesUtil.insert(new char[]{(char) (num_bootstrap_methods >> 8),
                    (char) (num_bootstrap_methods & 0xFF)}, buf, 0);
            for (BootstrapMethod method : bootstrap_methods)
                offset += BytesUtil.insert(method.getBytes(), buf, offset);
            return buf;
        }

        @Override
        public long getInfoByteLength() {
            int len = 2;
            for (BootstrapMethod method : bootstrap_methods)
                len += method.getByteLength();
            return len;
        }

        public int getNumBootstrapMethods() {
            return num_bootstrap_methods;
        }

        public BootstrapMethod[] getBootstrapMethods() {
            return bootstrap_methods;
        }

        public static final class BootstrapMethod {

            private final int bootstrap_method_ref; //Unsigned short
            private final int num_bootstrap_arguments; //Unsigned short
            private final int[] bootstrap_arguments; //Unsigned shorts

            public BootstrapMethod(int bootstrap_method_ref, int num_bootstrap_arguments, int[] bootstrap_arguments) {
                this.bootstrap_method_ref = bootstrap_method_ref;
                this.num_bootstrap_arguments = num_bootstrap_arguments;
                this.bootstrap_arguments = bootstrap_arguments;
            }

            public char[] getBytes() {
                char[] buf = new char[getByteLength()];
                buf[0] = (char) (bootstrap_method_ref >> 8);
                buf[1] = (char) (bootstrap_method_ref & 0xFF);
                buf[2] = (char) (num_bootstrap_arguments >> 8);
                buf[3] = (char) (num_bootstrap_arguments & 0xFF);
                int offset = 4;
                for (int arg : bootstrap_arguments) {
                    buf[offset++] = (char) (arg >> 8);
                    buf[offset++] = (char) (arg & 0xFF);
                }
                return buf;
            }

            public int getByteLength() {
                return 4 + (2 * num_bootstrap_arguments);
            }

            public int getBootstrapMethodRef() {
                return bootstrap_method_ref;
            }

            public int getNumBootstrapArguments() {
                return num_bootstrap_arguments;
            }

            public int[] getBootstrapArguments() {
                return bootstrap_arguments;
            }
        }
    }

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.24
     */
    public static final class MethodParametersInfo extends AttributeInfo {

        private final char parameters_count;
        private final Parameter[] parameters;

        public MethodParametersInfo(int attribute_name_index, long attribute_length, char parameters_count, Parameter[] parameters) {
            super(attribute_name_index, attribute_length);
            this.parameters_count = parameters_count;
            this.parameters = parameters;
        }

        @Override
        public char[][] getInfo() {
            char[][] buf = BytesUtil.allocate(getInfoByteLength());
            long offset = BytesUtil.insert(new char[]{parameters_count}, buf, 0);
            for (Parameter param : parameters)
                offset += BytesUtil.insert(param.getBytes(), buf, offset);
            return buf;
        }

        @Override
        public long getInfoByteLength() {
            return 1L + (2L * (long) parameters_count);
        }

        public char getParametersCount() {
            return parameters_count;
        }

        public Parameter[] getParameters() {
            return parameters;
        }

        public static final class Parameter {

            //Access flags
            public static final int ACC_FINAL = 0x0010;
            public static final int ACC_SYNTHETIC = 0x1000;
            public static final int ACC_MANDATED = 0x8000;

            private final int name_index; //Unsigned short
            private final int access_flags; //Unsigned short

            public Parameter(int name_index, int access_flags) {
                this.name_index = name_index;
                this.access_flags = access_flags;
            }

            public char[] getBytes() {
                return new char[] {(char) (name_index >> 8),
                        (char) (name_index & 0xFF),
                        (char) (access_flags >> 8),
                        (char) (access_flags & 0xFF)};
            }

            public int getByteLength() {
                return 4;
            }

            public int getNameIndex() {
                return name_index;
            }

            public int getAccessFlags() {
                return access_flags;
            }
        }
    }

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.25
     */
    public static final class ModuleInfo extends AttributeInfo {

        //Module flags
        public static final int ACC_OPEN = 0x0020;
        public static final int ACC_SYNTHETIC = 0x1000;
        public static final int ACC_MANDATED = 0x8000;

        private final int requires_count; //Unsigned short
        private final Requires[] requires;
        private final int exports_count; //Unsigned short
        private final Exports[] exports;
        private final int opens_count; //Unsigned short
        private final Opens[] opens;
        private final int uses_count; //Unsigned short
        private final int[] uses_index; //Unsigned shorts
        private final int provides_count; //Unsigned short
        private final Provides[] provides;

        public ModuleInfo(int attribute_name_index, long attribute_length, int requires_count, Requires[] requires, int exports_count, Exports[] exports, int opens_count, Opens[] opens, int uses_count, int[] uses_index, int provides_count, Provides[] provides) {
            super(attribute_name_index, attribute_length);
            this.requires_count = requires_count;
            this.requires = requires;
            this.exports_count = exports_count;
            this.exports = exports;
            this.opens_count = opens_count;
            this.opens = opens;
            this.uses_count = uses_count;
            this.uses_index = uses_index;
            this.provides_count = provides_count;
            this.provides = provides;
        }

        @Override
        public char[][] getInfo() {
            char[][] buf = BytesUtil.allocate(getInfoByteLength());
            long offset = BytesUtil.insert(new char[]{(char) (requires_count >> 8),
                    (char) (requires_count & 0xFF)}, buf, 0);
            for (Requires req : requires)
                offset += BytesUtil.insert(req.getBytes(), buf, offset);
            offset += BytesUtil.insert(new char[]{(char) (exports_count >> 8),
                    (char) (exports_count & 0xFF)}, buf, offset);
            for (Exports exp : exports)
                offset += BytesUtil.insert(exp.getBytes(), buf, offset);
            offset += BytesUtil.insert(new char[]{(char) (opens_count >> 8),
                    (char) (opens_count & 0xFF)}, buf, offset);
            for (Opens o : opens)
                offset += BytesUtil.insert(o.getBytes(), buf, offset);
            offset += BytesUtil.insert(new char[] {(char) (uses_count >> 8),
                    (char) (uses_count & 0xFF)}, buf, offset);
            for (int use : uses_index)
                offset += BytesUtil.insert(new char[]{(char) (use >> 8),
                        (char) (use & 0xFF)}, buf, offset);
            offset += BytesUtil.insert(new char[]{(char) (provides_count >> 8),
                    (char) (provides_count & 0xFF)}, buf, offset);
            for (Provides p : provides)
                offset += BytesUtil.insert(p.getBytes(), buf, offset);
            return buf;
        }

        @Override
        public long getInfoByteLength() {
            long len = 10 + (2 * uses_count);
            for (Requires req : requires)
                len += req.getByteLength();
            for (Exports exp : exports)
                len += exp.getByteLength();
            for (Opens op : opens)
                len += op.getByteLength();
            for (Provides prov : provides)
                len += prov.getByteLength();
            return len;
        }

        public int getRequiresCount() {
            return requires_count;
        }

        public Requires[] getRequires() {
            return requires;
        }

        public int getExportsCount() {
            return exports_count;
        }

        public Exports[] getExports() {
            return exports;
        }

        public int getOpensCount() {
            return opens_count;
        }

        public Opens[] getOpens() {
            return opens;
        }

        public int getUsesCount() {
            return uses_count;
        }

        public int[] getUsesCndex() {
            return uses_index;
        }

        public int getProvidesCount() {
            return provides_count;
        }

        public Provides[] getProvides() {
            return provides;
        }

        public static final class Requires {

            //Requires flags
            public static final int ACC_TRANSITIVE = 0x0020;
            public static final int ACC_STATIC_PHASE = 0x0040;
            public static final int ACC_SYNTHETIC = 0x1000;
            public static final int ACC_MANDATED = 0x8000;

            private final int requires_index; //Unsigned short
            private final int requires_flags; //Unsigned short
            private final int requires_version_index; //Unsigned short

            public Requires(int requires_index, int requires_flags, int requires_version_index) {
                this.requires_index = requires_index;
                this.requires_flags = requires_flags;
                this.requires_version_index = requires_version_index;
            }

            public char[] getBytes() {
                return new char[] {(char) (requires_index >> 8),
                        (char) (requires_index & 0xFF),
                        (char) (requires_flags >> 8),
                        (char) (requires_flags & 0xFF),
                        (char) (requires_version_index >> 8),
                        (char) (requires_version_index & 0xFF)};
            }

            public int getByteLength() {
                return 6;
            }

            public int getRequiresIndex() {
                return requires_index;
            }

            public int getRequiresFlags() {
                return requires_flags;
            }

            public int getRequiresVersionIndex() {
                return requires_version_index;
            }
        }

        public static final class Exports {

            //Exports flags
            public static final int ACC_SYNTHETIC = 0x1000;
            public static final int ACC_MANDATED = 0x8000;

            private final int exports_index; //Unsigned short
            private final int exports_flags; //Unsigned short
            private final int exports_to_count; //Unsigned short
            private final int[] exports_to_index; //Unsigned shorts

            public Exports(int exports_index, int exports_flags, int exports_to_count, int[] exports_to_index) {
                this.exports_index = exports_index;
                this.exports_flags = exports_flags;
                this.exports_to_count = exports_to_count;
                this.exports_to_index = exports_to_index;
            }

            public char[] getBytes() {
                char[] buf = new char[getByteLength()];
                buf[0] = (char) (exports_index >> 8);
                buf[1] = (char) (exports_index & 0xFF);
                buf[2] = (char) (exports_flags >> 8);
                buf[3] = (char) (exports_flags & 0xFF);
                buf[4] = (char) (exports_to_count >> 8);
                buf[5] = (char) (exports_to_count & 0xFF);
                int offset = 6;
                for (int i : exports_to_index) {
                    buf[offset++] = (char) (i >> 8);
                    buf[offset++] = (char) (i & 0xFF);
                }
                return buf;
            }

            public int getByteLength() {
                return 6 + (exports_to_count * 2);
            }

            public int getExportsIndex() {
                return exports_index;
            }

            public int getExportsFlags() {
                return exports_flags;
            }

            public int getExportsToCount() {
                return exports_to_count;
            }

            public int[] getExportsToIndex() {
                return exports_to_index;
            }
        }

        public static final class Opens {

            //Opens flags
            public static final int ACC_SYNTHETIC = 0x1000;
            public static final int ACC_MANDATED = 0x8000;

            private final int opens_index; //Unsigned short
            private final int opens_flags; //Unsigned short
            private final int opens_to_count; //Unsigned short
            private final int[] opens_to_index; //Unsigned shorts

            public Opens(int opens_index, int opens_flags, int opens_to_count, int[] opens_to_index) {
                this.opens_index = opens_index;
                this.opens_flags = opens_flags;
                this.opens_to_count = opens_to_count;
                this.opens_to_index = opens_to_index;
            }

            public char[] getBytes() {
                char[] buf = new char[getByteLength()];
                buf[0] = (char) (opens_index >> 8);
                buf[1] = (char) (opens_index & 0xFF);
                buf[2] = (char) (opens_flags >> 8);
                buf[3] = (char) (opens_flags & 0xFF);
                buf[4] = (char) (opens_to_count >> 8);
                buf[5] = (char) (opens_to_count & 0xFF);
                int offset = 6;
                for (int i : opens_to_index) {
                    buf[offset++] = (char) (i >> 8);
                    buf[offset++] = (char) (i & 0xFF);
                }
                return buf;
            }

            public int getByteLength() {
                return 6 + (opens_to_count * 2);
            }

            public int getOpensIndex() {
                return opens_index;
            }

            public int getOpensFlags() {
                return opens_flags;
            }

            public int getOpensToCount() {
                return opens_to_count;
            }

            public int[] getOpensToIndex() {
                return opens_to_index;
            }
        }

        public static final class Provides {
            
            private final int provides_index; //Unsigned short
            private final int provides_with_count; //Unsigned short
            private final int[] provides_with_index; //Unsigned shorts

            public Provides(int provides_index, int provides_with_count, int[] provides_with_index) {
                this.provides_index = provides_index;
                this.provides_with_count = provides_with_count;
                this.provides_with_index = provides_with_index;
            }

            public char[] getBytes() {
                char[] buf = new char[getByteLength()];
                buf[0] = (char) (provides_index >> 8);
                buf[1] = (char) (provides_index & 0xFF);
                buf[4] = (char) (provides_with_count >> 8);
                buf[5] = (char) (provides_with_count & 0xFF);
                int offset = 6;
                for (int i : provides_with_index) {
                    buf[offset++] = (char) (i >> 8);
                    buf[offset++] = (char) (i & 0xFF);
                }
                return buf;
            }

            public int getByteLength() {
                return 4 + (provides_with_count * 2);
            }

            public int getProvidesIndex() {
                return provides_index;
            }

            public int getProvidesWithCount() {
                return provides_with_count;
            }

            public int[] getProvidesWithIndex() {
                return provides_with_index;
            }
        }
    }

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.26
     */
    public static final class ModulePackagesInfo extends AttributeInfo {

        private final int package_count; //Unsigned short
        private final int[] package_index; //Unsigned shorts

        public ModulePackagesInfo(int attribute_name_index, long attribute_length, int package_count, int[] package_index) {
            super(attribute_name_index, attribute_length);
            this.package_count = package_count;
            this.package_index = package_index;
        }

        @Override
        public char[][] getInfo() {
            char[][] buf = BytesUtil.allocate(getInfoByteLength());
            long offset = BytesUtil.insert(new char[]{(char) (package_count >> 8),
                    (char) (package_count & 0xFF)}, buf, 0);
            for (int i : package_index)
                BytesUtil.insert(new char[]{(char) (i >> 8),
                        (char) (i & 0xFF)}, buf, offset);
            return buf;
        }

        @Override
        public long getInfoByteLength() {
            return 2L + (2L * (long) package_count);
        }

        public int getPackageCount() {
            return package_count;
        }

        public int[] getPackageIndex() {
            return package_index;
        }
    }

    /**
     * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.7.27
     */
    public static final class ModuleMainClass extends AttributeInfo {

        private final int main_class_index; //Unsigned short

        public ModuleMainClass(int attribute_name_index, long attribute_length, int main_class_index) {
            super(attribute_name_index, attribute_length);
            this.main_class_index = main_class_index;
        }

        @Override
        public char[][] getInfo() {
            return new char[][]{{(char) (main_class_index >> 8), (char) (main_class_index & 0xFF)}};
        }

        @Override
        public long getInfoByteLength() {
            return 2L;
        }

        public int getMainClassIndex() {
            return main_class_index;
        }
    }
}