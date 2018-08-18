package net.coagulate.JSLBot;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.coagulate.JSLBot.Handlers.Authorisation.Authorisation;
import net.coagulate.JSLBot.JSLBot.CmdHelp;

/** Bot's brain.
 * This used to be a Set of Handler's in JSLBot its self but it comes with a lot of polluting Reflection code so it got moved here.
 * Note we get called by a lot of different threads, understanding "immediate" and "delayed" execution and the importance in terms of
 * both locking the bot from doing two things at once, as well as *NOT* locking important threads that have to deal with e.g. Circuit UDP traffic.
 * 
 * Getting these things wrong will likely result in lag, packet storms during catch up, events not arriving, failing synchronous-cross-thread executions and so on.
 * 
 * @author Iain Price
 */
public class Brain {
    private Authorisation auth=null;    
    private final Set<Handler> brain;
    private final JSLBot bot;
    private boolean procrastinate=true;

    public Brain(JSLBot bot) {
        this.bot=bot;
        this.brain = new HashSet<>();
    }
    boolean isEmpty() {return brain.isEmpty();}

    void prepare() {
        queue.clear();
        handlermap.clear();
        commandmap.clear();
        populateCommandMap();
    }
    

    void loadHandlers(String[] handlers) {
        for (String handler:handlers) {
            loadHandler(handler);
        }
    }

    private void loadHandler(String handlername){
        if (handlername==null) { throw new NullPointerException("No handler specified"); }
        try {
            Handler h=createHandler(handlername);
            brain.add(h);
        } catch (InvocationTargetException ex) {
            Throwable t=ex;
            if (ex.getCause()!=null) { t=ex.getCause(); }
            Log.error(bot,"Exception loading handler "+handlername,t);
        }
    }
     
    private void populateCommandMap() {
        final boolean debug=false;
        for (Handler h:brain) {
            if (debug) { System.out.println(h.getClass().getName()); }
            for (Method m:h.getClass().getMethods()) {
                if (debug) { System.out.println(h.getClass().getName()+"."+m.getName()); }
                if (m.getAnnotation(CmdHelp.class)!=null) {
                    if (debug) { System.out.println(h.getClass().getName()+"."+m.getName()+" IS ANNOTATED"); }
                    String commandname=m.getName().toLowerCase();
                    if (debug) { System.out.println(h.getClass().getName()+"."+m.getName()+" Entered into command bank as '"+commandname+"'"); }
                    if (commandmap.containsKey(commandname)) {
                        Log.error(bot,"Duplicate definition for command "+commandname);
                    } else {
                        commandmap.put(commandname,m);
                    }
                }
            }
        }
    }
    public Method getCommand(String name) {
        return commandmap.get((name+"command").toLowerCase());
    }
    public Set<String> getCommands() {
        Set<String> ret=new HashSet<>();
        ret.addAll(commandmap.keySet());
        return ret;
    }
    
    private Handler createHandler(String name) throws InvocationTargetException {
        try {
            String classname=name;
            if (!name.contains(".")) { classname="net.coagulate.JSLBot.Handlers."+name; }
            Class c=Class.forName(classname);
            Configuration subconfiguration=bot.config.subspace(name);
            Constructor cons=c.getConstructor(JSLBot.class,Configuration.class);
            return (Handler) (cons.newInstance(bot,subconfiguration));
        } catch (SecurityException|NoSuchMethodException|ClassNotFoundException|IllegalAccessException|IllegalArgumentException|InstantiationException ex) {
            throw new AssertionError("Handler "+name+" fails to meet programming contract",ex);
        }
    }

    private String formatEventName(Event event) {
        String method=event.getName();
        char c[]=method.toCharArray();
        c[0]=Character.toLowerCase(c[0]);
        return new String(c);
    }
    
    private final Map<String,Set<Method>> handlermap=new HashMap<>();
    
    // brains have an internal queue for delayed processing events.
    private final List <Event> queue=new ArrayList<>();

    // what passes for an API :P
    public String execute(Event event) { return execute(event,true); }
    public void queue(Event event) { synchronized(queue) { queue.add(event); queue.notifyAll(); } }
    
    private final Map<String,Method> commandmap=new HashMap<>();
    
