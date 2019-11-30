package net.coagulate.JSLBot;
import net.coagulate.JSLBot.Packets.Message;
import net.coagulate.JSLBot.Packets.Messages.*;
import net.coagulate.JSLBot.Packets.Packet;
import net.coagulate.JSLBot.Packets.Types.*;
import org.apache.xmlrpc.XmlRpcException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.annotation.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/** Creates and runs a JSLBot.
 *
 * @author Iain Price
 */
public class JSLBot extends Thread {
    public final AtomicInteger bytesin=new AtomicInteger(0);
    public final AtomicInteger bytesout=new AtomicInteger(0);
    public final Map<Integer,Integer> messagebytesin=new HashMap<>();
    public final Map<Integer,Integer> messagebytesout=new HashMap<>();
    public final Integer startuptime=((int) Math.round(new Date().getTime()/1000.0));
    public int getSecondsSinceStartup() {
        int now= (int) Math.round(new Date().getTime() / 1000.0);
        return now-startuptime;
    }
    public void dumpAccounting() {
        System.out.println("DUMP ACCOUNTING FOR BOT "+this.getFullName());
        synchronized(messagebytesin) {
            for (Map.Entry<Integer, Integer> entry : messagebytesin.entrySet()) {
                System.out.println("Message ID : "+ entry.getKey() +" received "+ entry.getValue());
            }
        }
        synchronized(messagebytesout) {
            for (Map.Entry<Integer, Integer> entry : messagebytesout.entrySet()) {
                System.out.println("Message ID : "+ entry.getKey() +" transmitted "+ entry.getValue());
            }
        }
    }
    void accountMessageIn(int id, int length) {
        synchronized(messagebytesin) {
            int sum=0;
            if (messagebytesin.containsKey(id)) { sum=messagebytesin.get(id); }
            sum+=length;
            messagebytesin.put(id,sum);
        }
    }
    void accountMessageOut(int id, int length) {
        synchronized(messagebytesout) {
            int sum=0;
            if (messagebytesout.containsKey(id)) { sum=messagebytesout.get(id); }
            sum+=length;
            messagebytesout.put(id,sum);
        }
    }


    
    private final Logger log;
    // bot level data
    private String firstname;
    private String lastname;
    public boolean registershutdownhook=true;
    @Nonnull
    public String getFullName() { return firstname+" "+lastname; }
    @Nonnull
    public String getUsername() { return firstname+"."+lastname; }

    private String password;
    private String loginlocation;
    private LLUUID sessionid;
    public LLUUID getSession() { return sessionid; }
    private LLUUID uuid; public LLUUID getUUID() { return uuid; }
    private int circuitcode; public int getCircuitCode() { return circuitcode; }
    
    @Nonnull
    private final Brain brain;
    @Nonnull
    public Brain brain(){return brain;}
    
    Configuration config=new TransientConfiguration();

    private boolean quit=false; public boolean quitting() { return quit; }
    private String quitreason="";
    private boolean reconnect=false; public void setReconnect() { reconnect=true; }
    public void forceReconnect() { reconnect=true; shutdown("Forced to reconnect"); }
    public boolean ALWAYS_RECONNECT=false;
    /** Instruct the bot to always reconnect whne disconnected */
    public void setAlwaysReconnect() { ALWAYS_RECONNECT=true; reconnect=true;  }
    JSLInterface jslinterface;
    /** Instruct the bot to attempt to return home periodically
     * @param regionname Name of region we have as home.  Bot will periodically teleport home if not in this region.
     */
    public void homeSickFor(String regionname) { log.info("Registered homesickness towards "+regionname); homesickfor=regionname; }
    @Nullable
    private String homesickfor=null;
    @Nullable
    public String homeSickFor() { return homesickfor; }
    
    @Nullable
    private LLUUID inventoryroot=null;
    @Nullable
    public LLUUID getInventoryRoot() { return inventoryroot; }
    
    /** Get the JSLInterface API object for this bot.
     * 
     * @return JSLInterface for this bot
     */
    public JSLInterface api() { return jslinterface; }

    /** Create a bot based on configuration data.
     *
     * @param conf The configuration data object
     */

    public JSLBot(@Nonnull Configuration conf) {
        log=Logger.getLogger("net.coagulate.JSLBot."+conf.get("firstname")+" "+conf.get("lastname"));
        brain=new Brain(this);
        loadConf(conf);
    }
    
