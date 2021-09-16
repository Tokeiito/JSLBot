package net.coagulate.JSLBot.Packets.Messages;

import net.coagulate.JSLBot.Packets.Block;
import net.coagulate.JSLBot.Packets.Sequence;
import net.coagulate.JSLBot.Packets.Types.LLUUID;
import net.coagulate.JSLBot.Packets.Types.U32;

import javax.annotation.Nonnull;

public class RpcChannelRequest_bDataBlock extends Block {
	@Nonnull
    @Sequence(0)
	public U32 vgridx=new U32();
	@Nonnull
    @Sequence(1)
	public U32 vgridy=new U32();
	@Nonnull
    @Sequence(2)
	public LLUUID vtaskid=new LLUUID();
	@Nonnull
    @Sequence(3)
	public LLUUID vitemid=new LLUUID();
}