    private final Set<String> warned=new HashSet<>();
    /** Runs the event in THIS thread
     * 
     * @param event
     * @return 
     */
    private String execute(Event event,boolean immediate) {
        String messageid=event.getPrefixedName();
        String fen=formatEventName(event);
        fen=fen+event.typeString();
        String method=fen;
        if (event instanceof UDPEvent || event instanceof XMLEvent) { method+=(immediate?"Immediate":"Delayed"); }
        String response=null;
        if (immediate) { event.status(Event.STATUS.IMMEDIATE); } else { event.status(Event.STATUS.RUNNING); }
        if (!handlermap.containsKey(method)) { populateHandlerMap(event); }
        Set<Method> handlers=null;
        if (event instanceof UDPEvent || event instanceof XMLEvent) { handlers=handlermap.get(method); }
        if (event instanceof CommandEvent ) {
            handlers=new HashSet<>();
            Method handler = commandmap.get(method.toLowerCase());
            if (handler==null) { return "Unknown Command:"+method.toLowerCase(); }
            handlers.add(handler);
        }
        if (Debug.TRACKCOMMANDS && event instanceof CommandEvent) { Log.debug(event,"Entering executor in "+(immediate?"immediate":"delayed")+" mode"); }
        if (handlers==null) { Log.crit(this,"Found a null map for "+method+", but this should have been populated"); return""; }
        for (Method handler:handlers) {
            try {
                Object callon=findHandler(handler);
                if (event instanceof UDPEvent) { response=(String) handler.invoke(callon,(UDPEvent)event); }
                if (event instanceof XMLEvent) { response=(String) handler.invoke(callon,(XMLEvent)event); }
                if (event instanceof CommandEvent) {
                    CommandEvent cmd=(CommandEvent)event;
                    response=cmd.run(callon,handler);
                    cmd.response(response);
                }
            } catch (IllegalAccessException ex) {
                Log.warn(handler,"Method "+method+" has incorrect access modifier"); // impossible?
            } catch (IllegalArgumentException ex) {
                Log.warn(handler,"Method "+method+" has incorrect parameters"); // impossible?
            } catch (InvocationTargetException ex) {
                Throwable t=ex;
                if (t.getCause()!=null) { t=t.getCause(); }
                Log.error(handler,"Method "+method+" threw an error:", t);
            }
        }
        
        if (event instanceof UDPEvent || event instanceof XMLEvent) {
            if (immediate) { 
                event.status(Event.STATUS.QUEUED);
                queue(event);
                return response;
            }
            if (!warned.contains(fen) &&
                    ( handlermap.get(fen+"Delayed")==null || handlermap.get(fen+"Delayed").isEmpty()) &&
                    ( handlermap.get(fen+"Immediate")==null || handlermap.get(fen+"Immediate").isEmpty()) ) {
                Log.debug(this,"No handler for UDP/XML event "+fen);
                warned.add(fen);
            }
        } 
        event.status(Event.STATUS.COMPLETE);
        return response;
    }
    
    private Object findHandler(Method method) {
        Class c=method.getDeclaringClass();
        for (Handler h:brain) {
            if (h.getClass().equals(c)) { return h; }
        }
        throw new IllegalArgumentException("Could not find declaring class for "+method);
    }

    private void populateHandlerMap(Event event) {
        if (event instanceof UDPEvent || event instanceof XMLEvent) {
            populateHandlerMap(event,"Immediate");
            populateHandlerMap(event,"Delayed");
        }
    }
    
    private void populateHandlerMap(Event event,String suffix) {
        // find all the handlers that have a method like this and accumulate them into a set =)
        String fen=formatEventName(event);
        fen=fen+event.typeString();
        fen=fen+suffix;
        Set<Method> methods=new HashSet<>();
        for (Handler handler:brain) {
            try {
                if (event instanceof UDPEvent) { methods.add(handler.getClass().getMethod(fen, UDPEvent.class)); }
                if (event instanceof XMLEvent) { methods.add(handler.getClass().getMethod(fen, XMLEvent.class)); }
            } catch (NoSuchMethodException ex) {
                // this is OK and probably the default case, not every module implements everything.
            } catch (SecurityException ex) {
                // this is less OK
                Log.warn(handler,"Method "+fen+" is inaccessible, this is probably unintentional");
            }
        }
        /*System.out.print("Populating for '"+fen+"' found: ");
        for (Method m:methods) { System.out.print(m.getName()+" "); }
        System.out.println();*/
        handlermap.put(fen, methods);
    }

    
    void think() {
        Event event=null;
        synchronized(queue) {
            if (queue.isEmpty() && procrastinate) { try { queue.wait(Constants.BRAIN_PROCRASTINATES_FOR_MILLISECONDS); } catch (InterruptedException iex) {} }
            if (!queue.isEmpty()) { event=queue.remove(0); }
        }
        if (event!=null) { execute(event,false); }
    }