    private void loadConf(Configuration conf) {
        // load from config and call 'setup'
        config=conf; 
        String location=config.get("loginlocation");
        if (location==null || "".equals(location)) { location="home"; }  // default to home
        //String potentialmaster=config.get("owner");
        
        String handlerlist=config.get("handlers","");
        brain.loadHandlers(handlerlist.split(","));
        setup(config.get("firstname"),config.get("lastname"),config.get("password"),location);
        log.info("JSLBot initialisation complete, ready to launch");
    }

    // ********** BOT STARTUP **********
    private boolean connected=false; 
    /** Reports if the bot is currently connected
     * 
     * @return True if connected.
     */
    public boolean connected() { return connected; }
    private final Object connectsignal=new Object();
    /** Wait up to a limited ammount of time for the bot to complete a connection.
     * 
     * @param milliseconds Maximum time to wait for
     * @throws IllegalStateException If we fail to connect due to timeout or being in a quit state. 
     */
    public void waitConnection(long milliseconds) throws IllegalStateException {
        if (quit) { throw new IllegalStateException("Quitting, can not wait on connection"); }
        if (connected) { return; }
        try {
            synchronized(connectsignal) { connectsignal.wait(milliseconds); }                
        } catch (InterruptedException e) {}
        if (connected) { return; }
        throw new IllegalStateException("Still not connected after timeout");
    }
    
    private void setup(String firstname,String lastname,String password,String loginlocation) {
        // test that method names are preserved
        try {
            String argname=this.getClass().getDeclaredMethod("setup",String.class,String.class,String.class,String.class).getParameters()[0].getName();
            if ("arg0".equals(argname) || (!"firstname".equals(argname))) {
                System.out.println("===== FATAL ERROR =====");
                System.out.println("The name of the first method for setup() is "+argname);
                System.out.println("In the source this is called 'firstname'");
                System.out.println("JSLBot uses reflection to read parameter names to implement bot commands");
                System.out.println("It is necessary to include parameter names in the compiled bytecode");
                System.out.println("This is generally done by compiling (javac) with the '-g' and '-parameters' option to enable debug info");
                System.out.println("Command functionality will not work without this feature, the bot has been aborted for safety reasons");
                System.out.println("NetBeans note: Add to compiler additional options and DISABLE compile on save which ignores these settings");
                throw new AssertionError("Invalid JSLBot compilation, expected argument name 'newbrain', got '"+argname+"'");
            }
        }
        catch (@Nonnull NoSuchMethodException|SecurityException ex) {
            throw new AssertionError("Unable to read signature of setup method??",ex);
        }
        
        jslinterface=new JSLInterface(this);
        LLCATruster.initialise(); // probably compromises the SSL engine in various ways :(
        this.firstname=firstname;
        this.lastname=lastname;
        this.password=password;
        this.loginlocation=loginlocation;
        log.config(Constants.getVersion());
    }


    @Nonnull
    public Handler getHandler(String name) { return brain.getHandler(name); }
    
    /** Launch bot AI.
     * you can run this with Thread.start() if you want to run lots of bots
     * if you just want to run this one bot, you can yield your main thread into here by calling JSLBot.run();
     */
    @Override
    public void run() {
        log.info("JSLBot launching connection...");        
        setName("JSLBot Brain for "+firstname+" "+lastname);
        if (registershutdownhook) { Runtime.getRuntime().addShutdownHook(new ShutdownHook(this)); }
        // catch and report.  "mainLoop()" should guard against everything its self so this means that function is broken
        // nominated for 'best line of code, 2016'
        // ^^ nominated for most useul comment for dating my intermittent work on this project, now mid 2018.
        if (brain.isEmpty()) { log.warning("Bot has no brain and will be a virtual zombie."); }
        reconnect=true;
        while (ALWAYS_RECONNECT || reconnect) {
            quit=false; quitreason=""; connected=false; primary=null;
            reconnect=ALWAYS_RECONNECT;
            circuits.clear();
            brain.prepare();
            try { mainLoop(); }
            catch (Exception e) { log.log(SEVERE,"Main bot loop crashed - "+e.toString(),e); }
            connected=false; shutdown("Exited");
            brain.loginLoopSafety();
        }
    }

    public Logger getLogger(String subspace) {
        return Logger.getLogger(log.getName()+"."+subspace);
    }

    private static class ShutdownHook extends Thread {
        final JSLBot bot;
        ShutdownHook(JSLBot bot) {this.bot=bot;}
        @Override
        public void run() {bot.shutdown("JVM called shutdown hook (program terminated?)");}
    }
    
