package net.coagulate.JSLBot.Packets.Messages;
import java.util.*;
import net.coagulate.JSLBot.Packets.*;
import net.coagulate.JSLBot.Packets.Types.*;
public class NearestLandingRegionRequest extends Block implements Message {
	public final int getFrequency() { return Frequency.LOW; }
	public final int getId() { return 144; }
	public final String getName() { return "NearestLandingRegionRequest"; }
	@Sequence(0)
	public NearestLandingRegionRequest_bRequestingRegionData brequestingregiondata=new NearestLandingRegionRequest_bRequestingRegionData();
}
