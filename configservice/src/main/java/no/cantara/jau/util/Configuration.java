package no.cantara.jau.util;

import org.constretto.ConstrettoBuilder;
import org.constretto.ConstrettoConfiguration;
import org.constretto.model.Resource;

public class Configuration {
	
	private static final ConstrettoConfiguration configuration = new ConstrettoBuilder()
            .createPropertiesStore()
            .addResource(Resource.create("classpath:application.properties"))
            .addResource(Resource.create("file:./config_override/application_override.properties"))
            .done()
            .getConfiguration();
	
	private Configuration() {}
	
	public static String getString(String key) {
		return configuration.evaluateToString(key);
	}
	
	public static Integer getInt(String key) {
		return configuration.evaluateToInt(key);
	}
}