    // Wrapper for logging in, implements retries and backoff.
    private void performLogin(String firstname, String lastname, @Nonnull String password, String location) throws Exception {
        Exception last=null;
        for (int retries=0;retries<Constants.MAX_RETRIES;retries++) {
            try { login(firstname,lastname,password,location); return; }
            catch (@Nonnull RuntimeException | IOException | XmlRpcException e) {
                last=e;
                long delay = Constants.RETRY_INTERVAL*retries;
                if (delay>Constants.MAX_RETRY_INTERVAL) { delay=Constants.MAX_RETRY_INTERVAL; }
                if (e instanceof NullPointerException) { log.log(SEVERE,"Unexpected null pointer exception during login",e); }
                else { log.info("Login attempt "+(retries+1)+"/"+Constants.MAX_RETRIES+" failed: "+e.getClass().getSimpleName()+":"+e.getMessage()); }
                try { if (!quit) { Thread.sleep(delay); } } catch (InterruptedException f) {}
            }
            if (quit) { return; }
        }
        log.severe("All login attempts failed!");
        throw new IOException("Failed login",last);
    }
    
    
    
    
    
    
    
    // ********** LOGIN CODE / BOT PRIMITIVES **********
    
    // Perform a login attempt
    private void login(String firstname, String lastname, @Nonnull String password, String loginlocation) throws IOException, XmlRpcException  {
        // authentication is performed over XMLRPC over HTTPS
        Map<Object, Object> result = BotUtils.loginXMLRPC(this, firstname, lastname, password, loginlocation);
        if (!("true".equalsIgnoreCase((String)result.get("login")))) {
            throw new IOException("Server gave error: "+ result.get("message"));
        }
        String message=(String)result.get("message");
        log.info("Login MOTD: "+message);

        // the response contains things we'll need
        String fn=(String)result.get("first_name");
        this.firstname=fn.substring(1,fn.length()-1);
        this.lastname=(String)result.get("last_name");
        uuid=new LLUUID((String)result.get("agent_id"));
        // probably want to note the "udp_blacklist" which is a comma separated list of packet types to not use.  but then if we just aren't using them either...?
        circuitcode=(int)result.get("circuit_code");
        sessionid=new LLUUID((String)result.get("session_id"));
        String ip=(String)result.get("sim_ip");
        int port=(Integer)result.get("sim_port");
        int loginx=(Integer)result.get("region_x");
        int loginy=(Integer)result.get("region_y");
        Object[] inventoryrootarray=(Object[]) result.get("inventory-root");
        @SuppressWarnings("unchecked") // if it isn't, what do we do anyway?
        Map<String,String> rootmap=(Map<String,String>) inventoryrootarray[0];
        for (Map.Entry<String, String> entry : rootmap.entrySet()) {
            if (Debug.AUTH) {
                log.finer("Inventory Root "+ entry.getKey() +" = "+ entry.getValue());
            }
            inventoryroot=new LLUUID(entry.getValue());
        }
        //System.out.println("inventoryroot type is "+inventoryroot.getClass().getName());
        // derive region handle
        U64 handle=new U64();
        handle.value=loginx;
        handle.value=handle.value<<(32);
        handle.value=handle.value | (loginy);     
        if (Debug.AUTH || Debug.REGIONHANDLES) { log.finer("Computed initial handle of "+Long.toUnsignedString(handle.value)); }
        // caps
        String seedcaps=(String)result.get("seed_capability");
        log.info("Login is complete, opening initial circuit.");
        Circuit initial=new Circuit(this, ip, port, handle.value,seedcaps);
        initial.connect();
        // for our main connection, "move in" to the sim, it's expecting us :P
        primary=initial;
        circuits.put(handle.value, initial);
        completeAgentMovement();
        agentUpdate();
        connected=true;
        synchronized(connectsignal) { connectsignal.notifyAll(); } // wake up, sleepers
    }

