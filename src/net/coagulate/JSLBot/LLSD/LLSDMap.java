package net.coagulate.JSLBot.LLSD;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.NodeList;

/**
 *
 * @author Iain Price
 */
public class LLSDMap extends Container {
    private Map<String,Atomic> data=new HashMap<>();
    
    public LLSDMap(NodeList nodes) throws IOException {
        for (int i=0;i<nodes.getLength();i+=2) {
            String key=nodes.item(i).getFirstChild().getNodeValue();
            Atomic a=Atomic.create(nodes.item(i+1));
            if (a!=null) { data.put(key, a); }
        }
    }

    public LLSDMap() {
    }
    
    
    public String toXML(String prefix) {
        String resp=prefix+"<map>\n";
        for (String key:data.keySet()) {
            resp+=prefix+"<key>"+key+"</key>\n";
            resp+=data.get(key).toXML(prefix+"  ");
        }
        resp+=prefix+"</map>\n";
        return resp;
    }

    public Set<String> keys() { return data.keySet(); }
    public boolean containsKey(String key) {
        return data.containsKey(key);
    }
    public Atomic get(String key) { return data.get(key); }
    public Atomic get(String key,Atomic def) { if (data.containsKey(key)) { return data.get(key); } else { return def; } }

    public void put(String ack, Atomic atom) {
        data.put(ack,atom);
    }
}
