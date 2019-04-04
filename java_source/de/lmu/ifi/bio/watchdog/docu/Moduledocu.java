package de.lmu.ifi.bio.watchdog.docu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.Gson;

import de.lmu.ifi.bio.watchdog.helper.XMLBuilder;

/**
 * Class used for documentation of Watchdog modules
 * @author Michael Kluge
 *
 */
public class Moduledocu {
	private final int AUTO_ID;
	private final String NAME;
	private final ArrayList<String> CATEGORIES = new ArrayList<>();
	private final ArrayList<String> AUTHORS = new ArrayList<>();
	private final ArrayList<String> PMID = new ArrayList<>();
	private final ArrayList<String> MAINTAINER = new ArrayList<>();
	private final ArrayList<String> WEBSITE = new ArrayList<>();
	private final String DATE;
	private final String PAPER_DESC;
	private final ArrayList<VersionedInfo<String>> DEPENDENCIES = new ArrayList<>();
	private final ArrayList<VersionedInfo<String>> COMMENTS = new ArrayList<>();
	private final ArrayList<VersionedInfo<String>> DESC = new ArrayList<>();
	private final ArrayList<Paramdocu> PARAMS = new ArrayList<>();
	private final ArrayList<Returndocu> RETURN = new ArrayList<>();
	private final HashSet<Integer> VERSIONS = new HashSet<>();
	private static int currentID;
	
	public static final Gson GSON = new Gson(); 
	
	/**
	 * 
	 * @param name name of the module
	 * @param authors author(s) of the module
	 * @param pmid pubmedID(s) for citing the module
	 * @param paperDesc short description of the module for inclusion in a paper 
	 * @param dependencies external dependencies of the module
	 * @param description description of the modules
	 * @param params parameter of the module
	 */
	public Moduledocu(String name, ArrayList<String> categories, String date, ArrayList<String> authors, ArrayList<String> pmid, ArrayList<String> website, String paperDesc, ArrayList<VersionedInfo<String>> dependencies, ArrayList<VersionedInfo<String>> comments, ArrayList<VersionedInfo<String>> description, HashSet<Integer> versions, ArrayList<Paramdocu> params, ArrayList<Returndocu> returnV, ArrayList<String> maintainer) {
		this.NAME = name;
		this.CATEGORIES.addAll(categories);
		this.AUTHORS.addAll(authors);
		this.PMID.addAll(pmid);
		this.PAPER_DESC = paperDesc;
		this.DEPENDENCIES.addAll(dependencies);
		this.COMMENTS.addAll(comments);
		this.DESC.addAll(description);
		this.MAINTAINER.addAll(maintainer);
		this.VERSIONS.addAll(versions);
		this.DATE = date;
		this.WEBSITE.addAll(website);
		
		this.PARAMS.addAll(params);
		this.RETURN.addAll(returnV);
		
		this.AUTO_ID = currentID++;
	}
	
	/**
	 * 
	 * @param name name of the module
	 * @param authors author(s) of the module
	 * @param pmid pubmedID(s) for citing the module
	 * @param paperDesc short description of the module for inclusion in a paper 
	 * @param dependencies external dependencies of the module
	 * @param description description of the modules
	 * @param params parameter of the module
	 */
	public Moduledocu(String name, ArrayList<String> categories, String date, ArrayList<String> authors, ArrayList<String> pmid, ArrayList<String> website, String paperDesc, ArrayList<VersionedInfo<String>> dependencies, ArrayList<VersionedInfo<String>> comments, ArrayList<VersionedInfo<String>> description, HashSet<Integer> versions, HashMap<String, ArrayList<Paramdocu>> params, HashMap<String, ArrayList<Returndocu>> returnV, ArrayList<String> maintainer) {
		this.NAME = name;
		this.CATEGORIES.addAll(categories);
		this.AUTHORS.addAll(authors);
		this.PMID.addAll(pmid);
		this.PAPER_DESC = paperDesc;
		this.DEPENDENCIES.addAll(dependencies);
		this.COMMENTS.addAll(comments);
		this.DESC.addAll(description);
		this.MAINTAINER.addAll(maintainer);
		this.VERSIONS.addAll(versions);
		this.DATE = date;
		this.WEBSITE.addAll(website);
		
		// resolve hashmap --> if different versions & types then store them as different objects
		for(String key : params.keySet()) {
			for(Paramdocu p : params.get(key))
				this.PARAMS.add(p);
		}
		for(String key : returnV.keySet()) {
			for(Returndocu r : returnV.get(key))
				this.RETURN.add(r);
		}		
		this.AUTO_ID = currentID++;
	}

