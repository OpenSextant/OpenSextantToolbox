package org.opensextant.tagger;

import java.util.ArrayList;
import java.util.List;

public class Document {

	private String title;
	private String content;
	private List<Match> matchList;

	public Document() {
		this.title = "";
		this.content = "";
		matchList = new ArrayList<Match>();
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

	public List<Match> getMatchList() {
		return matchList;
	}

	public void setMatchList(List<Match> matchList) {
		this.matchList = matchList;
	}

	public void addMatch(Match match) {
		this.matchList.add(match);

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
