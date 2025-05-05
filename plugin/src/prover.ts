import { type ConfigPlugin, withDangerousMod } from "expo/config-plugins";
import fs from "node:fs";
import path from "node:path";

/**
 * A utility function to copy libraries and update the bridging header file.
 */
function copyLibsAndUpdateHeader(projectRoot: string) {
	try {
		const sourceDir = path.resolve(
			projectRoot,
			"./modules/letro-passport/ios/libs",
		);
		const targetDir = path.resolve(projectRoot, "ios/libs");
		const bridgingHeaderPath = path.resolve(
			projectRoot,
			"./ios/pnummobileapp-Bridging-Header.h",
		);

		if (!fs.existsSync(sourceDir)) {
			console.warn(
				`[expo-plugin-copy-libs] Source directory does not exist: ${sourceDir}`,
			);
			return;
		}

		// Copy library files
		fs.existsSync(targetDir);
		fs.copyFileSync(sourceDir, targetDir);
		console.log(
			`[expo-plugin-copy-libs] Successfully copied files to: ${targetDir}`,
		);

		// Update the bridging header file
		const bridgingHeaderContent = `#include "witnesscalc_prove_rsa_65537_sha256.h"
#include "witnesscalc_prove_rsa_65537_sha1.h"
#include "witnesscalc_prove_rsapss_65537_sha256.h"
#include "witnesscalc_register_rsa_65537_sha256.h"
#include "witnesscalc_register_rsa_65537_sha1.h"
#include "witnesscalc_register_rsapss_65537_sha256.h"
#include "witnesscalc_vc_and_disclose.h"
#include "groth16_prover.h"`;

		fs.writeFileSync(bridgingHeaderPath, bridgingHeaderContent);
		console.log(
			`[expo-plugin-copy-libs] Successfully updated ${bridgingHeaderPath}`,
		);
	} catch (error) {
		console.error("[expo-plugin-copy-libs] Error during execution:", error);
	}
}

/**
 * An Expo plugin using `withDangerousMod` to copy libraries and update the bridging header.
 */
export const withCopyLibsAndUpdateHeader: ConfigPlugin = (config) => {
	return withDangerousMod(config, [
		"ios",
		(config) => {
			console.error(
				"[expo-plugin-copy-libs] Starting library copy and header update...",
			);
			copyLibsAndUpdateHeader(config.modRequest.projectRoot);
			return config;
		},
	]);
};
