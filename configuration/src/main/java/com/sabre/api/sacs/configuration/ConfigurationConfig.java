package com.sabre.api.sacs.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * The Spring configuration class indicating that instances
 * of classes in this module should be managed by the Spring context.
 * 扫描这个类的所在包，将符合要求的类实例放入spring的ioc容器
 */
@Configuration
@ComponentScan
public class ConfigurationConfig {

}
