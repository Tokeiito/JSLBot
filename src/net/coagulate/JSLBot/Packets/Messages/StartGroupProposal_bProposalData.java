package net.coagulate.JSLBot.Packets.Messages;

import net.coagulate.JSLBot.Packets.Block;
import net.coagulate.JSLBot.Packets.Sequence;
import net.coagulate.JSLBot.Packets.Types.F32;
import net.coagulate.JSLBot.Packets.Types.LLUUID;
import net.coagulate.JSLBot.Packets.Types.S32;
import net.coagulate.JSLBot.Packets.Types.Variable1;

import javax.annotation.Nonnull;

public class StartGroupProposal_bProposalData extends Block {
	@Nonnull
    @Sequence(0)
	public LLUUID vgroupid=new LLUUID();
	@Nonnull
    @Sequence(1)
	public S32 vquorum=new S32();
	@Nonnull
    @Sequence(2)
	public F32 vmajority=new F32();
	@Nonnull
    @Sequence(3)
	public S32 vduration=new S32();
	@Nonnull
    @Sequence(4)
	public Variable1 vproposaltext=new Variable1();
}
