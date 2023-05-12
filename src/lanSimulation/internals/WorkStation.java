package lanSimulation.internals;

public class WorkStation extends Node{

	public WorkStation(String name) {
		super(name);
		
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
