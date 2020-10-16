package audit;

import java.util.HashMap;
import java.util.Map;

import com.qanairy.models.audit.domain.DomainPaddingAudit;

public class DomainPaddingAuditTest {

	public void evaluateSpacingConsistencyForConsistentSpacing() {
		//define map that represents values found during test
		Map<String, Double> element_padding_map = getConsistentUnitMap();
		
		
		
		
		DomainPaddingAudit padding_audit = new DomainPaddingAudit();
		
		//padding_audit.evaluateSpacingConsistency(element_padding_map);
	}

	private Map<String, Double> getConsistentUnitMap() {
		// TODO Auto-generated method stub
		return new HashMap<>();
	}
}
