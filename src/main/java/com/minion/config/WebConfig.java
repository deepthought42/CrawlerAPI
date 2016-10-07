package com.minion.config;

import javax.annotation.PostConstruct;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.orient.commons.repository.config.EnableOrientRepositories;
import org.springframework.data.orient.object.OrientObjectDatabaseFactory;
import org.springframework.data.orient.object.repository.support.OrientObjectRepositoryFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import com.minion.browsing.Page;
import com.minion.browsing.PageAlert;
import com.minion.browsing.PageElement;
import com.minion.browsing.PathObject;
import com.minion.structs.Path;
import com.minion.tester.Test;
import com.minion.tester.TestRecord;

@Configuration
@EnableAutoConfiguration
@EnableTransactionManagement
@EnableOrientRepositories(basePackages = "com.minion.persistence", repositoryFactoryBeanClass = OrientObjectRepositoryFactoryBean.class)
@ComponentScan("com.minion")
public class WebConfig {

    @Bean
    public OrientObjectDatabaseFactory factory() {
        OrientObjectDatabaseFactory factory =  new OrientObjectDatabaseFactory();

        factory.setUrl("remote:localhost:2480/Thoth");
        factory.setUsername("brandon");
        factory.setPassword("password");

        return factory;
    }

    @PostConstruct
    @Transactional
    public void registerEntities() {
        factory().db().getEntityManager().registerEntityClass(Page.class);
        factory().db().getEntityManager().registerEntityClass(PageElement.class);
        factory().db().getEntityManager().registerEntityClass(PageAlert.class);
        factory().db().getEntityManager().registerEntityClass(Test.class);
        factory().db().getEntityManager().registerEntityClass(Path.class);
        factory().db().getEntityManager().registerEntityClass(TestRecord.class);
        factory().db().getEntityManager().registerEntityClass(PathObject.class);
    }
}
