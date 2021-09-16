package net.coagulate.JSLBot.Packets.Messages;

import net.coagulate.JSLBot.JSLBot;
import net.coagulate.JSLBot.Packets.Block;
import net.coagulate.JSLBot.Packets.Frequency;
import net.coagulate.JSLBot.Packets.Message;
import net.coagulate.JSLBot.Packets.Sequence;

import javax.annotation.Nonnull;

public class DirClassifiedQuery extends Block implements Message {
	public final int getFrequency() { return Frequency.LOW; }
	public final int getId() { return 39; }
	@Nonnull
    public final String getName() { return "DirClassifiedQuery"; }
	@Nonnull
    @Sequence(0)
	public DirClassifiedQuery_bAgentData bagentdata=new DirClassifiedQuery_bAgentData();
	@Nonnull
    @Sequence(1)
	public DirClassifiedQuery_bQueryData bquerydata=new DirClassifiedQuery_bQueryData();
	public DirClassifiedQuery(){}
	public DirClassifiedQuery(@Nonnull JSLBot bot) {
		bagentdata.vsessionid=bot.getSession();
		bagentdata.vagentid=bot.getUUID();
	}
}
