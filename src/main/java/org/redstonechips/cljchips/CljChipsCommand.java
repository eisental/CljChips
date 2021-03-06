
package org.redstonechips.cljchips;

import clojure.lang.Keyword;
import clojure.lang.Namespace;
import clojure.lang.Symbol;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Scanner;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.redstonechips.RedstoneChips;
import org.redstonechips.command.CommandUtils;
import org.redstonechips.command.RCCommand;
import org.redstonechips.util.Signs;

/**
 *
 * @author taleisenberg
 */
public class CljChipsCommand extends RCCommand {

    public CljChipsCommand() {
        rc = RedstoneChips.inst();
    }
    
    @Override
    public void run(CommandSender cs, Command cmnd, String label, String[] args) {
        if (args.length==0) cljHelp(cs, args);        
        else if ("create".startsWith(args[0])) {
            Player p = CommandUtils.enforceIsPlayer(cs);            
            cljCreate(p);
            
        } else if ("load".startsWith(args[0])) {
            cljLoad(cs, args);
        }
        
    }    
    
    private void cljCreate(Player p) {
        if (p==null) return;        
        
        Block b = CommandUtils.targetBlock(p);

        if (rc.chipManager().getAllChips().getByStructureBlock(b.getLocation())!=null) {
            error(p, "Target block belongs to an active chip.");
            return;
        } 

        if (!(b.getState() instanceof Sign)) {
            error(p, "Target block is not a sign.");
            return;
        }

        Sign sign = (Sign)b.getState();
        String[] signArgs = Signs.readArgsFromSign(sign);
        if (signArgs.length==0) {
            error(p, "Clojure circuit name is missing from the sign.");
            return;
        }

        String name = signArgs[0];                
        try {
            createNewCljFile(p, name);
            rc.chipManager().maybeCreateAndActivateChip(b, p, 0);
        } catch (IllegalArgumentException|IOException e) {
            error(p, e.getMessage());
        }

    }
    
    private void cljLoad(CommandSender cs, String[] args) {
        if (args.length>=2) {
            try {
                Symbol symNs = Symbol.intern(null, args[1]);
                CljChips.require.invoke(symNs, Keyword.intern(null, "reload"));
                Namespace ns = (Namespace)CljChips.find_ns.invoke(symNs);

                if (ns!=null && ns.meta()!=null) {
                    String doc = CommandUtils.colorize((String)ns.meta().valAt(Keyword.intern(null, "doc")), ChatColor.GOLD);
                    info(cs, doc);
                }
                info(cs, "Loaded " + args[1] + ".");
            } catch (Exception ex) {
                error(cs, ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void cljHelp(CommandSender cs, String[] args) {
        
    }
    
    public void createNewCljFile(Player p, String name) throws IllegalArgumentException, IOException {
        if (!CljChips.isValidCljName(name)) 
            throw new IllegalArgumentException("`" + name + "` is not a valid clj chip name.");

        String filename = name.replaceAll("\\.", File.separator);
        File f = new File(CljChips.folder, filename + ".clj");
        if (f.exists()) throw new IllegalArgumentException(f.getName() + " already exists.");            
        else {
            String code = defaultClj(name);
            f.getParentFile().mkdirs();
            try (Writer out = new OutputStreamWriter(new FileOutputStream(f))) { 
                out.write(code);
            }                
        }
    }
    
    private static final String defaultCljFile = "/defaultclj";
    
    public static String defaultClj(String name) throws IOException {
        URL u = CljChips.class.getResource(defaultCljFile);
        InputStream stream = u.openStream();
        
        StringBuilder scriptBuilder = new StringBuilder();
        String nl = System.getProperty("line.separator");
        try (Scanner scanner = new Scanner(stream)) {
            while (scanner.hasNextLine()) {
                scriptBuilder.append(scanner.nextLine());
                scriptBuilder.append(nl);
            }
        }
        
        String text = scriptBuilder.toString();
        text = text.replaceAll("<NAME>", name);
        
        return text;
    }    
}
