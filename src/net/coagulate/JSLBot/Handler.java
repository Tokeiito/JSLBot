/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.coagulate.JSLBot;

import java.util.Map;
import net.coagulate.JSLBot.Packets.Message;
import net.coagulate.JSLBot.Packets.Packet;

/**
 *
 * @author Iain
 */
public abstract class Handler {
    
    public Handler(Configuration config){}
    @Override
    // name your handler.
    public abstract String toString();
    // register yourself, called BEFORE LOGIN... DO NOT DO SL RELATED STUFF HERE
    public abstract void initialise(JSLBot ai) throws Exception;
    // handler for IMMEDIATE mode
    // DO NOT USE OR IMPLEMENT UNLESS YOU ABSOLUTELY HAVE TO, KNOW WHY IT NEEDS TO BE HERE, AND UNDERSTAND WHY THERE IS A BIG CAPS WARNING HERE :P
    // there are explanations of the threading model in JSLBot.java, go read those, if you still dont get it, seek advice
    // messing up processImmediate will do horrible things to your bot, like cause it to get disconnected, or just timeout/deadlock
    public abstract void processImmediate(Event p) throws Exception;
    // handler for queued mode
    public abstract void process(Event p) throws Exception;
    // handler for commands
    public abstract String execute(String command,Map<String,String> parameters) throws Exception;
    // we've logged in, if you want to issue a bunch of lousy queries, this is probably the place.
    public abstract void loggedIn() throws Exception;
    public abstract String help(String command);
    
}
