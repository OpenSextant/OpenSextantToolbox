package org.opensextant.vocab;

import java.util.ArrayList;
import java.util.List;

import org.opensextant.tagger.Match;

public class VocabMatch extends Match{

	/** The vocab entries this has been matched to. */
	private List<Vocab> vocabs = new ArrayList<Vocab>();

	public List<Vocab> getVocabs() {
		return vocabs;
	}

	public void setVocabs(List<Vocab> vocabs) {
		this.vocabs = vocabs;
	}

	public void addVocab(Vocab v) {
		this.vocabs.add(v);
	}

}
