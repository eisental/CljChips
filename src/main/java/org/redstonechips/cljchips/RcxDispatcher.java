
package org.redstonechips.cljchips;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 *
 * @author taleisenberg
 */
public class RcxDispatcher implements CommandExecutor {
    public static RcxCommand command;
    
    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String label, String[] args) {
        if (command!=null) {
            command.execute(cs, args);
            return true;
        } else {
            cs.sendMessage(ChatColor.RED + "/rcx doesn't have a registered command yet.");
            return false;
        }
    }
}
