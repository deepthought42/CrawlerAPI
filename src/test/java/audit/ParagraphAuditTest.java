package audit;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.qanairy.models.audit.ParagraphingAudit;

public class ParagraphAuditTest {

	@Test
	public void sentenceScoreTest() {
		String good_sentence = "This is a good sentence";
		assertTrue(ParagraphingAudit.calculateSentenceScore(good_sentence).getPointsAchieved() == 2);
		
		String also_good_sentence = "This is also a very good sentence, I must say";
		assertTrue(ParagraphingAudit.calculateSentenceScore(also_good_sentence).getPointsAchieved() == 2);

		String meh_sentence = "This is sentence is the most meh sentence I have seen";
		assertTrue(ParagraphingAudit.calculateSentenceScore(meh_sentence).getPointsAchieved() == 1);

		String poor_meh_sentence = "This is the longest sentence you can have without getting zero points on for sentence length, so don't do it";
		assertTrue(ParagraphingAudit.calculateSentenceScore(poor_meh_sentence).getPointsAchieved() == 1);

		String terrible_sentence = "This sentence is way too long of a sentence and it really sounds a lot like a rambling run on sentence";
		assertTrue(ParagraphingAudit.calculateSentenceScore(terrible_sentence).getPointsAchieved() == 0);

	}
	
	@Test
	public void paragraphScoreTest() {
		assertTrue(ParagraphingAudit.calculateParagraphScore(1).getPointsAchieved() == 1);
		
		assertTrue(ParagraphingAudit.calculateParagraphScore(3).getPointsAchieved() == 1);

		assertTrue(ParagraphingAudit.calculateParagraphScore(4).getPointsAchieved() == 1);

		assertTrue(ParagraphingAudit.calculateParagraphScore(5).getPointsAchieved() == 1);

		assertTrue(ParagraphingAudit.calculateParagraphScore(6).getPointsAchieved() == 0);
	}
}
