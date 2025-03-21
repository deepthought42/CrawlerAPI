package com.crawlerApi.api;

import com.crawlerApi.models.enums.AuditLevel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
public class AuditStartRequest {
    @Getter
    @Setter
    private String url;

    @Getter
    @Setter
    private AuditLevel type;
}
