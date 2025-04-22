import * as FileSystem from "expo-file-system";
import { Platform } from "react-native";
import LetroPassportModule from "./LetroPassportModule";

export type Proof = {
	proof: {
		a: [string, string];
		b: [[string, string], [string, string]];
		c: [string, string];
	};
	pub_signals: string[];
};

export const parseProofAndroid = (response: string) => {
	const match = response.match(
		/ZkProof\(proof=Proof\(pi_a=\[(.*?)\], pi_b=\[\[(.*?)\], \[(.*?)\], \[1, 0\]\], pi_c=\[(.*?)\], protocol=groth16, curve=bn128\), pub_signals=\[(.*?)\]\)/,
	);

	if (!match) throw new Error("Invalid input format");

	const [, pi_a, pi_b_1, pi_b_2, pi_c, pub_signals] = match;

	return {
		proof: {
			a: pi_a?.split(",").map((n: string) => n.trim()),
			b: [
				pi_b_1?.split(",").map((n: string) => n.trim()),
				pi_b_2?.split(",").map((n: string) => n.trim()),
			],
			c: pi_c?.split(",").map((n: string) => n.trim()),
		},
		pub_signals: pub_signals?.split(",").map((n: string) => n.trim()),
	} as Proof;
};
export const generateProof = async (
	circuit: string,
	inputs: { [key: string]: unknown },
) => {
	console.log("launching generateProof function");
	console.log("inputs in prover.ts", inputs);
	console.log("circuit", circuit);

	const zkey_path = `${FileSystem.documentDirectory}/${circuit}.zkey`;
	const dat_path = `${FileSystem.documentDirectory}/${circuit}.dat`;

	const witness_calculator = circuit;

	if (!zkey_path || !witness_calculator || !dat_path) {
		throw new Error("Required parameters are missing");
	}
	console.log("zkey_path", zkey_path);
	console.log("witness_calculator", witness_calculator);
	console.log("dat_path", dat_path);

	try {
		const response = await LetroPassportModule.runProveAction(
			zkey_path,
			witness_calculator,
			dat_path,
			inputs,
		);

		console.log("local proof:", response);

		if (Platform.OS === "android") {
			const parsedResponse = parseProofAndroid(response);
			console.log("parsedResponse", parsedResponse);
			return formatProof(parsedResponse);
		}
		const parsedResponse = JSON.parse(response);
		console.log("parsedResponse", parsedResponse);

		return formatProof({
			proof: parsedResponse.proof,
			pub_signals: parsedResponse.inputs,
		});
	} catch (err) {
		console.error(err);
		throw err;
	}
};

export const formatProof = (
	rawProof: Proof,
): {
	proof: {
		pi_a: string[];
		pi_b: string[][];
		pi_c: string[];
		protocol: string;
		curve: string;
	};
	publicSignals: string[];
} => {
	return {
		proof: {
			pi_a: [rawProof.proof.a[0], rawProof.proof.a[1], "1"],
			pi_b: [
				[rawProof.proof.b[0][0], rawProof.proof.b[0][1]],
				[rawProof.proof.b[1][0], rawProof.proof.b[1][1]],
				["1", "0"],
			],
			pi_c: [rawProof.proof.c[0], rawProof.proof.c[1], "1"],
			protocol: "groth16",
			curve: "bn128",
		},
		publicSignals: rawProof.pub_signals,
	};
};
