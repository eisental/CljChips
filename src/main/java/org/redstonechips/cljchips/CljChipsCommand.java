
package org.redstonechips.cljchips;

import clojure.lang.Keyword;
import clojure.lang.Symbol;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Scanner;
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
        if (args.length==0) error(cs, "For help enter `/clj help`");
        else if ("create".startsWith(args[0])) {
            Player p = CommandUtils.enforceIsPlayer(cs);
            if (p==null) return;
            
            Block b = CommandUtils.targetBlock(p);
            
            if (rc.chipManager().getAllChips().getByStructureBlock(b.getLocation())!=null) {
                error(cs, "Target block belongs to an active chip.");
                return;
            } 
            
            if (!(b.getState() instanceof Sign)) {
                error(cs, "Target block is not a sign.");
                return;
            }
            
            Sign sign = (Sign)b.getState();
            String[] signArgs = Signs.readArgsFromSign(sign);
            if (signArgs.length==0) {
                error(cs, "Clojure name is missing from the sign.");
                return;
            }
            
            String name = signArgs[0];                
            try {
                createNewCljFile(p, name);
                rc.chipManager().maybeCreateAndActivateChip(b, cs, 0);
            } catch (IllegalArgumentException|IOException e) {
                error(p, e.getMessage());
            }
            
        } else if ("load".startsWith(args[0])) {
            if (args.length>=2) {
                try {
                    CljChips.require.invoke(Symbol.intern(null, args[1]), Keyword.intern(null, "reload"));
                    info(cs, "Loaded " + args[1] + ".");
                } catch (Exception ex) {
                    error(cs, ex.getMessage());
                }
            }
        }
        
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
