package net.coagulate.JSLBot;

import java.util.Set;

/** General abstract API for the configuration system, K=V style.
 *
 * @author Iain Price
 */
public abstract class Configuration {
    
    /** Create a subspace inside this configuration.
     * By applying a common prefix to all the subspace methods, a hierarchical structure of preferences (in 'folders' if you like) can be constructed.
     * 
     * @param prefix Prefix pre-pended to all future get/put requests.
     * @return A new "configuration" that is subspaced.
     */
    public Configuration subspace(String prefix) {
        return new ConfigurationSubspace(this,prefix);
    }
    /** Get a configuration element, or its default value
     * 
     * @param key Key to get
     * @param defaultvalue Value to return if not set in config
     * @return The value, or the defaultvalue if the value was null.  Only returns null if defaultvalue is null.
     */
    public String get(String key,String defaultvalue) { 
        String value=get(key);
        if (value==null) { return defaultvalue; }
        return value;
    }
    /** Configuration services must have a 'get' command
     * 
     * @param param Key to get
     * @return Value, or null if not present.
     */
    public abstract String get(String param);
    /** Configuration services must have a 'put' command
     * 
     * @param key Key of the KV pair
     * @param value Value of the KV pair
     */
    public abstract void put(String key,String value);    

    /** Get all the keys.
     *
     * @return A set of all the K's
     */
    public abstract Set<String> get();

    /** Debugging / dump all data.
     *
     * @return A string dump of all k=v data.
     */
    public abstract String dump();
    /** For seeking the root of the configuration tree, subspaces will point this upwards
     * 
     * @return The root of the configuration space.
     */
    public Configuration getMaster() { return this; }
}
