package net.coagulate.JSLBot.Packets.Messages;

import net.coagulate.JSLBot.Packets.Block;
import net.coagulate.JSLBot.Packets.Frequency;
import net.coagulate.JSLBot.Packets.Message;
import net.coagulate.JSLBot.Packets.Sequence;

import javax.annotation.Nonnull;
import java.util.List;

public class GroupDataUpdate extends Block implements Message {
	public final int getFrequency() { return Frequency.LOW; }
	public final int getId() { return 388; }
	@Nonnull
    public final String getName() { return "GroupDataUpdate"; }
	@Sequence(0)
	public List<GroupDataUpdate_bAgentGroupData> bagentgroupdata;
}
