import fs from "node:fs";
import path from "node:path";

import { type ConfigPlugin, withDangerousMod } from "expo/config-plugins";

async function readFileAsync(path: string) {
	return fs.promises.readFile(path, "utf8");
}

async function updatePbxproj(pbxprojPath: string) {
	try {
		if (!fs.existsSync(pbxprojPath)) {
			console.error(
				`[expo-plugin-update-pbxproj] File not found: ${pbxprojPath}`,
			);
			return;
		}
		const pbxprojContent = await readFileAsync(
			path.join(__dirname, "project.pbxproj"),
		);

		fs.writeFileSync(pbxprojPath, pbxprojContent, "utf8");
		console.log(
			`[expo-plugin-update-pbxproj] Successfully updated ${pbxprojPath}`,
		);
	} catch (error) {
		console.error(
			"[expo-plugin-update-pbxproj] Error updating pbxproj file:",
			error,
		);
	}
}

const withPbxProjeModification: ConfigPlugin = (config) => {
	return withDangerousMod(config, [
		"ios",
		(config) => {
			const pbxprojPath = path.resolve(
				config.modRequest.platformProjectRoot,
				"pnummobileapp.xcodeproj/project.pbxproj",
			);

			console.error(
				`[expo-plugin-update-pbxproj] Updating pbxprojPath: ${pbxprojPath}`,
			);
			updatePbxproj(pbxprojPath);

			return config;
		},
	]);
};

export default withPbxProjeModification;
