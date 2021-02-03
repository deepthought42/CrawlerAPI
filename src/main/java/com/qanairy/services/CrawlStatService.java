package com.qanairy.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qanairy.models.CrawlStat;

import com.qanairy.models.repository.CrawlStatRepository;

@Service
public class CrawlStatService {
	@SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private CrawlStatRepository crawl_stat_repository;
	
	public CrawlStat save(CrawlStat crawl_stat) {
		return crawl_stat_repository.save(crawl_stat);	
	}
}
