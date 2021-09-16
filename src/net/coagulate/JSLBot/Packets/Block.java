package net.coagulate.JSLBot.Packets;

import net.coagulate.JSLBot.Packets.Types.Type;
import net.coagulate.JSLBot.Packets.Types.U8;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.*;

/** Generic Block - the pieces that build UDP messages.
 *
 * @author Iain Price
 */
public abstract class Block {
    @Nonnull
    public ByteBuffer writeBlock() { throw new UnsupportedOperationException("Not implemented"); }
    public void readBlock(ByteBuffer in)  { throw new UnsupportedOperationException("Not implemented"); }
    
    @SuppressWarnings("unchecked") // reflect is a pain enough without force parameterising everything
    public int fieldSize(@Nonnull Field f) {
        if (Block.class.isAssignableFrom(f.getType())) {
            try {
                // its a block
                @Nonnull Block b=(Block)(f.getType().getDeclaredConstructor().newInstance());
                return b.size();
            } catch (@Nonnull InvocationTargetException|NoSuchMethodException|InstantiationException|IllegalAccessException ex) {
                throw new IllegalArgumentException("Unable to size supposed Block "+this.getClass().getName()+"/"+f.getName()+" type "+f.getType().getName(),ex);
            }
        }
        if (List.class.isAssignableFrom(f.getType())) {
            try {
                @Nonnull List<Block> l=(List<Block>)(f.get(this));
                @Nonnull Class<?> listtype=(Class) ((ParameterizedType)(f.getGenericType())).getActualTypeArguments()[0];
                int size=0;
                for (@Nonnull Block b:l) { size=size+b.size(); }
                return size+(new U8().size());
            } catch (@Nonnull IllegalAccessException | IllegalArgumentException e) {
                throw new IllegalArgumentException("Unable to size supposed variable count block "+this.getClass().getName()+"/"+f.getName()+" type "+f.getType().getName(),e);
            }
        }
        try {
            @Nonnull Type lltype = (Type) f.get(this);
            return lltype.size();
        } catch (IllegalAccessException ex) {
            throw new IllegalArgumentException("Unable to size supposed LL Type "+this.getClass().getName()+" field "+f.getName()+" of type "+f.getType().getName(),ex);
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Block assembly failure "+f.getName()+" is null");
        }
    }
    // gets the block's data fields, in order
    @Nonnull
    private List<Field> getFields() {
        // NOTE the fields all seem to come back in the order they're declared (Oracle JDK 8)
        // this isn't actually guaranteed by the method however, so we're annotating the fields with an ordering number and going off that
        // must start at 0 and be sequential.  The sub blocks start at zero themselves too.
        @Nonnull Map<Integer,Field> map=new HashMap<>();
        @Nonnull Field[] fs=this.getClass().getDeclaredFields();
        for (@Nonnull Field f : fs) {
            if (!f.getName().equals("this$0")) {
                Sequence a=f.getDeclaredAnnotation(Sequence.class);
                // all fields in the block MUST have a sequence
                if (a==null) { throw new IllegalArgumentException("Field "+f.getName()+" of type "+f.getType().getName()+" does not have annotation Sequence (?)"); }
                map.put(a.value(),f);
            }
        }
        @Nonnull List<Field> result=new ArrayList<>();
        @Nonnull List<Integer> sequence=new ArrayList<>();
        sequence.addAll(map.keySet());
        Collections.sort(sequence);
        for (Integer i:sequence) {
            result.add(map.get(i));
        }
        return result;
    }
    public int size() {
        final boolean debug=false;
        if (debug) { System.out.println("Sizing "+this.getClass().getName()); }
        int size=0;
        @Nonnull List<Field> fields=getFields();
        for (@Nonnull Field f:fields) {
            if (Block.class.isAssignableFrom(f.getType())) {
                if (debug) { System.out.println("> Enter Block recursion: "+f.getType().getName()); }
                try {
                    @Nonnull Block b=(Block)(f.get(this));
                    if (b==null) { throw new NullPointerException("Block contents "+this.getClass().getSimpleName()+" is null"); }
                    size+=b.size();
                } catch (@Nonnull IllegalArgumentException |IllegalAccessException ex) {
                    throw new IllegalArgumentException("Exception in block "+this.getClass().getSimpleName()+"",ex);
                }
                if (debug) { System.out.println("< Exit Block recursion: "+f.getType().getName()); }
            } else {
                if (debug) { System.out.println("Field "+f.getName()+" of type "+f.getType().getName()); }
                // cache me?, cos this is stupidly inefficient compared.  though probably still meaningless?
                // removed this as a 'task entry', this may not be this simple, there are Variable length fields, perhaps we have to just scan.
                size+=fieldSize(f);
            }
            
        }
        return size;
    }
    public void messageRead(ByteBuffer in) { throw new UnsupportedOperationException("NOTIMP"); }
    private void fieldToBytes(@Nonnull Field f, @Nonnull ByteBuffer output) {
        final boolean debug=false;
        if (debug) { System.out.println("Outputting field "+f.getName()+" of type "+f.getType().getName()); }
        if (Type.class.isAssignableFrom(f.getType())) {
            try {
                @Nonnull Type t=(Type) (f.get(this));
                t.write(output);
                return;
            } catch (@Nonnull IllegalArgumentException|IllegalAccessException ex) {
                throw new IllegalArgumentException("Internal error during field to bytes on field "+f.getName()+" of type "+f.getType().getName(),ex);
            }
        }
        throw new UnsupportedOperationException("Unknown field type, field name "+f.getName()+" type "+f.getType().getName());
    }
    private void blockToBytes(Block b, @Nonnull ByteBuffer out) {
        for (@Nonnull Field f:getFields()) {
            fieldToBytes(f,out);
        }
    }
    public void writeBytes(@Nonnull ByteBuffer out) {
        @Nonnull List <Field> fields=getFields();
        for (@Nonnull Field f: fields) {
            writeField(out,f);
        }
    }
    private void writeField(@Nonnull ByteBuffer out, @Nonnull Field f) {
        if (Block.class.isAssignableFrom(f.getType())) {
            Block b;
            try {
                b = (Block) f.get(this);
                b.writeBytes(out);
                return;
            } catch (@Nonnull IllegalAccessException|IllegalArgumentException ex) {
                throw new IllegalArgumentException("Error writing field "+f.getName()+" of type "+f.getType().getName(),ex);
            }
        }
        if (List.class.isAssignableFrom(f.getType())) {
            try {
                @Nonnull @SuppressWarnings("unchecked")
                List<Block> l=(List<Block>)(f.get(this));
                @Nonnull U8 qty=new U8();
                qty.value=(byte) l.size();
                qty.write(out);
                for (@Nonnull Block b:l) { b.writeBytes(out); }
                return;
            } catch (@Nonnull IllegalAccessException | IllegalArgumentException e) {
                throw new IllegalArgumentException("Error writing variable count block "+this.getClass().getName()+"/"+f.getName()+" type "+f.getType().getName(),e);
            }
        }        
        fieldToBytes(f,out);
    }
    public void readBytes(@Nonnull ByteBuffer in) {
        for (@Nonnull Field f:getFields()) {
            readField(in,f);
        }
    }
    private void readField(@Nonnull ByteBuffer in, @Nonnull Field f) {
        final boolean debug=false;
        if (debug) { System.out.println("READING FIELD "+f.getName()+" from "+this.getClass().getName()+" type "+f.getType().getName()); }
        if (Block.class.isAssignableFrom(f.getType())) {
            Block b;
            try {
                b = (Block) f.get(this);
                b.readBytes(in);
                return;
            } catch (@Nonnull IllegalAccessException|IllegalArgumentException ex) {
                throw new IllegalArgumentException("Error reading Block field "+f.getName()+" of type "+f.getType().getName(),ex);
            }
        }
        if (List.class.isAssignableFrom(f.getType())) {
            try {
                @Nonnull U8 qty=new U8(0);
                try { qty.read(in); }
                catch (BufferUnderflowException ignored) {
                    //System.out.println("Exception reading qty ; default zero");
                }
                @Nonnull List<Block> list=new ArrayList<>();
                for (int i=0;i<qty.value;i++) {
                    @Nonnull Class<?> listtype=(Class) ((ParameterizedType)(f.getGenericType())).getActualTypeArguments()[0];
                    if (debug) { System.out.println(listtype.getName()); }
                    @Nonnull Block b=(Block) listtype.getDeclaredConstructor().newInstance();
                    b.readBytes(in);
                    list.add(b);
                }
                f.set(this,list);
                return;
            } catch (@Nonnull IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                throw new IllegalArgumentException("Failed to read LIST type (variable block type) from class "+this.getClass().getName()+" field "+f.getName()+" type "+f.getType().getName()+" generic type "+f.getGenericType().getTypeName(),e);
            }
        }

        // else, a LL type?
        try {
            @Nonnull Type value=(Type) f.getType().getDeclaredConstructor().newInstance();
            try { value.read(in); }
            catch (BufferUnderflowException ignored) {
                //System.out.println("Buffer underflow reading a data type "+f.getType());
            }
            f.set(this,value);
        } catch (@Nonnull IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            throw new IllegalArgumentException("Failed to set class "+this.getClass().getName()+" field "+f.getName()+" type "+f.getType().getName(),e);
        }
    }
    public String dump() {
        String ret="";
        ret+=" [=="+this.getClass().getSimpleName()+"==]\n";
        for (@Nonnull Field f:getFields()) {
            ret+=dumpField(f);
        }
        return ret;
    }
    private String dumpField(@Nonnull Field f) {
        String ret="";
        ret+="   field: "+f.getName()+"["+f.getType().getSimpleName()+"]=";
        try {
            Object o=f.get(this);
            if (Block.class.isAssignableFrom(o.getClass())) { 
                @Nonnull Block b=(Block)o;
                ret=ret+b.dump();
                return ret;
            }
            if (List.class.isAssignableFrom(o.getClass())) {
                @Nonnull @SuppressWarnings("unchecked")
                List<Block> l=(List<Block>)o;
                int counter=0;
                for (@Nonnull Block b:l) {
                    ret=ret+"{ #"+counter+" }  "+b.dump();
                    counter++;
                }
            }
            ret+=o.toString()+"\n";
        } catch (@Nonnull NullPointerException|IllegalAccessException | IllegalArgumentException e) {
            ret+=e.toString();
        }
        return ret;
    }
}
