package net.coagulate.JSLBot.Packets.Types;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

/**
 *
 * @author Iain Price
 */
public class Variable1 extends Type {


    @Nonnull
    public  byte[] value=new byte[0];

    public Variable1(){}
    public Variable1(@Nonnull String string) {
        @Nonnull char[] array=string.toCharArray();
        value=new byte[array.length+1];
        for (int i=0;i<array.length;i++) { value[i]=(byte) array[i]; }
        value[value.length-1]=0;
    }
    @Override
    public int size() {
        return value.length+1;
    }

    @Override
    public void read(@Nonnull ByteBuffer in) {
        @Nonnull U8 length=new U8();
        length.read(in);
        int len=((int)(length.value))&0xff;
        value=new byte[len];
        for (int i=0;i<len;i++) {
            value[i]=in.get();
        }
    }

    @Override
    public void write(@Nonnull ByteBuffer out) {
        @Nonnull U8 length=new U8();
        length.value=(byte)  value.length;
        length.write(out);
        out.put(value);
    }

    @Nonnull
    @Override
    public String dump() {
        return "'"+ this +"'";
    }
    @Nonnull
    public String toString() {
        @Nonnull String str="";
        for (byte b:value) {
            if (b>0) { str=str+(char)b; }
        }
        return str;
    }
    
}
