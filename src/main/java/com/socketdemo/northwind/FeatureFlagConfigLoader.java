package com.socketdemo.northwind;

import org.yaml.snakeyaml.Yaml;

/**
 * Loads per-tenant feature-flag overrides from a YAML config blob that
 * tenants can edit through an admin panel.
 *
 * CVE-2022-1471 (SnakeYaml &lt; 1.31): the plain no-arg Yaml() constructor
 * uses a Constructor that can instantiate arbitrary Java types found in the
 * document, not just the safe scalar/map/list types. Any tenant-editable
 * YAML parsed with new Yaml().load(...) carries this risk.
 */
public class FeatureFlagConfigLoader {

    public Object loadConfig(String yamlConfig) {
        Yaml yaml = new Yaml();
        return yaml.load(yamlConfig);
    }
}
