package me.aamadeo.aol.on;

import me.aamadeo.aol.optimization.Solution;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name="Network")
public class Network {
	
	public static final double COST_MUXDEMUX_PORT = 0.3;
	public static final double COST_OSW_PORT = 0.5;
	public static final double COST_TSI = 0.3;
	public static final double COST_WLCONVERSION = 0.75;
	public static final double COST_KM = 0.1;
	

	@OneToMany(cascade=CascadeType.ALL)
	private Set<Node> nodes = new HashSet<Node>();
	
	@OneToMany(cascade=CascadeType.ALL)
	private Set<Link> links = new HashSet<Link>();
	
	@Id 
	@GeneratedValue 
	private int id; 
	
	private String name;
	
	/*Number of fibers per link*/
	private int F;
	
	/*Number of wavebands per fiber*/
	private int B;
	
	/*Number of wavelengths per waveband*/
	private int W;
	
	/*Number of timslots per wavelength*/
	private int T;

	public Network(){
		nodes.clear();
		links.clear();
	}
	
	public Network clone(){
		Network clone = new Network();
		
		HashMap<String,Node> nodesMap = new HashMap<String,Node>();
		
		clone.id = this.id;
		clone.F = this.F;
		clone.B = this.B;
		clone.W = this.W;
		clone.T = this.T;
		clone.name = this.name;
		clone.nodes = new HashSet<Node>();
		clone.links = new HashSet<Link>();
		
		for (Node node : nodes){
			Node newNode = node.clone();
			nodesMap.put(newNode.getLabel(), newNode);
			clone.nodes.add(newNode);
		}
		
		for (Link link : links){
			clone.links.add(link.clone(nodesMap,this));
		}
		
		return clone;
	}

	public boolean nodeExists(Node node) {
		return nodes.contains(node);
	}

	public boolean addNode(Node value) {
		return nodes.add(value);
	}

	public boolean removeNode(Node node) {
		if ( node != null ){
			node.breakAllLinks(this);
		}

		return nodes.remove(node);
	}

	public int cantidadNodos() {
		return nodes.size();
	}

	public Link addLink(Node a, Node b, int cost) {
		Link c = new Link(a,b,this);
		c.setCost(cost);
		links.add(c);
		
		return c; 
	}

	public boolean linkExists(Link link) {
		return links.contains(link);
	}

	public boolean removeLink(Link link) {
		return links.remove(link);
	}

	public int linksCount() {
		return links.size();
	}

	public Set<Node> getNodes() {
		return nodes;
	}

	public void setNodes(Set<Node> nodes) {
		this.nodes = nodes;
	}

	public Set<Link> getLinks() {
		return links;
	}

