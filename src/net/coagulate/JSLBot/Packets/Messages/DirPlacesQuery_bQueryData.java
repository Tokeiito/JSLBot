package net.coagulate.JSLBot.Packets.Messages;

import net.coagulate.JSLBot.Packets.Block;
import net.coagulate.JSLBot.Packets.Sequence;
import net.coagulate.JSLBot.Packets.Types.*;

import javax.annotation.Nonnull;

public class DirPlacesQuery_bQueryData extends Block {
	@Nonnull
    @Sequence(0)
	public LLUUID vqueryid=new LLUUID();
	@Nonnull
    @Sequence(1)
	public Variable1 vquerytext=new Variable1();
	@Nonnull
    @Sequence(2)
	public U32 vqueryflags=new U32();
	@Nonnull
    @Sequence(3)
	public S8 vcategory=new S8();
	@Nonnull
    @Sequence(4)
	public Variable1 vsimname=new Variable1();
	@Nonnull
    @Sequence(5)
	public S32 vquerystart=new S32();
}
