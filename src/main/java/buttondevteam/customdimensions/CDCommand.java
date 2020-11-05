package buttondevteam.customdimensions;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CDCommand implements CommandExecutor {
	private final CustomDimensions plugin;

	public CDCommand(CustomDimensions plugin) {this.plugin = plugin;}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		sender.sendMessage("§bLoading custom dimensions...");
		sender.sendMessage("To reload a dimension, specify its name after the command");
		try {
			if (args.length > 0)
				if (!Bukkit.unloadWorld(args[0], true))
					sender.sendMessage("§cFailed to unload world " + args[0]);
			plugin.reloadConfig(); //Reload ignore list
			plugin.load();
		} catch (Exception e) {
			e.printStackTrace();
		}
		sender.sendMessage("§bFinished loading custom dimensions!");
		sender.sendMessage("See the console for more details");
		return true;
	}
}
