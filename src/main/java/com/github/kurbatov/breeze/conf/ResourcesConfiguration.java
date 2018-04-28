package com.github.kurbatov.breeze.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
@Configuration
@PropertySource(value = {"file:application.properties", "file:local.properties"}, encoding = "UTF-8", ignoreResourceNotFound = true)
public class ResourcesConfiguration {
    
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }
    
    @Bean
    public ConversionServiceFactoryBean conversionService() {
        return new ConversionServiceFactoryBean();
    }
    
}