    /** Stops the brain procrastinating waiting for events.
     * Basically called during bot shutdown, we will stop the sleep-for-event behaviour, and wake up any sleeping threads.
     * Any trapped threads, or new calls to think() will thus be released immediately (though perhaps after running an event...)
     * 
     * Make sure the bot really is quitting otherwise you'll drive the event queue thread into a CPU consuming tight loop.
     */
    void stopProcrastinating() {
        procrastinate=false;
        synchronized(queue) { queue.notifyAll(); }
    }

    void loggedIn() {
        for (Handler h:brain) {
            try { h.loggedIn(); } catch (Exception e) { Log.error(this,"Handler "+h+" exceptioned handling login",e); }
        }
    }

    // track our launch attempts, ALWAYS_RECONNECT will only permit 5 attempts in 10 minutes...
    private final Date[] launches=new Date[Constants.MAX_LAUNCH_ATTEMPTS];
    void loginLoopSafety() {
        // if we have any null slots then we didn't even launch MAX times yet
        for (int i=0;i<launches.length;i++) {
            if (launches[i]==null) {
                Log.info(bot,"Reconnection Safety: We have not yet launched 5 times");
                launches[i]=new Date(); return;// use slot, return OK
            } 
        }
        // not not any null slots, whats the oldest timer?
        Date oldest=null;
        for (Date d:launches) {
            if (oldest==null) { oldest=d; } 
            else { if (d.before(oldest)) { oldest=d; } }
        }
        if (oldest==null) { throw new AssertionError("How is oldest null at this point?  if null we should have hit 'launched less than 5 times'"); }
        long ago=new Date().getTime()-oldest.getTime();
        int secondsago=(int)(ago/1000f);
        Log.info(bot,"Reconnection Safety: Last 5 login attempts took place over "+secondsago+" seconds");
        if (ago<(Constants.MAX_LAUNCH_ATTEMPTS_WINDOW_SECONDS)) { 
            Log.crit(bot,"Reconnection Safety: This is less than the threshold of "+Constants.MAX_LAUNCH_ATTEMPTS_WINDOW_SECONDS+", tripping safety.");
            loginLoopSafetyViolation();
            return; // if we get here.
        }
        // otherwise, overwrite oldest date with now and continue
        for (int i=0;i<launches.length;i++) {
            if (launches[i]==oldest) { launches[i]=new Date(); return; }
        }
        // should never get here
        throw new AssertionError("An oldest launch time was found in pass #1, but could not be found to be replaced in pass #2");
    }
    private void loginLoopSafetyViolation() {
        // probably need some choices here, sometimes it's probably appropriate to "exit" the class, perhaps via an 'error' of some kind that wont get caught
        // sometimes its probably appropriate to stop the whole system if the bot is critical (system.exit?)
        // sometimes the remainder of the application is more important and it should continue, and we should just sleep, which is what we do for now
        // no configuration here yet, hard coded 15 minute sleep, have fun with that.
        for (int i=15;i>0;i--) {
            Log.crit(bot,"Reconnection Safety: RECONNECTION SAFETY HAS TRIPPED.  THREAD FORCE-SLEEPING FOR "+i+" MINUTES.");
            try { Thread.sleep(60000); } catch (InterruptedException e) {}
        }
        Log.warn(bot,"Reconnection Safety: Reconnection safety tripped, we have slept for 15 minutes, and will now return to attempting connections.");
    }    
    
    
    public void setAuth(Authorisation auth) {
        this.auth=auth;
    }
    
    public String auth(CommandEvent event) {
        return auth.approve(event);
    }
    @Override
    public String toString() { return bot.toString()+"/Brain"; }

    Handler getHandler(String name) {
        for (Handler h:brain) {
            if (h.getClass().getSimpleName().equals(name)) { return h; }
        }
        throw new IllegalArgumentException("No handler called '"+name+"' is loaded");
    }

}
