
package org.redstonechips.cljchips;

import clojure.lang.Namespace;
import clojure.lang.Symbol;
import clojure.lang.Var;
import java.io.FileNotFoundException;
import org.redstonechips.RCPrefs;
import org.redstonechips.chip.Chip;
import org.redstonechips.chip.ChipListenerImpl;
import org.redstonechips.circuit.Circuit;

/**
 *
 * @author taleisenberg
 */
public class clj extends Circuit {

    @Override
    public void input(boolean state, int inIdx) {
    }

    @Override
    public Circuit init(String[] args) {
        if (args.length==0) return error("First sign argument must be a script name.");                
        
        try {
            Namespace ns = (Namespace)CljChips.load_clj_circuit.invoke(Symbol.intern(null,args[0]));
            Var circuitVar = ns.findInternedVar(Symbol.create("circuit"));
            if (circuitVar==null) return error("Clojure file must contain a var called `circuit`.");

            Object circuitObj = circuitVar.deref();
            if (circuitObj==null || !(circuitObj instanceof Circuit))
                return error("circuit var is nil." + (circuitObj!=null? " Found " + circuitObj.getClass().getCanonicalName() + "." : ""));
            else {                
                Circuit circuit = ((Circuit)circuitObj).constructWith(chip, outWriter, inputlen, outputlen);
                circuit.putMeta("ns", ns.name);
                circuit.putMeta("clj-name", args[0]);
                chip.addListener(new RemoveNSListener());
                info("Running script " + args[0] + " in namespace `" + ns.name.getName() + "`");
                return circuit;
                
                
            }
        } catch (Exception ex) {
            if (ex instanceof FileNotFoundException) {
                return error("Can't load file: " + ex.getMessage() + "\n" + 
                        RCPrefs.getInfoColor() + "Use `/clj create` to create a new file.");
            } else {
                ex.printStackTrace();
                return error(ex.getMessage());
            }
        }
                
        
    }
    
    /**
     * A chip listener for removing the clj circuit namespace on circuit shutdown.
     */
    private static class RemoveNSListener extends ChipListenerImpl {
        @Override
        public void chipShutdown(Chip c) {
            CljChips.remove_ns.invoke((Symbol)c.circuit.getMeta("ns"));
        }
    }
}
