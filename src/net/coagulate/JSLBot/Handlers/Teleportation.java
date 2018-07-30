package net.coagulate.JSLBot.Handlers;

import java.util.HashMap;
import java.util.Map;
import net.coagulate.JSLBot.Circuit;
import net.coagulate.JSLBot.Configuration;
import net.coagulate.JSLBot.Debug;
import net.coagulate.JSLBot.Event;
import net.coagulate.JSLBot.Global;
import net.coagulate.JSLBot.Handler;
import net.coagulate.JSLBot.JSLBot;
import net.coagulate.JSLBot.LLSD.LLSDArray;
import net.coagulate.JSLBot.LLSD.LLSDBinary;
import net.coagulate.JSLBot.LLSD.LLSDInteger;
import net.coagulate.JSLBot.LLSD.LLSDMap;
import net.coagulate.JSLBot.LLSD.LLSDString;
import net.coagulate.JSLBot.Log;
import static net.coagulate.JSLBot.Log.CRIT;
import static net.coagulate.JSLBot.Log.INFO;
import static net.coagulate.JSLBot.Log.NOTE;
import static net.coagulate.JSLBot.Log.debug;
import static net.coagulate.JSLBot.Log.info;
import static net.coagulate.JSLBot.Log.log;
import net.coagulate.JSLBot.Packets.Message;
import net.coagulate.JSLBot.Packets.Messages.CompleteAgentMovement;
import net.coagulate.JSLBot.Packets.Messages.CompleteAgentMovement_bAgentData;
import net.coagulate.JSLBot.Packets.Messages.ImprovedInstantMessage;
import net.coagulate.JSLBot.Packets.Messages.TeleportLocal;
import net.coagulate.JSLBot.Packets.Messages.TeleportLocationRequest;
import net.coagulate.JSLBot.Packets.Messages.TeleportLureRequest;
import net.coagulate.JSLBot.Packets.Messages.TeleportProgress;
import net.coagulate.JSLBot.Packets.Messages.TeleportStart;
import net.coagulate.JSLBot.Packets.Types.LLVector3;
import net.coagulate.JSLBot.Packets.Types.U32;
import net.coagulate.JSLBot.Packets.Types.U64;

/** Implements the teleportation (sub) protocol
 *
 * @author Iain Price <git@predestined.net>
 */
public class Teleportation extends Handler {

    Configuration config;
    public Teleportation(Configuration c){super(c); config=c;}
    @Override
    public String toString() {
        return "Teleportation manager";
    }
    Object signal=new Object();
    JSLBot bot;
    @Override
    public void initialise(JSLBot ai) throws Exception {
        bot=ai;
        ai.addCommand("teleport", this);
        ai.addImmediateHandler("TeleportProgress", this);
        ai.addImmediateHandler("TeleportStart",this);
        ai.addImmediateHandler("XML_TeleportFinish",this);
        ai.addImmediateHandler("TeleportLocal",this);
        ai.addHandler("ImprovedInstantMessage",this);
    }

    @Override
    public void processImmediate(Event event) throws Exception {
        Message m=event.message();
        if (m!=null) { processImmediate(m); return; }
        if (event.getName().equals("XML_TeleportFinish")) {            
            // get the data for the new region
            LLSDMap body=(LLSDMap) event.body();
            //System.out.println(body.toXML());
            LLSDArray info=(LLSDArray) body.get("Info");
            LLSDMap tpinfo=(LLSDMap) info.get().get(0);
            LLSDBinary simip=(LLSDBinary) tpinfo.get("SimIP");
            LLSDInteger simport=(LLSDInteger) tpinfo.get("SimPort");
            LLSDBinary regionhandle=(LLSDBinary) tpinfo.get("RegionHandle");
            if (Debug.REGIONHANDLES) { debug(bot,"TeleportFinish provided regionhandle "+Long.toUnsignedString(regionhandle.toLong())); }
            String targetaddress=simip.toIP();
            // create the circuit and transfer to it
            //System.out.println(event.body().toXML());
            LLSDString caps=(LLSDString) tpinfo.get("SeedCapability");
            Circuit circuit=bot.createCircuit(targetaddress,simport.get(),regionhandle.toLong(),caps.toString());
            bot.setPrimaryCircuit(circuit);
            bot.completeAgentMovement();
            bot.agentUpdate();
            // fire up the event queue
            // set flag, notify the waiting thread
            teleporting=false;
            synchronized(signal) { signal.notifyAll(); }
        }
    }    
    public void processImmediate(Message p) throws Exception {
        if (p instanceof TeleportProgress) {
            TeleportProgress tp=(TeleportProgress) p;
            Log.log(bot,Log.DEBUG,"Teleport Progress: "+((TeleportProgress) p).binfo.vmessage.toString());
        }
        if (p instanceof TeleportStart) {
            TeleportStart tp=(TeleportStart) p;
            log(bot,INFO,"Teleportation has started (with flags "+tp.binfo.vteleportflags.value+")");
        }
        if (p instanceof TeleportLocal) {
            TeleportLocal tp=(TeleportLocal) p;
            info(bot,"Teleportation completed locally");
            teleporting=false;
            synchronized(signal) { signal.notifyAll(); }
        }
    }

