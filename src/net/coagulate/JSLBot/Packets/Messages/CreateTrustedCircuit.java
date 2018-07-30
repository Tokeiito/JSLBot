package net.coagulate.JSLBot.Packets.Messages;
import java.util.*;
import net.coagulate.JSLBot.Packets.*;
import net.coagulate.JSLBot.Packets.Types.*;
public class CreateTrustedCircuit extends Block implements Message {
	public final int getFrequency() { return Frequency.LOW; }
	public final int getId() { return 392; }
	public final String getName() { return "CreateTrustedCircuit"; }
	@Sequence(0)
	public CreateTrustedCircuit_bDataBlock bdatablock=new CreateTrustedCircuit_bDataBlock();
}
