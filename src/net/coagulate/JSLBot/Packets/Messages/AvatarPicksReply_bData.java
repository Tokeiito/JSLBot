package net.coagulate.JSLBot.Packets.Messages;

import net.coagulate.JSLBot.Packets.Block;
import net.coagulate.JSLBot.Packets.Sequence;
import net.coagulate.JSLBot.Packets.Types.LLUUID;
import net.coagulate.JSLBot.Packets.Types.Variable1;

import javax.annotation.Nonnull;

public class AvatarPicksReply_bData extends Block {
	@Nonnull
    @Sequence(0)
	public LLUUID vpickid=new LLUUID();
	@Nonnull
    @Sequence(1)
	public Variable1 vpickname=new Variable1();
}
