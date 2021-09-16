package net.coagulate.JSLBot.Packets.Messages;

import net.coagulate.JSLBot.JSLBot;
import net.coagulate.JSLBot.Packets.Block;
import net.coagulate.JSLBot.Packets.Frequency;
import net.coagulate.JSLBot.Packets.Message;
import net.coagulate.JSLBot.Packets.Sequence;

import javax.annotation.Nonnull;
import java.util.List;

public class AvatarClassifiedReply extends Block implements Message {
	public final int getFrequency() { return Frequency.LOW; }
	public final int getId() { return 42; }
	@Nonnull
    public final String getName() { return "AvatarClassifiedReply"; }
	@Nonnull
    @Sequence(0)
	public AvatarClassifiedReply_bAgentData bagentdata=new AvatarClassifiedReply_bAgentData();
	@Sequence(1)
	public List<AvatarClassifiedReply_bData> bdata;
	public AvatarClassifiedReply(){}
	public AvatarClassifiedReply(@Nonnull JSLBot bot) {
		bagentdata.vagentid=bot.getUUID();
	}
}
