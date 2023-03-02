package utils;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
	
}
