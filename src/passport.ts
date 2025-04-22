import {
	EventEmitter,
	type EventSubscription,
	NativeModulesProxy,
} from "expo-modules-core";
import { Platform } from "react-native";
import type { ChangeEventPayload } from "./LetroPassport.types.ts";
import LetroPassportModule from "./LetroPassportModule";
import { assert, convertMRZData, extractMRZInfo, isDate } from "./utils";

export function hello(): string {
	try {
		return LetroPassportModule.hello();
	} catch (error) {
		console.error("Error in hello:", error);
		throw new Error("Failed to execute hello function.");
	}
}

export const IsSupported = (() => {
	try {
		return LetroPassportModule.isSupported();
	} catch (error) {
		console.error("Error checking support:", error);
		return false;
	}
})();

export function scan({
	documentNumber,
	dateOfBirth,
	dateOfExpiry,
	quality = 1,
}: {
	documentNumber: string;
	dateOfBirth: string;
	dateOfExpiry: string;
	quality?: number;
}) {
	try {
		assert(
			typeof documentNumber === "string",
			'expected string "documentNumber"',
		);
		assert(
			isDate(dateOfBirth),
			'expected string "dateOfBirth" in format "yyMMdd"',
		);
		assert(
			isDate(dateOfExpiry),
			'expected string "dateOfExpiry" in format "yyMMdd"',
		);

		if (Platform.OS === "ios") {
			return LetroPassportModule.scan(
				documentNumber,
				dateOfBirth,
				dateOfExpiry,
			);
		}
		return LetroPassportModule.scan({
			documentNumber,
			dateOfBirth,
			dateOfExpiry,
			quality,
		});
	} catch (error) {
		console.error("Error in scan:", error);
		throw new Error("Scan operation failed.");
	}
}

const emitter = new EventEmitter(
	LetroPassportModule ?? NativeModulesProxy.Test,
);

export function addChangeListener(
	listener: (event: ChangeEventPayload) => void,
): EventSubscription {
	try {
		// @ts-ignore
		return emitter.addListener("onChange", listener);
	} catch (error) {
		console.error("Error adding change listener:", error);
		throw new Error("Failed to add change listener.");
	}
}

export const startCameraScanning = async (): Promise<{
	documentNumber: string;
	dateOfBirth: string;
	dateOfExpiry: string;
}> => {
	try {
		if (Platform.OS === "ios") {
			const response = await LetroPassportModule.startScanning();
			return convertMRZData(response);
		}
		const response = await LetroPassportModule.startCameraActivity();
		return extractMRZInfo(response);
	} catch (error) {
		console.error("Error in startCameraScanning:", error);
		throw new Error("Failed to start camera scanning.");
	}
};

export const stopCameraScanning = () => {
	try {
		return LetroPassportModule.stopScanning();
	} catch (error) {
		console.error("Error in stopCameraScanning:", error);
		throw new Error("Failed to stop camera scanning.");
	}
};
