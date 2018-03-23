package injectr.ipatch.bytecode;

import injectr.ipatch.util.BytesUtil;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.*;

public class ReadWriteTest {

    @Test
    public void readAndVerify() throws IOException {
        ClassFile file = BytesUtil.readAndVerify(new FileInputStream("E:\\austi\\Development\\IntelliJ\\IPatch\\out\\production\\classes\\injectr\\ipatch\\bytecode\\AttributeInfo.class"));
    }

    @Test
    public void asmVerification() throws IOException {
        ClassFile file = ClassFile.readFrom(new FileInputStream("E:\\austi\\Development\\IntelliJ\\IPatch\\out\\production\\classes\\injectr\\ipatch\\bytecode\\AttributeInfo.class"));
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        file.writeTo(stream);
        CheckClassAdapter.verify(new ClassReader(new ByteArrayInputStream(stream.toByteArray())), true, new PrintWriter(System.out));
    }

    @Test
    public void readWriteRead() throws IOException {
        ClassFile file = ClassFile.readFrom(new FileInputStream("E:\\austi\\Development\\IntelliJ\\IPatch\\out\\production\\classes\\injectr\\ipatch\\bytecode\\AttributeInfo.class"));
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        file.writeTo(stream);
        ClassFile.readFrom(stream.toByteArray());
    }
}