    /** Send this generally useful message down the primary UDP circuit */
    public void completeAgentMovement() {
        CompleteAgentMovement p=new CompleteAgentMovement();
        p.bagentdata.vagentid=getUUID();
        p.bagentdata.vsessionid=getSession();
        p.bagentdata.vcircuitcode=new U32(getCircuitCode());
        send(p);
    }
    @Nullable
    private Date lastagentupdate=null;
    private float drawdistance=(float) 0.001;
    /** Push an agent update */
    public void forceAgentUpdate() { agentUpdate(true); }
    /** Send an agent update if one has not been sent recently */
    public void agentUpdate() { agentUpdate(false); }
    /** Send this generally useful message down the primary UDP circuit
     * @param force If true, ignore the usual timer-based spam prevention
     */
    private boolean blind=false;
    public void blind() { blind=true; forceAgentUpdate(); }
    public void unblind() { blind=false; forceAgentUpdate(); }
    public void agentUpdate(boolean force) {
        boolean debug=false;
        if (quitting()) {
            return;
        }
        // dont spam too many of these
        if (!force && lastagentupdate!=null) {
            if ((new Date().getTime())-lastagentupdate.getTime()<Constants.AGENT_UPDATE_FREQUENCY_MILLISECONDS) {
                return;
            }
        }
        lastagentupdate=new Date();
        AgentUpdate p=new AgentUpdate();
        p.bagentdata.vagentid=getUUID();
        p.bagentdata.vsessionid=getSession();
        LLVector3 camera = getPos();
        camera.z+=5;
        if (blind) {
            p.bagentdata.vcameracenter= new LLVector3(192,144,402);
            p.bagentdata.vcameraataxis=new LLVector3(0,1,0);
            p.bagentdata.vcameraleftaxis=new LLVector3(-1,0,0);
            p.bagentdata.vcameraupaxis=new LLVector3(0,0,1);
            p.bagentdata.vfar=new F32((float)0.001);
        } else {
            p.bagentdata.vcameracenter=camera;
            p.bagentdata.vcameraataxis=new LLVector3(0,1,0);
            p.bagentdata.vcameraleftaxis=new LLVector3(-1,0,0);
            p.bagentdata.vcameraupaxis=new LLVector3(0,0,1);
            p.bagentdata.vfar=new F32(drawdistance);
        }
        // FIXME CHECK THIS
        /*if (Math.random()>0.5) { p.bagentdata.vcontrolflags=new U32(1<<26); }
        p.bagentdata.vbodyrotation.x=(float) (Math.random()*Math.PI*2);
        p.bagentdata.vbodyrotation.y=(float) (Math.random()*Math.PI*2);
        p.bagentdata.vbodyrotation.z=(float) (Math.random()*Math.PI*2);
        p.bagentdata.vheadrotation.x=(float) (Math.random()*Math.PI*2);
        p.bagentdata.vheadrotation.y=(float) (Math.random()*Math.PI*2);
        p.bagentdata.vheadrotation.z=(float) (Math.random()*Math.PI*2); 
        p.bagentdata.vcameraataxis.x=1;
        p.bagentdata.vcameraleftaxis.y=1;
        p.bagentdata.vcameraupaxis.z=y;*/
        send(p);
        //debug("Agent Updated");
    }

    /** Send this generally useful message down the primary UDP circuit */
    @Nonnull
    Packet useCircuitCode() {
        UseCircuitCode cc=new UseCircuitCode();
        cc.bcircuitcode.vcode=new U32(getCircuitCode());
        cc.bcircuitcode.vsessionid=getSession();
        cc.bcircuitcode.vid=getUUID();
        Packet p=new Packet(cc);
        p.setReliable(true);
        return p;
    }


    /** Send an instant message immediately using the primary circuit
     * @param uuid UUID of agent to send message to
     * @param message Message to send
     */
    public void im(LLUUID uuid, @Nonnull String message) {
        ImprovedInstantMessage reply=new ImprovedInstantMessage(this);
        reply.bmessageblock.vtoagentid=uuid;
        reply.bmessageblock.vmessage=new Variable2(message);
        send(reply,true);
    }    

    
    
    // ********** TRANSMISSION PRIMITIVES **********
    /** Primary circuit, as in where the agent presence *is* */
    @Nullable
    Circuit primary=null;  @Nullable
	Circuit circuit() { return primary; }
    
    /** Get the name of the region the avatar is present in.
     *
     * @return Region name
     */
    public String getRegionName() { return circuit().getRegionName(); }
    
    /** Send a packet.
     *
     * @param p Packet to send
     */
    public void send(Packet p) {
        if (primary==null) { throw new IllegalStateException("Primary circuit is not defined or connected"); }
        primary.send(p);
    }

    /** Send a Message, optionally reliably
     *
     * @param m Message to send
     * @param reliable If set, use reliable mode
     */
    public void send(Message m,boolean reliable) {
        Packet p=new Packet(m);
        p.setReliable(reliable);
        send(p);
    }
    /** Send a message, without reliable flag.
     * 
     * @param m Message to send.
     */
    public void send(Message m) {send(m,false); }
    /** Change the primary circuit.
     * Mainly used by the Teleporation Handler.
     * @param c The new primary circuit.
     */
    public void setPrimaryCircuit(Circuit c) { primary=c; }
    