	public int getAutoID() {
		return this.AUTO_ID;
	}
	public String getName() {
		return this.NAME;
	}
	public ArrayList<String> getAuthorNames() {
		return new ArrayList<>(this.AUTHORS);
	}
	public ArrayList<String> getPMIDs() {
		return new ArrayList<>(this.PMID);
	}
	public ArrayList<String> getWebsite() {
		return new ArrayList<>(this.WEBSITE);
	}
	public ArrayList<VersionedInfo<String>> getDependencies() {
		return this.DEPENDENCIES;
	}
	public ArrayList<VersionedInfo<String>> getComments() {
		return this.COMMENTS;
	}
	public String getPaperDescription() {
		return this.PAPER_DESC;
	}
	public ArrayList<VersionedInfo<String>> getDescription() {
		return this.DESC;
	}
	public Paramdocu getParameter(String name) {
		for(Paramdocu p : this.PARAMS) {
			if(p.NAME.equals(name)) return p;
		}
		return null;
	}
	public Returndocu getReturnValue(String name) {
		for(Returndocu p : this.RETURN) {
			if(p.NAME.equals(name)) return p;
		}
		return null;
	}
	public ArrayList<Paramdocu> getParameter() {
		return new ArrayList<>(this.PARAMS);
	}
	public ArrayList<Returndocu> getReturnValues() {
		return new ArrayList<>(this.RETURN);
	}
	public ArrayList<String> getMaintainerUsernames() {
		return new ArrayList<>(this.MAINTAINER);
	}
	public Integer getMaxVersion() {
		return this.VERSIONS.stream().reduce(Integer::max).orElse(1);
	}
	public Integer getMinVersion() {
		return this.VERSIONS.stream().reduce(Integer::min).orElse(1);
	}
	public String getDate() {
		return this.DATE;
	}
	public ArrayList<String> getCategories() {
		return new ArrayList<>(this.CATEGORIES);
	}
	
	public String getJsonInfo() {
		// collect the info we want to export to the website for the search
		HashMap<String, Object> export = new HashMap<>();
		export.put(DocuXMLParser.AUTO_ID, Integer.toString((this.getAutoID())));
		export.put(DocuXMLParser.NAME, this.getName());
		export.put(DocuXMLParser.CATEGORY, this.getCategories());
		export.put(DocuXMLParser.AUTHOR, this.getAuthorNames());
		export.put(DocuXMLParser.DESCRIPTION, this.DESC);
		export.put(DocuXMLParser.DESCRIPTION_SEARCH, this.DESC.stream().map(x -> x.VALUE).collect(Collectors.toCollection(ArrayList<String>::new)));
		return GSON.toJsonTree(export).getAsJsonObject().toString();
	}

	
	public String toXML(boolean isTemplate) {
		XMLBuilder b = new XMLBuilder();
		b.noNewlines(true);
		b.addPlain("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		b.startTag("documentation", false, true, true);		
		b.addQuotedAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		b.addQuotedAttribute("xsi:noNamespaceSchemaLocation", "documentation.xsd");
		b.endOpeningTag(false);
		
		// info section
		b.addComment("mandatory fields: author, category, updated, description");
		b.startTag(DocuXMLParser.INFO, true, true);
		b.endOpeningTag(false);
		b.addComment("forename lastname");
		b.addTags(DocuXMLParser.AUTHOR, this.getAuthorNames());
		b.addComment("day the module was updated the last time");
		b.addTags(DocuXMLParser.UPDATED, this.getDate()); 
		b.addTags(DocuXMLParser.CATEGORY, this.getCategories());
		// add description and dependencies in "version blocks"
		for(VersionedInfo<String> vi : this.DESC)
			b.addTags(DocuXMLParser.DESCRIPTION, Pair.of(vi.MIN_VER, vi.MAX_VER), vi.VALUE);
		
		b.addComment("##### optional #####");
		b.addComment("website of the dependencies used in this module");
		b.addTags(DocuXMLParser.WEBSITE, this.getWebsite());
		b.addComment("short description and PubmedID for the methods section of a manuscript");
		b.addTags(DocuXMLParser.PAPER_DESC, this.getPaperDescription());
		b.addTags(DocuXMLParser.PMID, this.getPMIDs());
		
		b.addComment("external dependencies required for that module");
		for(VersionedInfo<String> vi : this.DEPENDENCIES)
			b.addTags(DocuXMLParser.DEPENDENCIES, Pair.of(vi.MIN_VER, vi.MAX_VER), vi.VALUE);
		
		b.addComment("module specific hints or comments");
		for(VersionedInfo<String> vi : this.COMMENTS)
			b.addTags(DocuXMLParser.COMMENTS, Pair.of(vi.MIN_VER, vi.MAX_VER), vi.VALUE);
		b.endCurrentTag();
		
		// maintainer section
		b.addComment("##### optional #####");
		b.addComment("github usernames of users who should be able to commit changes to that module");
		b.startTag(DocuXMLParser.MAINTAINER, true, true);
		b.endOpeningTag(false);
		b.addTags(DocuXMLParser.USERNAME, this.getMaintainerUsernames());
		b.endCurrentTag();
		
		// parameter section
		if(this.getParameter().size() > 0) {
			b.startTag(DocuXMLParser.PARAMETER, true, true);
			b.endOpeningTag(false);
			b.addComment("mandatory fields per parameter: name, type, description");
			b.addComment("optional fields per parameter: restrictions, default, minOccurs, maxOccurs, minVersion, maxVersion");
			for(Paramdocu p : this.getParameter()) {
				b.addContent(p.toXML(), true);
			}
			b.endCurrentTag(true);
		}
		
		// return value section
		if(this.getReturnValues().size() > 0) {
			b.startTag(DocuXMLParser.RETURN, true, true);
			b.endOpeningTag(false);
			b.addComment("mandatory fields per return variable: name, type, description");
			b.addComment("optional fields per return variable: minVersion, maxVersion");
			for(Returndocu r : this.getReturnValues()) {
				b.addContent(r.toXML(), true);
			}
			b.endCurrentTag();
		}
		
		// end document
		b.endCurrentTag();
		return b.toString();
	}

	public ArrayList<Integer> getVersions() {
		return new ArrayList<>(this.VERSIONS);
	}
}