    @Override
    public void process(Event e) throws Exception {
        Message p=e.message();
        if (p instanceof ImprovedInstantMessage) {
            ImprovedInstantMessage m=(ImprovedInstantMessage) p;
            int messagetype=m.bmessageblock.vdialog.value;
            String messagetext="["+m.bmessageblock.vfromagentname.toString()+"] "+m.bmessageblock.vmessage.toString();
            // this is a HEAVILY overloaded conduit of information
            // http://wiki.secondlife.com/wiki/ImprovedInstantMessage

            if (messagetype==22) { 
                Log.log(bot,NOTE,"Accepting Teleport Lure: "+messagetext);
                TeleportLureRequest req=new TeleportLureRequest();
                req.binfo.vagentid=bot.getUUID();
                req.binfo.vsessionid=bot.getSession();
                req.binfo.vlureid=m.bmessageblock.vid;
                //System.out.println(m.dump());
                teleporting=true;
                bot.send(req,true);
                synchronized(signal) { signal.wait(10000); }
                if (teleporting==true) {
                    log(bot,CRIT,"Timer expired while teleporting, lost in transit?");
                    bot.im(m.bagentdata.vagentid,"Failed to accept teleport lure, lost in transit?");
                } else {
                    info(bot,"Completed teleport intiated from lure");
                    bot.im(m.bagentdata.vagentid,"Accepted teleport lure and completed transit");
                }
            }
        }
    }

    boolean teleporting=false;
    @Override
    public String execute(String command, Map<String, String> parameters) throws Exception {
        if (command.equals("teleport")) {
            if (!(parameters.containsKey("x"))) { return "Missing X parameter"; }
            if (!(parameters.containsKey("y"))) { return "Missing Y parameter"; }
            if (!(parameters.containsKey("z"))) { return "Missing Z parameter"; }
            if (!(parameters.containsKey("region"))) { return "Missing region parameter"; }
            TeleportLocationRequest tp=new TeleportLocationRequest();
            tp.bagentdata.vagentid=bot.getUUID();
            tp.bagentdata.vsessionid=bot.getSession();
            tp.binfo.vposition=new LLVector3(parameters.get("x"),parameters.get("y"),parameters.get("z"));
            Map<String,String> lookupparams=new HashMap<>();
            lookupparams.put("name",parameters.get("region"));
            String regionhandle=bot.execute("region.lookup", lookupparams);
            if (Debug.REGIONHANDLES) { debug(bot,"Region lookup for "+parameters.get("region")+" gave handle "+new U64(regionhandle)); }
            try { tp.binfo.vregionhandle=new U64(regionhandle);  }
            catch (NumberFormatException e) { return "Failed to resolve region name "+parameters.get("region"); }
            bot.send(tp,true);
            //bot.clearUnhandled(); // this just causes us to spew "unhandled packet" alerts from scratch, for debugging at some point
            teleporting=true;
            synchronized(signal) { signal.wait(10000); }
            if (teleporting==true) { log(bot,CRIT,"Timer expired while teleporting, lost in transit?"); } 
            boolean completed=!teleporting;
            teleporting=false;
            return "TP Sequence finished, success code is "+completed;
        }
        return "No such command "+command;
    }

    @Override
    public void loggedIn() throws Exception {
    }

    @Override
    public String help(String command) {
        if (command.equals("teleport")) { return "teleport region <region> x <x> y <y> z <z>\nTeleport to the specified region and x,y,z co-ordinates"; }
        return "Unknown command "+command;
    }
    
}
