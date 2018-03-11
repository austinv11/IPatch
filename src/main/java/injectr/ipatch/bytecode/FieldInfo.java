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

/**
 * https://docs.oracle.com/javase/specs/jvms/se9/html/jvms-4.html#jvms-4.5
 */
public final class FieldInfo {

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

    public static FieldInfo[] readFieldInfo(int field_info_count, ConstantPoolInfo[] constant_pool, DataInputStream stream) throws IOException {
        FieldInfo[] fields = new FieldInfo[field_info_count];
        for (int i = 0; i < field_info_count; i++) {
            int flags = stream.readUnsignedShort();
            int name = stream.readUnsignedShort();
            int descriptor = stream.readUnsignedShort();
            int attr_count = stream.readUnsignedShort();
            AttributeInfo[] attr = AttributeInfo.readAttributeInfo(attr_count, constant_pool, stream);
            fields[i] = new FieldInfo(flags, name, descriptor, attr_count, attr);
        }
        return fields;
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

    public char[][] getInfo() {
        long len = 8L;
        for (AttributeInfo attr : attributes)
            len += attr.getInfoByteLength() + 6L;
        char[][] buf = BytesUtil.allocate(len);
        long offset = BytesUtil.insert(new char[]{(char) (access_flags >> 8),
                (char) (access_flags & 0xFF),
                (char) (name_index >> 8),
                (char) (name_index & 0xFF),
                (char) (descriptor_index >> 8),
                (char) (descriptor_index & 0xFF),
                (char) (attributes_count >> 8),
                (char) (attributes_count & 0xFF)}, buf, 0);
        for (AttributeInfo info : attributes) {
            offset += BytesUtil.insert(new char[]{(char) (info.getAttributeNameIndex() >> 8),
                    (char) (info.getAttributeNameIndex() & 0xFF),
                    (char) (info.getAttributeLength() >> 24),
                    (char) (info.getAttributeLength() >> 16),
                    (char) (info.getAttributeLength() >> 8),
                    (char) (info.getAttributeLength() & 0xFF)}, buf, offset);
            for (char[] chunk : info.getInfo())
                offset += BytesUtil.insert(chunk, buf, offset);
        }
        return buf;
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
