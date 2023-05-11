package lanSimulation.internals;

public class WorkStation extends Node{

	public WorkStation(byte type, String name) {
		super(type, name);
		
	}
	
	@Override
	public void toXML(StringBuffer buf) {
		buf.append("<workstation>");
		buf.append(name_);
		buf.append("</workstation>");
	}

	@Override
	public void toText(StringBuffer buf)
	{
		buf.append("Workstation ");
		buf.append(name_);
		buf.append(" [Workstation]");
	}
	
}
