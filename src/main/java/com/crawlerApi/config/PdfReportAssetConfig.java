package com.crawlerApi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for PDF report asset URLs.
 * These URLs can be configured in application.properties or via environment variables.
 * If not configured, the assets will be skipped (null values are handled gracefully).
 */
@Component
@Configuration
@ConfigurationProperties(prefix = "pdf.report.assets")
@Getter
@Setter
public class PdfReportAssetConfig {
    
    /**
     * URL for the happy face emoji icon (score >= 80)
     */
    private String happyFaceIconUrl;
    
    /**
     * URL for the average face emoji icon (60 <= score < 80)
     */
    private String averageFaceIconUrl;
    
    /**
     * URL for the sad face emoji icon (score < 60)
     */
    private String sadFaceIconUrl;
    
    /**
     * URL for the background path pattern light image
     */
    private String backgroundPathPatternLightUrl;
    
    /**
     * URL for the color management cover background image
     */
    private String colorManagementCoverBackgroundUrl;
    
    /**
     * URL for the score overview card image
     */
    private String scoreOverviewCardUrl;
    
    /**
     * URL for the needs work emoji icon
     */
    private String needsWorkEmojiUrl;
}