	public void setLinks(Set<Link> links) {
		this.links = links;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public int getF() {
		return F;
	}

	public void setF(int F) {
		this.F = F;
	}
	
	public int getB() {
		return B;
	}

	public void setB(int B) {
		this.B = B;
	}

	public int getW() {
		return W;
	}

	public void setW(int W) {
		this.W = W;
	}

	public int getT() {
		return T;
	}

	public void setT(int T) {
		this.T = T;
	}
	
	public int getBandWidth(){
		return B*T*W;
	}

	
	public Node randomNode(){
		double i = Math.random()*((double) nodes.size());
		int j = 1;
		
		Iterator<Node> iter = nodes.iterator();
		
		while(j < i){
			iter.next();
			j++;
		}
		
		return iter.next();
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public void initialize(){
		
		for(Link link: links){
			link.initialize();
			
			link.getOrigin().addLinkID(link);
			link.getDestination().addLinkID(link);	
		}
		
		for(Node node : nodes){
			node.initialize();
		}
	}
	
	public void initialize(Solution solution){
		initialize();
		
		for(Service service : solution.getServices()){
			service.commitLightLinks();
		}
	}
	
	/**
	 * Creates an image of the network showing the usage of the links. Where :
	 * a) Greens shows low usage of the link (usage <=33%)
	 * b) Blue shows medium usage of the link (33% < usage <= 66%)
	 * c) Red shows high usage of the link(66% < usage < 100%)
	 * d) Black shows full usage of the link (usage = 100%)
	 *
	 * 
	 * @param outputDirectory	Output directory.
	 * @param graphID			Cadena utilizada para diferenciar a varias imagenes el mismo grafo
	 */
	public void usage(String outputDirectory, String graphID){
		String graphName = this.name + "_usage" + graphID;
		String fileName = graphName + ".gv";
		String cmd = "\"C:\\Program Files (x86)\\Graphviz 2.28\\bin\\dot.exe\"";
		
		cmd += " -Ksfdp -Goverlap=prism -Tpng -o \"" + outputDirectory + "\\" + graphName + ".png\" \"";
		cmd += outputDirectory + "\\" + fileName + "\"";
		
		try {
			FileWriter fw = new FileWriter(new File(outputDirectory+"/"+fileName));
			
			fw.write("graph " + graphName + " {\n");
			
			for(Node node : nodes){
				String spec = " [penwidth=1 " + (node.isLocked() ? ", style=filled, fillcolor=\"#000000\")" : "") +  " ];\n";
				fw.write(node + spec);
			}
			
			for(Link link: links){
				int usage = link.getUsage();
				int penwidth = 1 + usage / 15;
				
				String spec = "[penwidth="+penwidth+", weight=2";
								
				if ( 0 == usage){
					spec += ", color=\"#AAAAAA\"];";
				} else if( 0 < usage && usage <= 33){
					spec += ", color=\"#11AA11\"];";
				} else if (33 < usage && usage <= 66){
					spec += ", color=\"#AAAA11\"];";
				} else if (66 < usage && usage < 100){
					spec += ", color=\"#AA1111\"];";
				} else {
					spec += ", color=\"#000000\"];";
				}
				
				fw.write(link.getOrigin() + " -- " + link.getDestination() + spec + "\n");
			}
			
			fw.write("}");
			fw.flush();
			fw.close();
			
			Process p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
			
			File dotFile = new File(outputDirectory+"\\"+fileName);
			dotFile.delete();
		} catch (IOException e){
			e.printStackTrace();
			System.exit(1);
		} catch (InterruptedException ie){
			ie.printStackTrace();
			System.exit(1);
		}
	}
	
	
	/**
	 * Creates an image of the network showing the links used by a Service.
	 * 
	 * @param service			Service
	 * @param outputDirectory	Directorio donde crear la imagen
	 * @param name				Name of the image. Default: Network.name + _ + Service.name.
	 */
	public void drawService(Service service, String outputDirectory, String name){
		String graphName = name == null ? this.name + "_" + service : name;
		String fileName = graphName + ".gv";
		String cmd = "\"C:\\Program Files (x86)\\Graphviz 2.28\\bin\\dot.exe\"";
		cmd += " -Ksfdp -Goverlap=prism -Tpng -o \"" + outputDirectory + "\\" + graphName + ".png\" \"";
		cmd += outputDirectory + "\\" + fileName + "\"";
		
		Node origin = service.getRequest().getOrigin();
		Node destination = service.getRequest().getDestino();
		
		HashSet<Node> nodes = new HashSet<Node>();
		HashSet<Link> links = new HashSet<Link>();
		
		if(service.getPath() != null){
			Node current = service.getPath().getOrigin();
			nodes.add(current);

			for(Hop hop : service.getPath().getHops()){
				Link link = hop.getLink();
				current = link.getDestination();
							
				nodes.add(current);
				links.add(link);
			}
		}
		
		try {
			FileWriter fw = new FileWriter(new File(outputDirectory+"\\"+fileName));
			
			fw.write("graph " + graphName + " {\n");
			
			for(Node node : this.nodes){
				String spec = " ";
				
				if(origin.equals(node) || destination.equals(node)){
					spec += " [penwidth=3, style=filled, fillcolor=\"#AA1111\"];\n";
				} else if(nodes.contains(node)){
					spec += " [penwidth=3, style=filled, fillcolor=\"#11AA11\"];\n";
				} else {
					spec += "[penwidth=1];\n";
				}
				
				fw.write(node + spec);
			}
			
			for(Link link: this.links){
				String spec = " ";
				
				if(links.contains(link)){
					spec += "[penwidth=3, weight=2, color=\"#11AA11\"";
				} else {
					spec += "[penwidth=1";
				}
				
				spec += ", label=\"" + link.getCost() + "\"];";
				
				fw.write(link.getOrigin() + " -- " + link.getDestination() + spec);
			}
			
			fw.write("}");
			fw.flush();
			fw.close();
			
			Process p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
			
			File dotFile = new File(outputDirectory+"\\"+fileName);
			dotFile.delete();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (InterruptedException ie){
			ie.printStackTrace();
			System.exit(1);
		}
	}
}
