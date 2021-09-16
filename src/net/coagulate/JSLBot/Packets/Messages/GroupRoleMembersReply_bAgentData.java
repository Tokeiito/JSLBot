package net.coagulate.JSLBot.Packets.Messages;

import net.coagulate.JSLBot.Packets.Block;
import net.coagulate.JSLBot.Packets.Sequence;
import net.coagulate.JSLBot.Packets.Types.LLUUID;
import net.coagulate.JSLBot.Packets.Types.U32;

import javax.annotation.Nonnull;

public class GroupRoleMembersReply_bAgentData extends Block {
	@Sequence(0)
	public LLUUID vagentid=new LLUUID();
	@Nonnull
    @Sequence(1)
	public LLUUID vgroupid=new LLUUID();
	@Nonnull
    @Sequence(2)
	public LLUUID vrequestid=new LLUUID();
	@Nonnull
    @Sequence(3)
	public U32 vtotalpairs=new U32();
}
