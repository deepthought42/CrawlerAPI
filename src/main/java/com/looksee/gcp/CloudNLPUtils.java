package com.looksee.gcp;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.Document.Type;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.Sentence;
import com.google.cloud.language.v1.Sentiment;

public class CloudNLPUtils {
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(CloudNLPUtils.class);

	public static List<Sentence> extractSentences(String text) throws IOException {
	    LanguageServiceClient language = LanguageServiceClient.create();
		Document doc = Document.newBuilder().setContent(text).setType(Type.PLAIN_TEXT).build();

		// Detects the sentiment of the text
		List<Sentence> sentences = language.analyzeSyntax(doc).getSentencesList();
		language.shutdown();
		return sentences;
		
	}
	
	public static Sentiment extractSentiment(String text) throws IOException {
	    LanguageServiceClient language = LanguageServiceClient.create();
	    Document doc = Document.newBuilder().setContent(text).setType(Type.PLAIN_TEXT).build();
	    
    	Sentiment sentiment = language.analyzeSentiment(doc).getDocumentSentiment();
    	language.shutdown();
    	return sentiment;
    	//System.out.printf("Text: %s%n", text);
    	//System.out.printf("Sentiment: %s, %s%n", sentiment.getScore(), sentiment.getMagnitude());
    
	}
}
