package net.coagulate.JSLBot.LLSD;

import net.coagulate.JSLBot.Packets.Types.LLUUID;
import org.w3c.dom.Node;

/**
 *
 * @author Iain Price
 */
public class LLSDUUID extends Atomic {

    LLUUID value=new LLUUID();
    public LLSDUUID(String uuid) {
        value=new LLUUID(uuid);
    }

    public LLSDUUID(Node item) {
        value=new LLUUID(item.getTextContent());
    }

    public LLSDUUID() {
        value=new LLUUID();
    }

    @Override
    public String toXML(String lineprefix) {
        return lineprefix+"<uuid>"+value.toUUIDString()+"</uuid>\n";
    }
    public String toString() { return value.toString(); }

    public String get() { return value.toString(); }

    public LLUUID toLLUUID() {
        return value;
    }
}
