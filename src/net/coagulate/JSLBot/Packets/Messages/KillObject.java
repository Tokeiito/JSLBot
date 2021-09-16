package net.coagulate.JSLBot.Packets.Messages;

import net.coagulate.JSLBot.Packets.Block;
import net.coagulate.JSLBot.Packets.Frequency;
import net.coagulate.JSLBot.Packets.Message;
import net.coagulate.JSLBot.Packets.Sequence;

import javax.annotation.Nonnull;
import java.util.List;

public class KillObject extends Block implements Message {
	public final int getFrequency() { return Frequency.HIGH; }
	public final int getId() { return 16; }
	@Nonnull
    public final String getName() { return "KillObject"; }
	@Sequence(0)
	public List<KillObject_bObjectData> bobjectdata;
}
