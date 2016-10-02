package nl.gertontenham.ords.templates.config;

import oracle.dbtools.plugin.api.conf.Configuration;
import oracle.dbtools.plugin.api.di.annotations.Provides;

import javax.inject.Inject;

/**
 * Plugin configurator class
 */
@Provides
public class Configurator {

    private final Configuration ordsConfig;

    @Inject
    public Configurator(Configuration ordsConfig) {
        this.ordsConfig = ordsConfig;
    }

    public String getTemplateRootPath() {
        return ordsConfig.get("templating.rootpath");
    }
}
