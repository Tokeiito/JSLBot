package net.coagulate.JSLBot.Packets.Messages;

import net.coagulate.JSLBot.Packets.Block;
import net.coagulate.JSLBot.Packets.Sequence;
import net.coagulate.JSLBot.Packets.Types.S32;
import net.coagulate.JSLBot.Packets.Types.Variable2;

import javax.annotation.Nonnull;

public class ParcelOverlay_bParcelData extends Block {
	@Nonnull
    @Sequence(0)
	public S32 vsequenceid=new S32();
	@Nonnull
    @Sequence(1)
	public Variable2 vdata=new Variable2();
}
