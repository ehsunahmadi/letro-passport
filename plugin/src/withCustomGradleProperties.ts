import { type ConfigPlugin, withGradleProperties } from "expo/config-plugins";

const withCustomGradleProperties: ConfigPlugin = (config) => {
	return withGradleProperties(config, (config) => {
		const enableJetifier = {
			type: "property" as const,
			key: "android.enableJetifier",
			value: "true",
		};
		const packagingOptions = {
			type: "property" as const,
			key: "android.packagingOptions.excludes",
			value:
				"META-INF/LICENSE,META-INF/NOTICE,META-INF/versions/9/OSGI-INF/MANIFEST.MF",
		};
		const additionalProperties = [enableJetifier, packagingOptions];

		for (const property of additionalProperties) {
			config.modResults.push(property);
		}

		return config;
	});
};

export default withCustomGradleProperties;
