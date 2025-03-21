package com.crawlerApi.models.competitiveanalysis.brand;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.crawlerApi.models.repository.BrandRepository;

@Service
public class BrandService {

	@Autowired
	private BrandRepository brand_repo;
	
	public Brand save(Brand brand){
		return brand_repo.save(brand);
	}
}