    /** Describes a command.
     * Commands must have this annotation. */
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Target(ElementType.METHOD)
    public @interface CmdHelp {

        /** Get the description for this command
         *
         * @return This command's description
         */
        @Nonnull String description();
    }

    /** Describes an argument to a command. */
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Target(ElementType.PARAMETER)
    public @interface ParamHelp {

        /** Description for this parameter
         *
         * @return This parameter's description.
         */
        @Nonnull String description();
    }



    // post login main loop, update + think in a loop, until we're quitting (disconnecting)
    private void mainLoop() throws Exception {
        performLogin(firstname,lastname,password,loginlocation);
        if (!quit) { brain.loggedIn(); }
        while (!quit) {
            //agentUpdate();
            brain.think();
        }
        log.warning("Bot exited: "+quitreason);
    }

    /** Get the primary circuit's CAPS.
     * 
     * @return CAPS object for the avatar's region.
     */
    public CAPS getCAPS() { return primary.getCAPS(); }
    /** Resolve a UUID into a firstname, either via cache or via lookup
     * @param uuid UUID to look up
     * @return  The first name
     */
    @Nullable
    public String getFirstName(@Nonnull LLUUID uuid) {
        if (uuid.equals(new LLUUID())) { return "NOUUID"; }
        if (Global.firstName(uuid)==null) { try { getCAPS().getNames(uuid); } catch (IOException e) { log.log(WARNING,"Failed to lookup agent names",e); } }
        if (Global.firstName(uuid)==null) { Global.firstName(uuid,"???"); }
        return Global.firstName(uuid);
    }
    /** Resolve a UUID into lastname, either via cache or via lookup
     * @param uuid UUID to look up
     * @return The last name
     */
    @Nullable
    public String getLastName(@Nonnull LLUUID uuid) {
        if (uuid.equals(new LLUUID())) { return "NOUUID"; }
        if (Global.lastName(uuid)==null) { try { getCAPS().getNames(uuid); } catch (IOException e) { log.log(WARNING,"Failed to lookup agent names",e); } }
        if (Global.lastName(uuid)==null) { Global.lastName(uuid,"???"); }
        return Global.lastName(uuid);
    }
    /** Resolve a UUID into a username, either via cache or via lookup
     * @param uuid UUID to look up
     * @return  User name
     */
    @Nullable
    public String getUserName(@Nonnull LLUUID uuid) {
        if (uuid.equals(new LLUUID())) { return "NOUUID"; }
        if (Global.userName(uuid)==null) { try { getCAPS().getNames(uuid); } catch (IOException e) { log.log(WARNING,"Failed to lookup agent names",e); } }
        if (Global.userName(uuid)==null) { Global.userName(uuid,"???"); }
        return Global.userName(uuid);
    }
    /** Resolve a UUID into a displayname, either via cache or via lookup
     * @param uuid UUID to look up
     * @return  Display name
     */
    @Nullable
    public String getDisplayName(@Nonnull LLUUID uuid) {
        if (uuid.equals(new LLUUID())) { return "NOUUID"; }
        if (Global.displayName(uuid)==null) { try { getCAPS().getNames(uuid); } catch (IOException e) { log.log(WARNING,"Failed to lookup agent names",e); } }
        if (Global.displayName(uuid)==null) { Global.displayName(uuid,"???"); }
        return Global.displayName(uuid);
    }


    private final Map<Long,Circuit> circuits=new HashMap<>();

    /** Obtain a circuit to the target.
     * If a live circuit already exists for this handle, that is returned, otherwise a new circuit is created and started.
     * Note the target sim must be expecting us.
     * @param numericip Sim IP address
     * @param port Sim IP port
     * @param handle Region handle
     * @param capsurl CAPS url for target region, potentially null for child agents which get this later.
     * @return Activated circuit for requested region handle
     * @throws IOException If the circuit fails to connect.
     */
    public Circuit createCircuit(String numericip, int port, long handle, String capsurl) throws IOException {
        synchronized(circuits) {
            if (circuits.containsKey(handle)) {
                if (circuits.get(handle).isAlive()) {
                    // already got a circuit
                    if (Debug.CIRCUIT) { log.fine("Duplicate circuit to "+handle+" ignored"); }
                    return circuits.get(handle);
                }
            }
            if (Debug.CIRCUIT) { log.fine("New circuit to "+handle); }
            Circuit newcircuit=new Circuit(this, numericip, port, handle, capsurl);
            newcircuit.connect();
            circuits.put(handle,newcircuit);
            return newcircuit;
        }
    }

