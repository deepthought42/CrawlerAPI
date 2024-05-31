package utils;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.looksee.models.audit.Audit;
import com.looksee.models.enums.AuditCategory;
import com.looksee.models.enums.AuditLevel;
import com.looksee.models.enums.AuditName;
import com.looksee.models.enums.AuditSubcategory;
import com.looksee.utils.AuditUtils;

/**
 * 
 * 
 */
public class AuditUtilsTest {
	
	@Test
	public void verifyCalcuateProgressByCategory(){
		int page_count = 2;
		
		Set<AuditName> expected_audits = new HashSet<>();
		expected_audits.add(AuditName.NON_TEXT_BACKGROUND_CONTRAST);
		expected_audits.add(AuditName.TEXT_BACKGROUND_CONTRAST);
		
		Set<Audit> audits = new HashSet<>();
		audits.add(new Audit(AuditCategory.AESTHETICS,
							 AuditSubcategory.CONTRAST,
							 AuditName.TEXT_BACKGROUND_CONTRAST,
							 0,
							 new HashSet<>(),
							 AuditLevel.DOMAIN,
							 1,
							 "",
							 "",
							 "",
							 true));
		
		audits.add(new Audit(AuditCategory.AESTHETICS,
							 AuditSubcategory.CONTRAST,
							 AuditName.TEXT_BACKGROUND_CONTRAST,
							 0,
							 new HashSet<>(),
							 AuditLevel.DOMAIN,
							 1,
							 "",
							 "",
							 "",
							 true));
		
		double progress = AuditUtils.calculateProgress(AuditCategory.AESTHETICS,
														 page_count,
														 audits,
														 expected_audits);
		
		System.out.println("progress :: " + progress);
		assertTrue(progress == 0.5);
		
		audits.add(new Audit(AuditCategory.AESTHETICS,
							 AuditSubcategory.CONTRAST,
							 AuditName.NON_TEXT_BACKGROUND_CONTRAST,
							 0,
							 new HashSet<>(),
							 AuditLevel.DOMAIN,
							 1,
							 "",
							 "",
							 "",
							 true));
		
		progress = AuditUtils.calculateProgress(AuditCategory.AESTHETICS,
											 page_count,
											 audits,
											 expected_audits);
							
		System.out.println("progress 2 :: " + progress);
		assertTrue(progress == 0.75);
	}
	
	@Test 
	public void calculateAccessibilityScore(){
		Set<Audit> audits = new HashSet<>();
		audits.add(new Audit(AuditCategory.AESTHETICS, AuditSubcategory.TEXT_CONTRAST, AuditName.TEXT_BACKGROUND_CONTRAST, 0, new HashSet<>(), AuditLevel.DOMAIN, 100, "", "", "", true));

		double score = AuditUtils.calculateAccessibilityScore(audits);
		assert score == 0;

		audits.add(new Audit(AuditCategory.AESTHETICS, AuditSubcategory.TEXT_CONTRAST, AuditName.TEXT_BACKGROUND_CONTRAST, 100, new HashSet<>(), AuditLevel.DOMAIN, 100, "", "", "", true));
		audits.add(new Audit(AuditCategory.AESTHETICS, AuditSubcategory.NON_TEXT_CONTRAST, AuditName.TEXT_BACKGROUND_CONTRAST, 10, new HashSet<>(), AuditLevel.DOMAIN, 20, "", "", "", true));

		score = AuditUtils.calculateAccessibilityScore(audits);
		assert score == 0.5;

		audits.add(new Audit(AuditCategory.AESTHETICS, AuditSubcategory.LINKS, AuditName.LINKS, 1, new HashSet<>(), AuditLevel.DOMAIN, 20, "", "", "", true));

		score = AuditUtils.calculateAccessibilityScore(audits);
		assert score == 0.3875;

		audits.add(new Audit(AuditCategory.AESTHETICS, AuditSubcategory.LINKS, AuditName.ALT_TEXT, 1, new HashSet<>(), AuditLevel.DOMAIN, 20, "", "", "", true));

		score = AuditUtils.calculateAccessibilityScore(audits);
		assert score == 0.32;

		audits.add(new Audit(AuditCategory.AESTHETICS, AuditSubcategory.LINKS, AuditName.IMAGE_POLICY, 1, new HashSet<>(), AuditLevel.DOMAIN, 1, "", "", "", true));

		score = AuditUtils.calculateAccessibilityScore(audits);
		assert score == 0.32;
	}
}
