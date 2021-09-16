package net.coagulate.JSLBot.Packets.Messages;

import net.coagulate.JSLBot.Packets.Block;
import net.coagulate.JSLBot.Packets.Sequence;
import net.coagulate.JSLBot.Packets.Types.LLUUID;

import javax.annotation.Nonnull;

public class RpcChannelReply_bDataBlock extends Block {
	@Nonnull
    @Sequence(0)
	public LLUUID vtaskid=new LLUUID();
	@Nonnull
    @Sequence(1)
	public LLUUID vitemid=new LLUUID();
	@Nonnull
    @Sequence(2)
	public LLUUID vchannelid=new LLUUID();
}
