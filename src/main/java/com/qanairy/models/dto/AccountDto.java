package com.qanairy.models.dto;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.qanairy.models.Domain;

/**
 * 
 */
public class AccountDto {
     
	@NotNull
    @NotEmpty  
    private String org_name;
    
    @NotNull
    @NotEmpty
    private String service_package;
     
    @NotNull
    @NotEmpty
    private String payment_acct_num;

    @NotNull
    @NotEmpty
    private List<Domain> domains;
    
	public String getOrgName() {
		return org_name;
	}

	public void setOrgName(String org_name) {
		this.org_name = org_name;
	}

	public String getServicePackage() {
		return service_package;
	}

	public void setServicePackage(String service_package) {
		this.service_package = service_package;
	}

	public String getPaymentAcctNum() {
		return payment_acct_num;
	}

	public void setPaymentAcctNum(String payment_acct_num) {
		this.payment_acct_num = payment_acct_num;
	}

	public List<Domain> getDomains() {
		return domains;
	}

	public void setDomains(List<Domain> domains) {
		this.domains = domains;
	}
}