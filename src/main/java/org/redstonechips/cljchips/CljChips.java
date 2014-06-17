package org.redstonechips.cljchips;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.Symbol;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.redstonechips.RedstoneChips;
import org.redstonechips.circuit.CircuitLibrary;
import org.redstonechips.event.EventDispatcher;

/**
 *
 * @author Tal Eisenberg
 */
public class CljChips extends CircuitLibrary {
    public static File folder;
    
    private static CljChips inst;
    public static CljChips inst() { return inst; }
    
    public static IFn load_clj_circuit, require, remove_ns, find_ns;
    
    public static EventDispatcher eventDispatcher = new EventDispatcher();
    
    @Override
    public Class[] getCircuitClasses() {
        return new Class[] { clj.class };
    }

    @Override
    public void onRedstoneChipsEnable(RedstoneChips instance) {        
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        folder = getDataFolder();        
        
        try {
            Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            // run init script
            System.out.println("[CljChips] Initalizing Clojure... (this usually takes some time.)");
            RT.loadResourceScript("cljchips/init.clj");
            
            // keep some function references
            require = Clojure.var("clojure.core", "require");
            remove_ns = Clojure.var("clojure.core", "remove-ns");
            find_ns = Clojure.var("clojure.core", "find-ns");
            
            require.invoke(Symbol.intern(null, "cljchips.factory"));
            load_clj_circuit = Clojure.var("cljchips.factory", "load-clj-circuit");
        } catch (IOException ex) {
            Logger.getLogger(CljChips.class.getName()).log(Level.SEVERE, null, ex);
        }                 
    
    }

    @Override
    public void onLoad() {
        inst = this;
    }
    
    @Override
    public void onEnable() {
        getCommand("cljchips").setExecutor(new CljChipsCommand());
        getCommand("redstonechips-x").setExecutor(new RcxDispatcher());
        
        eventDispatcher.bindToPlugin(this);
    }

    @Override
    public void onDisable() {
        inst = null;
        eventDispatcher.stop();
    }               
    
    public static boolean isValidCljName(String name) {
        return !name.contains(File.separator);
    }
}