    /** Inform bot that a circuit closed.
     * If this is our primary (non child agent) circuit we're in trouble and will quit.
     * @param regionhandle Region handle that's closing connection.
     * @param circ Associated circuit, used as a 'check' only
     */
    void deregisterCircuit(Long regionhandle, Circuit circ) {
        synchronized(circuits) {
            Circuit c=circuits.get(regionhandle);
            if (c!=null) { c.close(); }
            if (circ!=c && c!=null) { log.severe("Closing a region handle but the circuit is not the one we have registered"); }
            circuits.remove(regionhandle);
            // dont warn if shutting down
            if (!quit && c==primary) { log.severe("Closure of primary circuit detected, this is fatal?"); shutdown("Primary circuit lost, we have been disconnected?"); }
        }
    }

    /** Get the regional info for the primary region
     * @return  Primary region the avatar is present in
     */
    public Regional getRegional() {
        return primary.regional();
    }

    /** Get regional data for all connected circuit
     * @return  Get all connected regionals
     */
    @Nonnull
    public Set<Regional> getRegionals() {
        Set<Regional> regionalset=new HashSet<>();
        synchronized(circuits) {
            for (Circuit circuit : circuits.values()) {
                //System.out.println("Circuit "+handle);
                regionalset.add(circuit.regional());
            }
        }
        return regionalset;
    }

    /** Get the circuit for a given region handle
     * 
     * @param regionhandle Region handle to query
     * @return Circuit for the region handle, or null
     */
    Circuit getCircuit(Long regionhandle) {
        return circuits.get(regionhandle); 
    }

    /** Get all circuits
     * @return Set of all circuits
     */
    @Nonnull
    public Set<Circuit> getCircuits() {
        synchronized(circuits) {
            return new HashSet<>(circuits.values());
        }
    }

    /** Initiate disconnection from SL
     * @param reason Reason for disconnecting.
     */
    public void shutdown(String reason) {
        connected=false; 
        if (quit) { return; } // do not re-enter
        quit=true; quitreason=reason;
        log.warning("Shutdown requested: "+reason);
        // because we'll get concurrent modification exceptions otherwise, as we close the circuits while iterating.
        Set<Circuit> closeme = new HashSet<>(getCircuits());
        for (Circuit c:closeme) {
            try { c.close(); } catch (Exception e) {}
        }
        brain.stopProcrastinating();  // release the main thread
    }
    private float x=0; private float y=0; private float z=0;
    @Nonnull
    public LLVector3 getPos() { return new LLVector3(x,y,z); }
    public void setPos(float x, float y, float z) { this.x=x; this.y=y; this.z=z; }

    public void setPos(@Nonnull LLVector3 p) { x=p.x; y=p.y; z=p.z; }

    private float lx=0; private float ly=0; private float lz=0;
    public void setLookAt(@Nonnull LLVector3 l) {lx=l.x;ly=l.y;lz=l.z;}

    @Nonnull
    public LLVector3 getLookAt() { return new LLVector3(lx,ly,lz); }
    public void setLookAt(float x,float y,float z) { lx=x; ly=y; lz=z; }

    private int fovgen=0;
    public void setFOV(float angle) {
        AgentFOV fov=new AgentFOV();
        fov.bagentdata.vagentid=getUUID();
        fov.bagentdata.vcircuitcode=new U32(getCircuitCode());
        fov.bagentdata.vsessionid=getSession();
        fov.bfovblock.vgencounter=new U32(fovgen++);
        fov.bfovblock.vverticalangle=new F32(angle);
        send(fov,true);
    }
    public void setMaxFOV() { setFOV((float) (Math.PI)); }
    public void setMinFOV() { setFOV((float) 0.01); }
    public float drawDistance() { return drawdistance; }
    public void drawDistance(float newdd) {
        drawdistance=newdd;
        forceAgentUpdate();
    }


    private int circuitsequence=0;
    private final Object circuitsequencelock=new Object();
    int getCircuitSequence() { synchronized(circuitsequencelock) { circuitsequence++; return circuitsequence; } }

    @Nonnull
    @Override
    public String toString() { return this.getFullName(); }
}
