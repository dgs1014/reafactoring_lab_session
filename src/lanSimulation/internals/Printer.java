package lanSimulation.internals;

public class Printer extends Node {

	public Printer(byte type, String name) {
		super(type, name);
		
	}
	
	@Override
	public void toXML(StringBuffer buf) {
		buf.append("<printer>");
		buf.append(name_);
		buf.append("</printer>");
	}

	@Override
	public void toText(StringBuffer buf)
	{
		buf.append("Printer ");
		buf.append(name_);
		buf.append(" [Printer]");
	}	
	
}
