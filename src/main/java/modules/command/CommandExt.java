package modules.command;

import bot.Constants;
import bot.Main;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import jdk.nashorn.internal.runtime.regexp.joni.exception.SyntaxException;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;

import java.util.*;

abstract class CommandExt extends Command {
	
	static final Category FUN = new Category("Fun");
	static final Category MOD = new Category("Moderation");
	static final Category UTIL = new Category("Utilities");
	
	protected String description;
	protected String usage;
	protected String example;
	protected long[] channels = new long[] {Constants.COFFEE, Constants.BOT_SPAM};
	
	private void validateArgs () {
		Deque<String> check = new ArrayDeque<>();
		
		for (String par : usage.split("")) {
			String message = "Invalid argument syntax at " + this.name;
			
			switch (par) {
				case "<":
				case "[":
				case "{":
					check.push(par);
					break;
				case ">":
					if (Objects.requireNonNull(check.peek()).equals("<"))
						check.pop();
					else
						throw new SyntaxException(message);
					break;
				case "]":
					if (Objects.requireNonNull(check.peek()).equals("]"))
						check.pop();
					else
						throw new SyntaxException(message);
					break;
				case "}":
					if (Objects.requireNonNull(check.peek()).equals("}"))
						check.pop();
					else
						throw new SyntaxException(message);
					break;
				default:
					break;
			}
		}
	}
	
	private int isArgs () {
		boolean req = usage.contains("<");
		boolean opt = usage.contains("[");
		if (!req && !opt)
			return 0;
		if (opt && !req)
			return 1;
		if (!opt)
			return 2;
		if (usage.indexOf('<') < usage.indexOf('['))
			return 3;
		if (usage.indexOf('<') > usage.indexOf('['))
			return 4;
		return -1;
	}
	
	private String getAvailableChannels () {
		StringBuilder message = new StringBuilder();
		
		for (long ch : channels)
			message.append(Main.getChannel(ch).getAsMention()).append(" ");
		
		return message.toString();
	}
	
	private boolean preValidate (CommandEvent event) {
		if (event.getAuthor().isBot() || event.getAuthor().isFake())
			return false;
		
		List<Role> roles = event.getMember().getRoles();
		Guild guild = event.getGuild();
		MessageChannel channel = event.getChannel();
		int length = event.getMessage().getContentRaw().split(" ").length;
		int args = isArgs();
		
		if (args == 0) {
			if (length > 1) {
				event.replyError("This command doesn't require arguments");
				return false;
			}
		} else if ((args == 2 || args == 3) && length <= 1) {
			event.replyError("This command requires arguments");
			return false;
		}
		
		if ((channels != null && guildOnly) && (!contains(channels, channel.getIdLong()) &&
		                                        !roles.contains(guild.getRoleById(Constants.LEADER)) &&
		                                        !roles.contains(guild.getRoleById(Constants.CHIEF)))) {
			event.getMessage().delete().queue();
			channel.sendMessage("You cannot use this command here!\n" + "Available channels: " + getAvailableChannels())
			       .queue(message -> new Timer().schedule(new TimerTask() {
				       @Override
				       public void run () {
					       message.delete().queue();
				       }
			       }, Constants.DELETION_DELAY));
			return false;
		}
		return true;
	}
	
	private boolean contains (long[] channels, long idLong) {
		for (long l : channels) {
			if (l == idLong)
				return true;
		}
		return false;
	}
	
	public boolean validate (CommandEvent event) {
		return preValidate(event);
	}
	
	// TODO: Make CommandExt's implementation instance, not class
}

