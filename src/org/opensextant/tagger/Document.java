package org.opensextant.tagger;

import java.util.ArrayList;
import java.util.List;

public class Document {

	private String title;
	private String content;
	private List<Match> annoList;

	public Document() {
		this.title = "";
		this.content = "";
		annoList = new ArrayList<Match>();
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public List<Match> getAnnoList() {
		return annoList;
	}

	public void setAnnoList(List<Match> annoList) {
		this.annoList = annoList;
	}

	public void addAnno(Match tmpAnno) {
		this.annoList.add(tmpAnno);

	}

	public String getSnippet(Match an, int size) {
		int start = (int) (an.getStart() - size);
		int end = (int) (an.getEnd() + size);

		if (start < 0) {
			start = 0;
		}
		if (end > this.content.length()) {
			end = this.content.length();
		}

		return this.content.substring(start, end).replaceAll("[\n\r]+", " ");
	}

}
