package net.coagulate.JSLBot.Packets.Messages;

import net.coagulate.JSLBot.Packets.Block;
import net.coagulate.JSLBot.Packets.Frequency;
import net.coagulate.JSLBot.Packets.Message;
import net.coagulate.JSLBot.Packets.Sequence;

import javax.annotation.Nonnull;
import java.util.List;

public class OfflineNotification extends Block implements Message {
	public final int getFrequency() { return Frequency.LOW; }
	public final int getId() { return 323; }
	@Nonnull
    public final String getName() { return "OfflineNotification"; }
	@Sequence(0)
	public List<OfflineNotification_bAgentBlock> bagentblock;
}
